package com.payroll.aj.payroll.engine;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Secure SpEL-based formula evaluator for salary component calculations.
 *
 * <h3>Security model</h3>
 * <p>This service uses {@link SimpleEvaluationContext} configured for
 * <strong>read-only data binding only</strong>. This explicitly blocks:</p>
 * <ul>
 *   <li>Type references — {@code T(java.lang.Runtime)} ✘</li>
 *   <li>Constructor invocations — {@code new java.io.File(...)} ✘</li>
 *   <li>Bean references — {@code @beanName} ✘</li>
 *   <li>Arbitrary method invocations — {@code #var.getClass()} ✘</li>
 * </ul>
 *
 * <p>Only variable references ({@code #VAR_NAME}) and arithmetic
 * operators ({@code +}, {@code -}, {@code *}, {@code /}, {@code %})
 * are permitted.</p>
 *
 * <h3>Precision</h3>
 * <p>All evaluation results are converted to {@link BigDecimal} with
 * scale 2 and {@link RoundingMode#HALF_UP} as mandated by
 * ARCHITECTURE_SPEC §4.</p>
 */
@Service
public class SpelFormulaEvaluatorService {

    private static final int MONETARY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Matches SpEL variable references: {@code #UPPER_CASE_NAME}.
     * Excludes SpEL built-ins ({@code #this}, {@code #root}) by
     * requiring uppercase first character.
     */
    private static final Pattern VARIABLE_PATTERN =
            Pattern.compile("#([A-Z_][A-Z0-9_]*)");

    private final SpelExpressionParser parser = new SpelExpressionParser();

    /**
     * Evaluates a SpEL formula expression against a variable context.
     *
     * <p>Example: {@code evaluate("(#BASE_SALARY * 0.40) + #TRAVEL",
     * Map.of("BASE_SALARY", bd(50000), "TRAVEL", bd(5000)))}
     * returns {@code 25000.00}.</p>
     *
     * @param expression the SpEL expression string
     * @param variables  named BigDecimal variables available as
     *                   {@code #VARIABLE_NAME} in the expression
     * @return the evaluated result as BigDecimal(scale=2, HALF_UP)
     * @throws SpelParseException      if the expression syntax is invalid
     * @throws SpelEvaluationException  if evaluation fails (unknown
     *                                  variable, blocked operation, etc.)
     * @throws IllegalStateException    if the result is null or non-numeric
     */
    public BigDecimal evaluate(String expression, Map<String, BigDecimal> variables) {
        Expression parsed = parser.parseExpression(expression);

        SimpleEvaluationContext context = SimpleEvaluationContext
                .forReadOnlyDataBinding()
                .build();

        variables.forEach(context::setVariable);

        Object result = parsed.getValue(context);

        return toBigDecimal(result).setScale(MONETARY_SCALE, ROUNDING_MODE);
    }

    /**
     * Extracts all uppercase variable references ({@code #VAR_NAME})
     * from a formula expression using regex.
     *
     * @param formula the SpEL expression string
     * @return set of variable names (without the {@code #} prefix)
     */
    public Set<String> extractVariableReferences(String formula) {
        Matcher matcher = VARIABLE_PATTERN.matcher(formula);
        return matcher.results()
                .map(m -> m.group(1))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Validates a formula expression for syntactic correctness and
     * ensures it only references variables in the allowed set.
     *
     * <p>Performs a trial evaluation with {@code BigDecimal.ONE} for
     * all allowed variables to verify runtime safety.</p>
     *
     * @param formula          the SpEL expression to validate
     * @param allowedVariables the set of permitted variable names
     * @return {@code true} if the formula is valid and safe
     */
    public boolean isValidFormula(String formula, Set<String> allowedVariables) {
        if (formula == null || formula.isBlank()) {
            return false;
        }

        try {
            // 1. Syntactic validation
            parser.parseExpression(formula);

            // 2. Variable scope check
            Set<String> referenced = extractVariableReferences(formula);
            if (!allowedVariables.containsAll(referenced)) {
                return false;
            }

            // 3. Trial evaluation with dummy values
            Map<String, BigDecimal> dummy = new HashMap<>();
            for (String var : allowedVariables) {
                dummy.put(var, BigDecimal.ONE);
            }
            evaluate(formula, dummy);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Converts a SpEL evaluation result to {@link BigDecimal}.
     * Uses Java 21 pattern matching for {@code switch}.
     */
    private BigDecimal toBigDecimal(Object value) {
        return switch (value) {
            case BigDecimal bd -> bd;
            case BigInteger bi -> new BigDecimal(bi);
            case Double d -> BigDecimal.valueOf(d);
            case Float f -> BigDecimal.valueOf(f.doubleValue());
            case Long l -> BigDecimal.valueOf(l);
            case Integer i -> BigDecimal.valueOf(i);
            case Number n -> BigDecimal.valueOf(n.doubleValue());
            case null -> throw new IllegalStateException(
                    "Formula evaluated to null");
            default -> throw new IllegalStateException(
                    "Formula evaluated to non-numeric type: "
                            + value.getClass().getName());
        };
    }
}
