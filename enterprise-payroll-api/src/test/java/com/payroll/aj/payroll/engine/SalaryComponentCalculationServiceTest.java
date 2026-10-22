package com.payroll.aj.payroll.engine;

import com.payroll.aj.common.enums.CalculationType;
import com.payroll.aj.common.enums.ComponentType;
import com.payroll.aj.payroll.entity.EmployeeSalaryStructure;
import com.payroll.aj.payroll.entity.SalaryComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the salary component calculation engine.
 *
 * <p>Tests cover flat amounts, percentage calculations, formula
 * dependency ordering, override semantics, aggregation (gross/net),
 * and circular dependency detection.</p>
 */
class SalaryComponentCalculationServiceTest {

    private SalaryComponentCalculationService calculationService;

    private static final BigDecimal BASE_SALARY = new BigDecimal("50000.00");
    private static final LocalDate TODAY = LocalDate.now();

    @BeforeEach
    void setUp() {
        SpelFormulaEvaluatorService formulaEvaluator =
                new SpelFormulaEvaluatorService();
        calculationService = new SalaryComponentCalculationService(formulaEvaluator);
    }

    // ── Helper builders ──────────────────────────────────────────

    private SalaryComponent buildComponent(String code, String name,
                                           ComponentType type,
                                           CalculationType calcType,
                                           BigDecimal defaultValue,
                                           String formula) {
        return SalaryComponent.builder()
                .code(code)
                .name(name)
                .type(type)
                .calculationType(calcType)
                .defaultValue(defaultValue)
                .formulaExpression(formula)
                .build();
    }

    private EmployeeSalaryStructure buildStructure(SalaryComponent component,
                                                   BigDecimal overrideAmount) {
        return EmployeeSalaryStructure.builder()
                .component(component)
                .overrideAmount(overrideAmount)
                .effectiveFrom(TODAY)
                .build();
    }

    // ── FLAT_AMOUNT ──────────────────────────────────────────────

    @Nested
    @DisplayName("FLAT_AMOUNT components")
    class FlatAmountTests {

        @Test
        @DisplayName("Uses default value when no override")
        void flatAmountUsesDefault() {
            SalaryComponent conveyance = buildComponent(
                    "EAR-IND-CONVEYANCE", "Conveyance Allowance",
                    ComponentType.EARNING, CalculationType.FLAT_AMOUNT,
                    new BigDecimal("1600.00"), null);

            PayrollBreakdown result = calculationService.calculate(
                    BASE_SALARY, List.of(buildStructure(conveyance, null)));

            assertEquals(1, result.earnings().size());
            assertEquals(new BigDecimal("1600.00"),
                    result.earnings().getFirst().amount());
            // gross = 50000 + 1600 = 51600
            assertEquals(new BigDecimal("51600.00"), result.grossPay());
        }

        @Test
        @DisplayName("Override amount takes precedence over default")
        void flatAmountOverride() {
            SalaryComponent conveyance = buildComponent(
                    "EAR-IND-CONVEYANCE", "Conveyance Allowance",
                    ComponentType.EARNING, CalculationType.FLAT_AMOUNT,
                    new BigDecimal("1600.00"), null);

            PayrollBreakdown result = calculationService.calculate(
                    BASE_SALARY,
                    List.of(buildStructure(conveyance, new BigDecimal("2500.00"))));

            assertEquals(new BigDecimal("2500.00"),
                    result.earnings().getFirst().amount());
        }
    }

    // ── PERCENTAGE_OF_BASE ───────────────────────────────────────

    @Nested
    @DisplayName("PERCENTAGE_OF_BASE components")
    class PercentageOfBaseTests {

        @Test
        @DisplayName("40% of 50000 = 20000.00")
        void percentageCalculation() {
            SalaryComponent hra = buildComponent(
                    "EAR-IND-HRA", "House Rent Allowance",
                    ComponentType.EARNING, CalculationType.PERCENTAGE_OF_BASE,
                    new BigDecimal("40.00"), null);

            PayrollBreakdown result = calculationService.calculate(
                    BASE_SALARY, List.of(buildStructure(hra, null)));

            assertEquals(new BigDecimal("20000.00"),
                    result.earnings().getFirst().amount());
        }

        @Test
        @DisplayName("12% of 50000 = 6000.00 (PF deduction)")
        void percentageDeduction() {
            SalaryComponent pf = buildComponent(
                    "DED-IND-PF", "Provident Fund",
                    ComponentType.DEDUCTION, CalculationType.PERCENTAGE_OF_BASE,
                    new BigDecimal("12.00"), null);

            PayrollBreakdown result = calculationService.calculate(
                    BASE_SALARY, List.of(buildStructure(pf, null)));

            assertEquals(new BigDecimal("6000.00"),
                    result.deductions().getFirst().amount());
            assertEquals(new BigDecimal("6000.00"), result.totalDeductions());
            // net = 50000 (gross, no earnings) - 6000 = 44000
            assertEquals(new BigDecimal("44000.00"), result.netPay());
        }

        @Test
        @DisplayName("Override percentage: employee gets 45% instead of 40%")
        void percentageOverride() {
            SalaryComponent hra = buildComponent(
                    "EAR-IND-HRA", "House Rent Allowance",
                    ComponentType.EARNING, CalculationType.PERCENTAGE_OF_BASE,
                    new BigDecimal("40.00"), null);

            PayrollBreakdown result = calculationService.calculate(
                    BASE_SALARY,
                    List.of(buildStructure(hra, new BigDecimal("45.00"))));

            // 50000 * 45/100 = 22500
            assertEquals(new BigDecimal("22500.00"),
                    result.earnings().getFirst().amount());
        }
    }

    // ── CUSTOM_FORMULA ───────────────────────────────────────────

    @Nested
    @DisplayName("CUSTOM_FORMULA components")
    class CustomFormulaTests {

        @Test
        @DisplayName("Formula referencing BASE_SALARY only")
        void formulaWithBaseSalary() {
            SalaryComponent special = buildComponent(
                    "EAR-IND-SPECIAL", "Special Allowance",
                    ComponentType.EARNING, CalculationType.CUSTOM_FORMULA,
                    BigDecimal.ZERO, "#BASE_SALARY * 0.20");

            PayrollBreakdown result = calculationService.calculate(
                    BASE_SALARY, List.of(buildStructure(special, null)));

            // 50000 * 0.20 = 10000
            assertEquals(new BigDecimal("10000.00"),
                    result.earnings().getFirst().amount());
        }

        @Test
        @DisplayName("Formula with dependency on another component")
        void formulaWithDependency() {
            SalaryComponent conveyance = buildComponent(
                    "EAR-IND-CONVEYANCE", "Conveyance",
                    ComponentType.EARNING, CalculationType.FLAT_AMOUNT,
                    new BigDecimal("1600.00"), null);

            SalaryComponent bonus = buildComponent(
                    "EAR-IND-BONUS", "Bonus",
                    ComponentType.EARNING, CalculationType.CUSTOM_FORMULA,
                    BigDecimal.ZERO,
                    "(#BASE_SALARY * 0.10) + #CONVEYANCE");

            PayrollBreakdown result = calculationService.calculate(
                    BASE_SALARY,
                    List.of(
                            buildStructure(conveyance, null),
                            buildStructure(bonus, null)));

            // conveyance = 1600, bonus = (50000*0.10) + 1600 = 6600
            ComponentLineItem bonusItem = result.earnings().stream()
                    .filter(li -> li.componentCode().equals("EAR-IND-BONUS"))
                    .findFirst().orElseThrow();

            assertEquals(new BigDecimal("6600.00"), bonusItem.amount());
        }

        @Test
        @DisplayName("Override bypasses formula evaluation")
        void formulaOverrideBypassesEvaluation() {
            SalaryComponent special = buildComponent(
                    "EAR-IND-SPECIAL", "Special Allowance",
                    ComponentType.EARNING, CalculationType.CUSTOM_FORMULA,
                    BigDecimal.ZERO, "#BASE_SALARY * 0.20");

            PayrollBreakdown result = calculationService.calculate(
                    BASE_SALARY,
                    List.of(buildStructure(special, new BigDecimal("7777.00"))));

            assertEquals(new BigDecimal("7777.00"),
                    result.earnings().getFirst().amount());
        }
    }

    // ── Dependency ordering ──────────────────────────────────────

    @Nested
    @DisplayName("Dependency ordering")
    class DependencyOrderingTests {

        @Test
        @DisplayName("Formulas listed before their dependencies are still resolved")
        void outOfOrderDependencies() {
            // Formula listed FIRST but depends on FLAT component listed SECOND
            SalaryComponent formula = buildComponent(
                    "EAR-IND-TOTAL_ALLOW", "Total Allowance",
                    ComponentType.EARNING, CalculationType.CUSTOM_FORMULA,
                    BigDecimal.ZERO, "#HRA + #CONVEYANCE");

            SalaryComponent hra = buildComponent(
                    "EAR-IND-HRA", "HRA",
                    ComponentType.EARNING, CalculationType.PERCENTAGE_OF_BASE,
                    new BigDecimal("40.00"), null);

            SalaryComponent conveyance = buildComponent(
                    "EAR-IND-CONVEYANCE", "Conveyance",
                    ComponentType.EARNING, CalculationType.FLAT_AMOUNT,
                    new BigDecimal("1600.00"), null);

            PayrollBreakdown result = calculationService.calculate(
                    BASE_SALARY,
                    List.of(
                            buildStructure(formula, null),    // depends on HRA + CONVEYANCE
                            buildStructure(hra, null),
                            buildStructure(conveyance, null)));

            // HRA = 20000, Conveyance = 1600, Total = 21600
            ComponentLineItem totalAllow = result.earnings().stream()
                    .filter(li -> li.componentCode().equals("EAR-IND-TOTAL_ALLOW"))
                    .findFirst().orElseThrow();

            assertEquals(new BigDecimal("21600.00"), totalAllow.amount());
        }

        @Test
        @DisplayName("Circular dependency throws IllegalStateException")
        void circularDependencyDetected() {
            SalaryComponent compA = buildComponent(
                    "EAR-IND-A", "Component A",
                    ComponentType.EARNING, CalculationType.CUSTOM_FORMULA,
                    BigDecimal.ZERO, "#B * 2");

            SalaryComponent compB = buildComponent(
                    "EAR-IND-B", "Component B",
                    ComponentType.EARNING, CalculationType.CUSTOM_FORMULA,
                    BigDecimal.ZERO, "#A * 3");

            assertThrows(IllegalStateException.class, () ->
                    calculationService.calculate(
                            BASE_SALARY,
                            List.of(
                                    buildStructure(compA, null),
                                    buildStructure(compB, null))));
        }
    }

    // ── Full payroll aggregation ─────────────────────────────────

    @Nested
    @DisplayName("Full payroll aggregation")
    class AggregationTests {

        @Test
        @DisplayName("Gross = base + earnings; Net = gross - deductions")
        void fullPayrollCalculation() {
            SalaryComponent hra = buildComponent(
                    "EAR-IND-HRA", "HRA",
                    ComponentType.EARNING, CalculationType.PERCENTAGE_OF_BASE,
                    new BigDecimal("40.00"), null);

            SalaryComponent conveyance = buildComponent(
                    "EAR-IND-CONV", "Conveyance",
                    ComponentType.EARNING, CalculationType.FLAT_AMOUNT,
                    new BigDecimal("1600.00"), null);

            SalaryComponent pf = buildComponent(
                    "DED-IND-PF", "Provident Fund",
                    ComponentType.DEDUCTION, CalculationType.PERCENTAGE_OF_BASE,
                    new BigDecimal("12.00"), null);

            SalaryComponent profTax = buildComponent(
                    "DED-IND-PTAX", "Professional Tax",
                    ComponentType.DEDUCTION, CalculationType.FLAT_AMOUNT,
                    new BigDecimal("200.00"), null);

            PayrollBreakdown result = calculationService.calculate(
                    BASE_SALARY,
                    List.of(
                            buildStructure(hra, null),
                            buildStructure(conveyance, null),
                            buildStructure(pf, null),
                            buildStructure(profTax, null)));

            // Earnings: HRA=20000, Conv=1600
            // Gross = 50000 + 20000 + 1600 = 71600
            assertEquals(new BigDecimal("71600.00"), result.grossPay());
            assertEquals(2, result.earnings().size());

            // Deductions: PF=6000, PTax=200
            // Total deductions = 6200
            assertEquals(new BigDecimal("6200.00"), result.totalDeductions());
            assertEquals(2, result.deductions().size());

            // Net = 71600 - 6200 = 65400
            assertEquals(new BigDecimal("65400.00"), result.netPay());
        }

        @Test
        @DisplayName("Empty component list: net = base salary")
        void emptyComponentsReturnsBase() {
            PayrollBreakdown result = calculationService.calculate(
                    BASE_SALARY, List.of());

            assertEquals(new BigDecimal("50000.00"), result.grossPay());
            assertEquals(new BigDecimal("0.00"), result.totalDeductions());
            assertEquals(new BigDecimal("50000.00"), result.netPay());
        }
    }

    // ── extractFormulaVariable ───────────────────────────────────

    @Nested
    @DisplayName("extractFormulaVariable")
    class ExtractVariableTests {

        @Test
        @DisplayName("EAR-IND-HRA01 → HRA01")
        void standardEarningCode() {
            assertEquals("HRA01",
                    SalaryComponentCalculationService
                            .extractFormulaVariable("EAR-IND-HRA01"));
        }

        @Test
        @DisplayName("DED-IND-PF01 → PF01")
        void standardDeductionCode() {
            assertEquals("PF01",
                    SalaryComponentCalculationService
                            .extractFormulaVariable("DED-IND-PF01"));
        }

        @Test
        @DisplayName("EAR-IND-TRAVEL_ALLOWANCE → TRAVEL_ALLOWANCE")
        void underscoreInCode() {
            assertEquals("TRAVEL_ALLOWANCE",
                    SalaryComponentCalculationService
                            .extractFormulaVariable("EAR-IND-TRAVEL_ALLOWANCE"));
        }

        @Test
        @DisplayName("Plain code without dashes returns as-is")
        void plainCodeReturnsAsIs() {
            assertEquals("BONUS",
                    SalaryComponentCalculationService
                            .extractFormulaVariable("BONUS"));
        }
    }
}
