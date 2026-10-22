import React, { useState, useEffect } from 'react';
import {
  Drawer,
  Form,
  InputNumber,
  Button,
  Typography,
  Divider,
  Alert,
  Tag,
  Space,
  Tooltip,
} from 'antd';
import {
  PlayCircleOutlined,
  InfoCircleOutlined,
  CheckCircleOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import { type SalaryComponent, extractVariableName } from '../../api/component';
import './FormulaTesterDrawer.css';

const { Text, Title, Paragraph } = Typography;

// ── Types ─────────────────────────────────────────────────────────────────────

interface EvalResult {
  value: number;
  breakdown: string;
  isError: boolean;
  errorMessage?: string;
}

interface FormulaTesterDrawerProps {
  open: boolean;
  component: SalaryComponent | null;
  onClose: () => void;
}

// ── SpEL Simulator ────────────────────────────────────────────────────────────

/**
 * Client-side JavaScript simulation of the backend SpEL engine.
 *
 * Supports the same arithmetic subset the SpelFormulaEvaluatorService permits:
 *   - Variable references: #BASE_SALARY, #HRA01, etc.
 *   - Operators: +, -, *, /, %, ()
 *
 * The resolver populates all #VAR references with known context values,
 * then uses Function() to safely evaluate the numeric expression.
 */
const simulateSpel = (
  expression: string,
  context: Record<string, number>,
): number => {
  // Replace #VAR_NAME with the numeric value from context
  let jsExpr = expression.replace(/#([A-Z_][A-Z0-9_]*)/g, (_, varName: string) => {
    const val = context[varName];
    if (val === undefined) {
      throw new Error(`Unresolved variable: #${varName}`);
    }
    return String(val);
  });

  // Sanitize: only allow digits, operators, spaces, parentheses, and dots
  if (/[^0-9+\-*/.%() ]/.test(jsExpr)) {
    throw new Error('Formula contains unsupported operations');
  }

  // eslint-disable-next-line no-new-func
  const fn = new Function(`return (${jsExpr})`);
  const result = fn() as unknown;
  if (typeof result !== 'number' || !isFinite(result)) {
    throw new Error('Formula produced a non-finite result');
  }
  return Math.round(result * 100) / 100;
};

// ── Evaluate Component ────────────────────────────────────────────────────────

const evaluate = (
  component: SalaryComponent,
  baseSalary: number,
): EvalResult => {
  try {
    switch (component.calculation_type) {
      case 'FLAT_AMOUNT': {
        const value = component.default_value;
        return {
          value,
          breakdown: `Fixed amount = ${value.toLocaleString('en-IN', { style: 'currency', currency: 'INR' })}`,
          isError: false,
        };
      }

      case 'PERCENTAGE_OF_BASE': {
        const pct = component.default_value;
        const value = Math.round(baseSalary * (pct / 100) * 100) / 100;
        return {
          value,
          breakdown: `${pct}% × ₹${baseSalary.toLocaleString('en-IN')} = ₹${value.toLocaleString('en-IN')}`,
          isError: false,
        };
      }

      case 'CUSTOM_FORMULA': {
        const expr = component.formula_expression ?? '';
        if (!expr.trim()) {
          return { value: 0, breakdown: 'No formula expression set.', isError: true, errorMessage: 'Empty formula' };
        }

        // Build context: BASE_SALARY is always available.
        // Other referenced variables are seeded with a placeholder of 0
        // (real values would come from resolved component dependencies).
        const context: Record<string, number> = { BASE_SALARY: baseSalary };
        const varPattern = /#([A-Z_][A-Z0-9_]*)/g;
        let m: RegExpExecArray | null;
        const refs: string[] = [];
        while ((m = varPattern.exec(expr)) !== null) {
          const varName = m[1];
          if (varName !== 'BASE_SALARY' && !(varName in context)) {
            context[varName] = 0;
            refs.push(varName);
          }
        }

        const value = simulateSpel(expr, context);
        const refNote =
          refs.length > 0
            ? `\n⚠ Dependencies [${refs.map((r) => `#${r}`).join(', ')}] seeded with ₹0 — live values differ.`
            : '';

        return {
          value,
          breakdown: `${expr}${refNote}`,
          isError: false,
        };
      }
    }
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : String(err);
    return { value: 0, breakdown: '', isError: true, errorMessage: msg };
  }
};

// ── Component ─────────────────────────────────────────────────────────────────

const FormulaTesterDrawer: React.FC<FormulaTesterDrawerProps> = ({
  open,
  component,
  onClose,
}) => {
  const [baseSalary, setBaseSalary] = useState<number>(100000);
  const [result, setResult] = useState<EvalResult | null>(null);

  // Reset result when a new component is selected
  useEffect(() => {
    setResult(null);
  }, [component]);

  if (!component) return null;

  const varName = extractVariableName(component.code);
  const isCustom = component.calculation_type === 'CUSTOM_FORMULA';
  const isPercentage = component.calculation_type === 'PERCENTAGE_OF_BASE';

  const handleEvaluate = () => {
    if (!baseSalary || baseSalary <= 0) return;
    setResult(evaluate(component, baseSalary));
  };

  const formatCurrency = (n: number) =>
    n.toLocaleString('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 });

  return (
    <Drawer
      title={null}
      open={open}
      onClose={onClose}
      width={480}
      className="formula-drawer"
      closable={false}
    >
      {/* ── Header ── */}
      <div className="drawer-header">
        <div className="drawer-header-left">
          <div className={`drawer-type-badge ${component.type === 'EARNING' ? 'badge-earning' : 'badge-deduction'}`}>
            {component.type}
          </div>
          <div>
            <Title level={5} className="drawer-title">{component.name}</Title>
            <Space size={6}>
              <Tag color="blue" className="drawer-code-tag">{component.code}</Tag>
              <Tag color="default" className="drawer-var-tag">
                SpEL: <code>#{varName}</code>
              </Tag>
            </Space>
          </div>
        </div>
        <Button type="text" onClick={onClose} className="drawer-close-btn">✕</Button>
      </div>

      <Divider className="drawer-divider" />

      {/* ── Formula Preview ── */}
      <div className="formula-preview-section">
        <Text className="section-label">Calculation Logic</Text>

        {isCustom && component.formula_expression && (
          <div className="formula-block">
            <Text className="formula-label">SpEL Expression</Text>
            <pre className="formula-expression">{component.formula_expression}</pre>
            <Text className="formula-hint">
              <InfoCircleOutlined /> Variables map to resolved component values at runtime.
              Dependencies seeded with ₹0 for simulation.
            </Text>
          </div>
        )}

        {isPercentage && (
          <div className="formula-block">
            <Text className="formula-label">Formula</Text>
            <pre className="formula-expression">
              #{varName} = #BASE_SALARY × ({component.default_value}% ÷ 100)
            </pre>
          </div>
        )}

        {component.calculation_type === 'FLAT_AMOUNT' && (
          <div className="formula-block">
            <Text className="formula-label">Fixed Amount</Text>
            <pre className="formula-expression">
              #{varName} = {formatCurrency(component.default_value)}
            </pre>
          </div>
        )}

        <div className="formula-meta-row">
          <Tag color={component.is_taxable ? 'volcano' : 'default'} className="meta-tag">
            {component.is_taxable ? '⚡ Taxable' : 'Tax-exempt'}
          </Tag>
          <Tag color={component.is_statutory ? 'gold' : 'default'} className="meta-tag">
            {component.is_statutory ? '⚖ Statutory' : 'Non-statutory'}
          </Tag>
        </div>
      </div>

      <Divider className="drawer-divider" />

      {/* ── Tester Form ── */}
      <div className="tester-section">
        <Text className="section-label">
          Sandbox Evaluator
          <Tooltip title="Results are computed in JavaScript to simulate the backend SpEL engine. The actual server uses BigDecimal arithmetic — results may differ by a few paise.">
            <InfoCircleOutlined className="section-hint-icon" />
          </Tooltip>
        </Text>

        <Form layout="vertical" className="tester-form">
          <Form.Item
            label="Hypothetical Base Salary (₹)"
            className="tester-form-item"
          >
            <InputNumber
              value={baseSalary}
              onChange={(v) => setBaseSalary(v ?? 0)}
              min={1}
              max={10_000_000}
              step={1000}
              formatter={(v) => `₹ ${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
              parser={(v) => Number((v ?? '').replace(/₹\s?|(,*)/g, '')) as unknown as 0}
              size="large"
              className="salary-input"
              id="formula-base-salary"
            />
          </Form.Item>

          <Button
            type="primary"
            icon={<PlayCircleOutlined />}
            onClick={handleEvaluate}
            size="large"
            block
            className="evaluate-btn"
            id="formula-evaluate"
          >
            Evaluate
          </Button>
        </Form>
      </div>

      {/* ── Result ── */}
      {result && (
        <div className="result-section">
          {result.isError ? (
            <Alert
              type="error"
              icon={<WarningOutlined />}
              showIcon
              message="Evaluation Error"
              description={result.errorMessage}
              className="result-alert"
            />
          ) : (
            <div className="result-card">
              <div className="result-header">
                <CheckCircleOutlined className="result-check-icon" />
                <Text className="result-label">Computed Value</Text>
              </div>
              <div className="result-value">
                {formatCurrency(result.value)}
              </div>
              {component.type === 'DEDUCTION' && (
                <Text className="result-deduction-note">
                  This amount will be <strong>subtracted</strong> from gross pay
                </Text>
              )}
              {result.breakdown && (
                <Paragraph className="result-breakdown">
                  <pre>{result.breakdown}</pre>
                </Paragraph>
              )}
            </div>
          )}
        </div>
      )}
    </Drawer>
  );
};

export default FormulaTesterDrawer;
