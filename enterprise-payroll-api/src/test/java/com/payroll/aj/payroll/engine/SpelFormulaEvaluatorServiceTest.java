package com.payroll.aj.payroll.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.SpelEvaluationException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive tests for the SpEL formula evaluator.
 *
 * <p>Covers arithmetic precision, complex multi-variable formulas,
 * SpEL injection safety, rounding behaviour, and validation.</p>
 */
class SpelFormulaEvaluatorServiceTest {

    private SpelFormulaEvaluatorService evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new SpelFormulaEvaluatorService();
    }

    // ── Arithmetic Precision ─────────────────────────────────────

    @Nested
    @DisplayName("Arithmetic precision")
    class ArithmeticPrecisionTests {

        @Test
        @DisplayName("Simple variable reference returns BigDecimal(scale=2)")
        void simpleVariableReference() {
            BigDecimal result = evaluator.evaluate(
                    "#BASE_SALARY",
                    Map.of("BASE_SALARY", new BigDecimal("50000.00")));

            assertEquals(new BigDecimal("50000.00"), result);
        }

        @Test
        @DisplayName("40% of 50000.00 = 20000.00")
        void percentageOfBaseSalary() {
            BigDecimal result = evaluator.evaluate(
                    "#BASE_SALARY * 0.40",
                    Map.of("BASE_SALARY", new BigDecimal("50000.00")));

            assertEquals(new BigDecimal("20000.00"), result);
        }

        @Test
        @DisplayName("12% of base + overtime = (50000 * 0.12) + 3000 = 9000.00")
        void complexFormulaWithMultipleVariables() {
            Map<String, BigDecimal> vars = Map.of(
                    "BASE_SALARY", new BigDecimal("50000.00"),
                    "OVERTIME", new BigDecimal("3000.00"));

            BigDecimal result = evaluator.evaluate(
                    "(#BASE_SALARY * 0.12) + #OVERTIME", vars);

            assertEquals(new BigDecimal("9000.00"), result);
        }

        @Test
        @DisplayName("Subtraction: 50000.00 - 4500.50 = 45499.50")
        void subtractionFormula() {
            BigDecimal result = evaluator.evaluate(
                    "#GROSS - #TAX",
                    Map.of("GROSS", new BigDecimal("50000.00"),
                            "TAX", new BigDecimal("4500.50")));

            assertEquals(new BigDecimal("45499.50"), result);
        }

        @Test
        @DisplayName("Compound formula: (#A * 0.40) + (#B * 0.10) - #C")
        void compoundFormulaWithThreeVariables() {
            Map<String, BigDecimal> vars = Map.of(
                    "A", new BigDecimal("100000.00"),
                    "B", new BigDecimal("50000.00"),
                    "C", new BigDecimal("2000.00"));

            BigDecimal result = evaluator.evaluate(
                    "(#A * 0.40) + (#B * 0.10) - #C", vars);

            // (100000 * 0.40) + (50000 * 0.10) - 2000 = 40000 + 5000 - 2000 = 43000
            assertEquals(new BigDecimal("43000.00"), result);
        }
    }

    // ── Scale & Rounding ─────────────────────────────────────────

    @Nested
    @DisplayName("Scale and rounding")
    class ScaleAndRoundingTests {

        @Test
        @DisplayName("Result is always scale=2 with HALF_UP rounding")
        void resultAlwaysScale2() {
            BigDecimal result = evaluator.evaluate(
                    "#BASE_SALARY * 0.333",
                    Map.of("BASE_SALARY", new BigDecimal("10000.00")));

            // 10000 * 0.333 = 3330.0 → scale 2 → 3330.00
            assertEquals(2, result.scale());
            assertEquals(new BigDecimal("3330.00"), result);
        }

        @Test
        @DisplayName("Fractional paisa/cent precision: 33333.33 * 0.40 rounds correctly")
        void fractionalPrecision() {
            BigDecimal result = evaluator.evaluate(
                    "#BASE_SALARY * 0.40",
                    Map.of("BASE_SALARY", new BigDecimal("33333.33")));

            // 33333.33 * 0.40 = 13333.332 → HALF_UP → 13333.33
            assertEquals(new BigDecimal("13333.33"), result);
        }

        @Test
        @DisplayName("Division rounds to scale=2 without ArithmeticException")
        void divisionWithRounding() {
            BigDecimal result = evaluator.evaluate(
                    "#TOTAL / 3",
                    Map.of("TOTAL", new BigDecimal("10000.00")));

            // 10000 / 3 = 3333.33... → HALF_UP scale 2 → 3333.33
            assertEquals(new BigDecimal("3333.33"), result);
        }

        @Test
        @DisplayName("No fractional drift across chained operations")
        void noDriftAcrossChainedOperations() {
            Map<String, BigDecimal> vars = Map.of(
                    "BASE_SALARY", new BigDecimal("75421.57"),
                    "ALLOWANCE", new BigDecimal("3256.83"));

            BigDecimal result = evaluator.evaluate(
                    "(#BASE_SALARY * 0.12) + (#ALLOWANCE * 0.50)", vars);

            // (75421.57 * 0.12) + (3256.83 * 0.50)
            // ≈ 9050.5884 + 1628.415 = 10679.0034 → scale 2 → 10679.00
            assertEquals(new BigDecimal("10679.00"), result);
            assertEquals(2, result.scale());
        }
    }

    // ── SpEL Injection Safety ────────────────────────────────────

    @Nested
    @DisplayName("SpEL injection safety")
    class InjectionSafetyTests {

        private final Map<String, BigDecimal> safeVars =
                Map.of("BASE_SALARY", new BigDecimal("50000"));

        @Test
        @DisplayName("T(Runtime).exec() is blocked")
        void runtimeExecBlocked() {
            assertThrows(SpelEvaluationException.class, () ->
                    evaluator.evaluate(
                            "T(java.lang.Runtime).getRuntime().exec('calc')",
                            safeVars));
        }

        @Test
        @DisplayName("T(System).exit() is blocked")
        void systemExitBlocked() {
            assertThrows(SpelEvaluationException.class, () ->
                    evaluator.evaluate(
                            "T(java.lang.System).exit(0)",
                            safeVars));
        }

        @Test
        @DisplayName("Constructor invocation (new File) is blocked")
        void constructorBlocked() {
            assertThrows(SpelEvaluationException.class, () ->
                    evaluator.evaluate(
                            "new java.io.File('/etc/passwd').exists()",
                            safeVars));
        }

        @Test
        @DisplayName("Bean reference (@bean) is blocked")
        void beanReferenceBlocked() {
            assertThrows(SpelEvaluationException.class, () ->
                    evaluator.evaluate(
                            "@systemProperties",
                            safeVars));
        }

        @Test
        @DisplayName("Reflection via getClass() is blocked")
        void reflectionBlocked() {
            assertThrows(SpelEvaluationException.class, () ->
                    evaluator.evaluate(
                            "#BASE_SALARY.getClass().forName('java.lang.Runtime')",
                            safeVars));
        }

        @Test
        @DisplayName("ProcessBuilder construction is blocked")
        void processBuilderBlocked() {
            assertThrows(SpelEvaluationException.class, () ->
                    evaluator.evaluate(
                            "new java.lang.ProcessBuilder('cmd').start()",
                            safeVars));
        }
    }

    // ── Variable Extraction ──────────────────────────────────────

    @Nested
    @DisplayName("Variable extraction")
    class VariableExtractionTests {

        @Test
        @DisplayName("Extracts all uppercase variable references")
        void extractsAllVariables() {
            Set<String> refs = evaluator.extractVariableReferences(
                    "(#BASE_SALARY * 0.40) + #TRAVEL_ALLOWANCE - #TAX");

            assertEquals(Set.of("BASE_SALARY", "TRAVEL_ALLOWANCE", "TAX"), refs);
        }

        @Test
        @DisplayName("Empty formula returns empty set")
        void emptyFormulaReturnsEmptySet() {
            Set<String> refs = evaluator.extractVariableReferences("100 + 200");
            assertTrue(refs.isEmpty());
        }
    }

    // ── Formula Validation ───────────────────────────────────────

    @Nested
    @DisplayName("Formula validation")
    class FormulaValidationTests {

        @Test
        @DisplayName("Valid formula with allowed variables passes")
        void validFormulaPasses() {
            boolean valid = evaluator.isValidFormula(
                    "(#BASE_SALARY * 0.40) + #HRA",
                    Set.of("BASE_SALARY", "HRA"));

            assertTrue(valid);
        }

        @Test
        @DisplayName("Formula referencing unknown variable fails")
        void unknownVariableFails() {
            boolean valid = evaluator.isValidFormula(
                    "#BASE_SALARY + #UNKNOWN",
                    Set.of("BASE_SALARY"));

            assertFalse(valid);
        }

        @Test
        @DisplayName("Null formula fails")
        void nullFormulaFails() {
            assertFalse(evaluator.isValidFormula(null, Set.of("BASE_SALARY")));
        }

        @Test
        @DisplayName("Blank formula fails")
        void blankFormulaFails() {
            assertFalse(evaluator.isValidFormula("   ", Set.of("BASE_SALARY")));
        }

        @Test
        @DisplayName("Malformed expression fails")
        void malformedExpressionFails() {
            assertFalse(evaluator.isValidFormula(
                    "(#BASE_SALARY * ", Set.of("BASE_SALARY")));
        }
    }
}
