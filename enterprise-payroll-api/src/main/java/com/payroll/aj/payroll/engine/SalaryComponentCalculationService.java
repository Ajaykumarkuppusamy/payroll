package com.payroll.aj.payroll.engine;

import com.payroll.aj.common.enums.CalculationType;
import com.payroll.aj.common.enums.ComponentType;
import com.payroll.aj.payroll.entity.EmployeeSalaryStructure;
import com.payroll.aj.payroll.entity.SalaryComponent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Core payroll calculation engine.
 *
 * <p>Given an employee's base salary and their active salary structure
 * (component assignments), this service resolves every component's
 * monetary value in proper <strong>dependency order</strong> and
 * produces a complete {@link PayrollBreakdown}.</p>
 *
 * <h3>Resolution phases</h3>
 * <ol>
 *   <li><strong>Phase 1</strong> — resolve all {@code FLAT_AMOUNT}
 *       and {@code PERCENTAGE_OF_BASE} components (plus any
 *       {@code CUSTOM_FORMULA} with an override amount). These have
 *       no formula dependencies.</li>
 *   <li><strong>Phase 2</strong> — iteratively resolve
 *       {@code CUSTOM_FORMULA} components. A formula is only
 *       evaluated once all its variable dependencies exist in the
 *       context. This implements implicit topological ordering.
 *       If no progress is made in a pass, a circular-dependency
 *       error is raised.</li>
 * </ol>
 *
 * <h3>Variable naming</h3>
 * <p>Component codes follow the pattern
 * {@code {TYPE}-{COUNTRY}-{CODE}} (e.g., {@code EAR-IND-HRA01}).
 * The trailing {@code {CODE}} segment is extracted and used as
 * the SpEL formula variable name (e.g., {@code #HRA01}).
 * {@code #BASE_SALARY} is always available as a built-in.</p>
 *
 * <h3>Aggregation</h3>
 * <p>{@code grossPay = baseSalary + Σ(earnings)}<br>
 * {@code netPay = grossPay − Σ(deductions)}</p>
 */
@Service
@RequiredArgsConstructor
public class SalaryComponentCalculationService {

    private static final int MONETARY_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final String BASE_SALARY_VAR = "BASE_SALARY";

    private final SpelFormulaEvaluatorService formulaEvaluator;

    /**
     * Calculates the full payroll breakdown for one employee.
     *
     * @param baseSalary       the employee's {@code base_salary}
     * @param activeStructures the employee's active component
     *                         assignments (with eagerly loaded
     *                         {@link SalaryComponent} references)
     * @return complete breakdown with gross, deductions, net,
     *         and itemised components
     * @throws IllegalStateException if a circular formula dependency
     *                               is detected
     */
    public PayrollBreakdown calculate(BigDecimal baseSalary,
                                      List<EmployeeSalaryStructure> activeStructures) {

        Map<String, BigDecimal> context = new LinkedHashMap<>();
        context.put(BASE_SALARY_VAR, baseSalary);

        // ── Partition by resolvability ────────────────────────────
        List<EmployeeSalaryStructure> immediate = new ArrayList<>();
        Deque<EmployeeSalaryStructure> formulaQueue = new ArrayDeque<>();

        for (EmployeeSalaryStructure ess : activeStructures) {
            SalaryComponent comp = ess.getComponent();
            boolean isFormulaWithoutOverride =
                    comp.getCalculationType() == CalculationType.CUSTOM_FORMULA
                            && ess.getOverrideAmount() == null;
            if (isFormulaWithoutOverride) {
                formulaQueue.add(ess);
            } else {
                immediate.add(ess);
            }
        }

        // ── Phase 1: resolve non-formula components ──────────────
        List<ComponentLineItem> lineItems = new ArrayList<>();

        for (EmployeeSalaryStructure ess : immediate) {
            BigDecimal amount = resolveAmount(ess, baseSalary);
            String varName = extractFormulaVariable(ess.getComponent().getCode());
            context.put(varName, amount);
            lineItems.add(toLineItem(ess, amount));
        }

        // ── Phase 2: iteratively resolve formula components ──────
        int maxPasses = formulaQueue.size() + 1;
        int pass = 0;

        while (!formulaQueue.isEmpty() && pass < maxPasses) {
            pass++;
            int resolvedThisPass = 0;
            Iterator<EmployeeSalaryStructure> it = formulaQueue.iterator();

            while (it.hasNext()) {
                EmployeeSalaryStructure ess = it.next();
                SalaryComponent comp = ess.getComponent();
                Set<String> refs = formulaEvaluator
                        .extractVariableReferences(comp.getFormulaExpression());

                if (context.keySet().containsAll(refs)) {
                    BigDecimal amount = formulaEvaluator
                            .evaluate(comp.getFormulaExpression(), context);
                    String varName = extractFormulaVariable(comp.getCode());
                    context.put(varName, amount);
                    lineItems.add(toLineItem(ess, amount));
                    it.remove();
                    resolvedThisPass++;
                }
            }

            if (resolvedThisPass == 0) {
                break;
            }
        }

        if (!formulaQueue.isEmpty()) {
            String unresolvedCodes = formulaQueue.stream()
                    .map(ess -> ess.getComponent().getCode())
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException(
                    "Circular dependency or unresolvable formula references "
                            + "in salary components: [" + unresolvedCodes + "]");
        }

        // ── Aggregate ────────────────────────────────────────────
        List<ComponentLineItem> earnings = lineItems.stream()
                .filter(li -> li.type() == ComponentType.EARNING)
                .toList();

        List<ComponentLineItem> deductions = lineItems.stream()
                .filter(li -> li.type() == ComponentType.DEDUCTION)
                .toList();

        BigDecimal grossPay = earnings.stream()
                .map(ComponentLineItem::amount)
                .reduce(baseSalary, BigDecimal::add)
                .setScale(MONETARY_SCALE, ROUNDING);

        BigDecimal totalDeductions = deductions.stream()
                .map(ComponentLineItem::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONETARY_SCALE, ROUNDING);

        BigDecimal netPay = grossPay.subtract(totalDeductions)
                .setScale(MONETARY_SCALE, ROUNDING);

        return new PayrollBreakdown(
                grossPay, totalDeductions, netPay, earnings, deductions);
    }

    // ── Internal helpers ─────────────────────────────────────────

    /**
     * Resolves the monetary amount for a non-formula component
     * (or a formula component with an override).
     */
    private BigDecimal resolveAmount(EmployeeSalaryStructure ess,
                                     BigDecimal baseSalary) {
        SalaryComponent comp = ess.getComponent();

        return switch (comp.getCalculationType()) {
            case FLAT_AMOUNT -> {
                BigDecimal value = ess.getOverrideAmount() != null
                        ? ess.getOverrideAmount()
                        : comp.getDefaultValue();
                yield value.setScale(MONETARY_SCALE, ROUNDING);
            }
            case PERCENTAGE_OF_BASE -> {
                BigDecimal percentage = ess.getOverrideAmount() != null
                        ? ess.getOverrideAmount()
                        : comp.getDefaultValue();
                yield baseSalary.multiply(percentage)
                        .divide(BigDecimal.valueOf(100), MONETARY_SCALE, ROUNDING);
            }
            case CUSTOM_FORMULA -> {
                // Reached only when overrideAmount is non-null
                // (formulas without override go to the formula queue)
                yield ess.getOverrideAmount()
                        .setScale(MONETARY_SCALE, ROUNDING);
            }
        };
    }

    /**
     * Extracts the formula variable name from a component code.
     *
     * <p>Code pattern: {@code {TYPE}-{COUNTRY}-{CODE}}.<br>
     * Example: {@code EAR-IND-HRA01} → {@code HRA01}</p>
     *
     * @param componentCode the full component code
     * @return the {@code {CODE}} segment used in SpEL formulas
     */
    static String extractFormulaVariable(String componentCode) {
        int firstDash = componentCode.indexOf('-');
        if (firstDash < 0) {
            return componentCode;
        }
        int secondDash = componentCode.indexOf('-', firstDash + 1);
        return secondDash >= 0
                ? componentCode.substring(secondDash + 1)
                : componentCode;
    }

    private ComponentLineItem toLineItem(EmployeeSalaryStructure ess,
                                         BigDecimal amount) {
        SalaryComponent comp = ess.getComponent();
        return new ComponentLineItem(
                comp.getCode(), comp.getName(), comp.getType(), amount);
    }
}
