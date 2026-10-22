import React, { useEffect, useState, useCallback } from 'react';
import {
  Table,
  Tag,
  Typography,
  Alert,
  Space,
  Input,
  Button,
  Tooltip,
  Badge,
  Modal,
  Form,
  Select,
  InputNumber,
  Switch,
  message,
  Row,
  Col,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  CalculatorOutlined,
  PlayCircleOutlined,
} from '@ant-design/icons';
import {
  getComponents,
  createComponent,
  type SalaryComponent,
  type ComponentType,
  type CalculationType,
  type CreateComponentInput,
  extractVariableName,
} from '../api/component';
import useAuthStore from '../store/useAuthStore';
import FormulaTesterDrawer from '../components/payroll/FormulaTesterDrawer';
import './FormulaStudio.css';

const { Title, Text } = Typography;

// ── Display Config ────────────────────────────────────────────────────────────

const TYPE_CONFIG: Record<ComponentType, { color: string; label: string }> = {
  EARNING:   { color: 'green', label: 'Earning'   },
  DEDUCTION: { color: 'red',   label: 'Deduction' },
};

const CALC_CONFIG: Record<CalculationType, { color: string; label: string }> = {
  FLAT_AMOUNT:        { color: 'blue',    label: 'Flat Amount'        },
  PERCENTAGE_OF_BASE: { color: 'purple',  label: 'Percentage of Base' },
  CUSTOM_FORMULA:     { color: 'volcano', label: 'Custom Formula'     },
};

// ── Table Columns ─────────────────────────────────────────────────────────────

const buildColumns = (
  onTestFormula: (component: SalaryComponent) => void,
): ColumnsType<SalaryComponent> => [
  {
    title: 'Code',
    dataIndex: 'code',
    key: 'code',
    width: 160,
    sorter: (a, b) => a.code.localeCompare(b.code),
    render: (code: string) => (
      <Tag color="blue" className="code-tag">
        {code}
      </Tag>
    ),
  },
  {
    title: 'Name',
    dataIndex: 'name',
    key: 'name',
    render: (name: string, record: SalaryComponent) => (
      <div className="component-name-cell">
        <div className="component-name">{name}</div>
        <div className="component-var-hint">
          SpEL: <code className="spel-var">#{extractVariableName(record.code)}</code>
        </div>
      </div>
    ),
  },
  {
    title: 'Type',
    dataIndex: 'type',
    key: 'type',
    width: 120,
    filters: [
      { text: 'Earning',   value: 'EARNING'   },
      { text: 'Deduction', value: 'DEDUCTION' },
    ],
    onFilter: (value, record) => record.type === value,
    render: (type: ComponentType) => {
      const cfg = TYPE_CONFIG[type] ?? { color: 'default', label: type };
      return (
        <Tag color={cfg.color} className="type-tag">
          {cfg.label}
        </Tag>
      );
    },
  },
  {
    title: 'Calculation',
    dataIndex: 'calculation_type',
    key: 'calculation_type',
    width: 180,
    filters: [
      { text: 'Flat Amount',        value: 'FLAT_AMOUNT'        },
      { text: 'Percentage of Base', value: 'PERCENTAGE_OF_BASE' },
      { text: 'Custom Formula',     value: 'CUSTOM_FORMULA'     },
    ],
    onFilter: (value, record) => record.calculation_type === value,
    render: (calc: CalculationType) => {
      const cfg = CALC_CONFIG[calc] ?? { color: 'default', label: calc };
      return (
        <Tag color={cfg.color} className="calc-tag">
          {cfg.label}
        </Tag>
      );
    },
  },
  {
    title: 'Formula / Value',
    key: 'formula',
    render: (_: unknown, record: SalaryComponent) => {
      if (record.calculation_type === 'CUSTOM_FORMULA') {
        return (
          <code className="formula-code">
            {record.formula_expression ?? '—'}
          </code>
        );
      }
      if (record.calculation_type === 'PERCENTAGE_OF_BASE') {
        return (
          <span className="formula-value">
            {record.default_value}% of Base
          </span>
        );
      }
      return (
        <span className="formula-value">
          ₹ {record.default_value.toLocaleString('en-IN')}
        </span>
      );
    },
  },
  {
    title: 'Taxable',
    dataIndex: 'is_taxable',
    key: 'is_taxable',
    width: 90,
    render: (v: boolean) => (
      <Tag color={v ? 'orange' : 'default'} className="boolean-tag">
        {v ? 'Taxable' : 'Exempt'}
      </Tag>
    ),
  },
  {
    title: 'Statutory',
    dataIndex: 'is_statutory',
    key: 'is_statutory',
    width: 100,
    render: (v: boolean) => (
      <Tag color={v ? 'magenta' : 'default'} className="boolean-tag">
        {v ? 'Statutory' : 'Standard'}
      </Tag>
    ),
  },
  {
    title: 'Actions',
    key: 'actions',
    width: 90,
    render: (_: unknown, record: SalaryComponent) =>
      record.calculation_type === 'CUSTOM_FORMULA' && (
        <Tooltip title="Test formula with sandbox values">
          <Button
            size="small"
            icon={<PlayCircleOutlined />}
            className="test-formula-btn"
            onClick={() => onTestFormula(record)}
          >
            Test
          </Button>
        </Tooltip>
      ),
  },
];

// ── Component ─────────────────────────────────────────────────────────────────

const FormulaStudio: React.FC = () => {
  const { activeCompanyId } = useAuthStore();
  const [components, setComponents] = useState<SalaryComponent[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchText, setSearchText] = useState('');
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [selectedComponent, setSelectedComponent] = useState<SalaryComponent | null>(null);
  const [form] = Form.useForm();
  const [calcType, setCalcType] = useState<CalculationType>('FLAT_AMOUNT');

  const [pagination, setPagination] = useState<TablePaginationConfig>({
    current: 1,
    pageSize: 10,
    showSizeChanger: true,
    showTotal: (t) => `${t} components`,
  });

  const fetchComponents = useCallback(async () => {
    if (!activeCompanyId) return;
    setLoading(true);
    setError(null);

    try {
      const data = await getComponents(activeCompanyId);
      setComponents(data);
    } catch {
      setError('Failed to load salary components from server.');
    } finally {
      setLoading(false);
    }
  }, [activeCompanyId]);

  useEffect(() => {
    void fetchComponents();
  }, [fetchComponents]);

  const handleTestFormula = (component: SalaryComponent) => {
    setSelectedComponent(component);
    setDrawerOpen(true);
  };

  const handleCreateComponent = async () => {
    try {
      const values = await form.validateFields();
      if (!activeCompanyId) return;

      setCreating(true);
      const input: CreateComponentInput = {
        code: values.code,
        name: values.name,
        type: values.type,
        calculation_type: values.calculation_type,
        formula_expression: values.calculation_type === 'CUSTOM_FORMULA' ? values.formula_expression : null,
        default_value: values.default_value ?? 0,
        is_taxable: values.is_taxable ?? true,
        is_statutory: values.is_statutory ?? false,
      };

      await createComponent(activeCompanyId, input);
      message.success(`Component '${values.name}' created successfully!`);
      setCreateModalOpen(false);
      form.resetFields();
      void fetchComponents();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      message.error(axiosErr.response?.data?.message || 'Failed to create component');
    } finally {
      setCreating(false);
    }
  };

  const filteredData = searchText
    ? components.filter((c) => {
        const q = searchText.toLowerCase();
        return (
          c.code.toLowerCase().includes(q) ||
          c.name.toLowerCase().includes(q) ||
          (c.formula_expression ?? '').toLowerCase().includes(q)
        );
      })
    : components;

  const earningCount = components.filter((c) => c.type === 'EARNING').length;
  const formulaCount = components.filter((c) => c.calculation_type === 'CUSTOM_FORMULA').length;

  return (
    <div className="studio-page">
      {/* Page Header */}
      <div className="studio-header">
        <div className="studio-title-group">
          <div className="studio-icon">
            <CalculatorOutlined />
          </div>
          <div>
            <Title level={4} className="studio-title">Formula Studio</Title>
            <Text className="studio-subtitle">
              Salary Component Catalog ·{' '}
              <Badge color="green" text={<span className="stat-text">{earningCount} earnings</span>} />
              {' · '}
              <Badge color="volcano" text={<span className="stat-text">{formulaCount} formulas</span>} />
            </Text>
          </div>
        </div>

        <Space size={12}>
          <Input
            prefix={<SearchOutlined className="search-icon" />}
            placeholder="Search by code, name, formula…"
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            allowClear
            className="studio-search"
            id="studio-search"
          />
          <Tooltip title="Refresh">
            <Button
              icon={<ReloadOutlined />}
              onClick={() => void fetchComponents()}
              loading={loading}
              className="refresh-btn"
              id="studio-refresh"
            />
          </Tooltip>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              form.resetFields();
              setCalcType('FLAT_AMOUNT');
              form.setFieldsValue({
                type: 'EARNING',
                calculation_type: 'FLAT_AMOUNT',
                default_value: 0,
                is_taxable: true,
                is_statutory: false,
              });
              setCreateModalOpen(true);
            }}
            className="add-component-btn"
            id="studio-add-component"
          >
            New Component
          </Button>
        </Space>
      </div>

      {error && (
        <Alert
          message="Server Error"
          description={error}
          type="error"
          showIcon
          closable
          className="studio-error"
          action={<Button size="small" onClick={() => void fetchComponents()}>Retry</Button>}
        />
      )}

      {!activeCompanyId && !loading && (
        <Alert
          message="No company selected"
          description="Select a company from the header to view salary components."
          type="info"
          showIcon
          className="studio-error"
        />
      )}

      {/* Component Table */}
      <div className="studio-table-wrapper">
        <Table<SalaryComponent>
          rowKey="id"
          dataSource={filteredData}
          columns={buildColumns(handleTestFormula)}
          loading={loading}
          pagination={pagination}
          onChange={(pag) => setPagination(pag)}
          className="studio-table"
          scroll={{ x: 1000 }}
          locale={{ emptyText: searchText ? 'No components match your search.' : 'No components found for this jurisdiction.' }}
        />
      </div>

      {/* Formula Tester Drawer */}
      <FormulaTesterDrawer
        open={drawerOpen}
        component={selectedComponent}
        onClose={() => {
          setDrawerOpen(false);
          setSelectedComponent(null);
        }}
      />

      {/* Create Component Modal */}
      <Modal
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, color: '#f8fafc' }}>
            <PlusOutlined style={{ color: '#a855f7', fontSize: 18 }} />
            <span>Create Salary Component Template</span>
          </div>
        }
        open={createModalOpen}
        onCancel={() => setCreateModalOpen(false)}
        width={620}
        footer={[
          <Button key="cancel" onClick={() => setCreateModalOpen(false)} disabled={creating}>
            Cancel
          </Button>,
          <Button
            key="submit"
            type="primary"
            loading={creating}
            onClick={handleCreateComponent}
            style={{
              background: 'linear-gradient(135deg, #7c3aed, #4f46e5)',
              border: 'none',
            }}
          >
            Create Component
          </Button>,
        ]}
        styles={{
          body: {
            background: '#0f172a',
            paddingTop: 8,
          },
          header: {
            background: '#0f172a',
            borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
            paddingBottom: 14,
          },
        }}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="code"
                label={<span style={{ color: '#cbd5e1' }}>Code (e.g. EAR-IND-PERF)</span>}
                rules={[
                  { required: true, message: 'Code is required' },
                  { pattern: /^[A-Z]{3}-[A-Z]{2,3}-[A-Z0-9]+$/, message: 'Format: TYPE-COUNTRY-CODE (e.g. EAR-IND-BONUS)' },
                ]}
              >
                <Input placeholder="EAR-IND-BONUS" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="name"
                label={<span style={{ color: '#cbd5e1' }}>Component Name</span>}
                rules={[{ required: true, message: 'Name is required' }]}
              >
                <Input placeholder="e.g. Performance Bonus" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="type"
                label={<span style={{ color: '#cbd5e1' }}>Type</span>}
                rules={[{ required: true }]}
              >
                <Select
                  options={[
                    { value: 'EARNING', label: 'Earning' },
                    { value: 'DEDUCTION', label: 'Deduction' },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="calculation_type"
                label={<span style={{ color: '#cbd5e1' }}>Calculation Mechanism</span>}
                rules={[{ required: true }]}
              >
                <Select
                  onChange={(val: CalculationType) => setCalcType(val)}
                  options={[
                    { value: 'FLAT_AMOUNT', label: 'Flat Monetary Amount (₹)' },
                    { value: 'PERCENTAGE_OF_BASE', label: 'Percentage of Base (%)' },
                    { value: 'CUSTOM_FORMULA', label: 'Custom SpEL Formula' },
                  ]}
                />
              </Form.Item>
            </Col>
          </Row>

          {calcType === 'CUSTOM_FORMULA' ? (
            <Form.Item
              name="formula_expression"
              label={<span style={{ color: '#cbd5e1' }}>SpEL Formula Expression</span>}
              extra={<span style={{ color: '#94a3b8', fontSize: 11 }}>Example: (#BASE_SALARY * 0.10) + #HRA</span>}
              rules={[{ required: true, message: 'Formula expression is required' }]}
            >
              <Input placeholder="(#BASE_SALARY * 0.10) + #HRA" />
            </Form.Item>
          ) : (
            <Form.Item
              name="default_value"
              label={
                <span style={{ color: '#cbd5e1' }}>
                  {calcType === 'PERCENTAGE_OF_BASE' ? 'Default Percentage (%)' : 'Default Flat Amount (₹)'}
                </span>
              }
              rules={[{ required: true, message: 'Default value is required' }]}
            >
              <InputNumber
                style={{ width: '100%' }}
                min={0}
                placeholder={calcType === 'PERCENTAGE_OF_BASE' ? 'e.g. 10 for 10%' : 'e.g. 5000'}
              />
            </Form.Item>
          )}

          <Row gutter={16} style={{ marginTop: 8 }}>
            <Col span={12}>
              <Form.Item
                name="is_taxable"
                valuePropName="checked"
                label={<span style={{ color: '#cbd5e1' }}>Taxable Component</span>}
              >
                <Switch defaultChecked />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="is_statutory"
                valuePropName="checked"
                label={<span style={{ color: '#cbd5e1' }}>Statutory Requirement</span>}
              >
                <Switch />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
};

export default FormulaStudio;
