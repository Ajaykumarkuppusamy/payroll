import React, { useEffect, useState, useCallback } from 'react';
import {
  Table,
  Tag,
  Typography,
  Alert,
  Space,
  Button,
  Badge,
  Modal,
  Form,
  InputNumber,
  Select,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlayCircleOutlined, ReloadOutlined } from '@ant-design/icons';
import {
  getBatches,
  runPayroll,
  type PayrollBatchResponse,
  type BatchStatus,
} from '../api/payroll';
import useAuthStore from '../store/useAuthStore';
import BatchDetailsDrawer from '../components/payroll/BatchDetailsDrawer';
import './PayrollConsole.css';

const { Title, Text } = Typography;

const BATCH_STATUS_CONFIG: Record<BatchStatus, { color: string; label: string }> = {
  DRAFT: { color: 'default', label: 'Draft' },
  PENDING: { color: 'gold', label: 'Pending' },
  PROCESSING: { color: 'blue', label: 'Processing' },
  COMPLETED: { color: 'green', label: 'Completed' },
  FAILED: { color: 'red', label: 'Failed' },
};

const formatCurrency = (amount: number): string =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(amount);

const PayrollConsole: React.FC = () => {
  const { activeCompanyId } = useAuthStore();
  const [batches, setBatches] = useState<PayrollBatchResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [running, setRunning] = useState(false);
  const [form] = Form.useForm();

  // Drawer State
  const [selectedBatch, setSelectedBatch] = useState<PayrollBatchResponse | null>(null);

  const fetchBatches = useCallback(async () => {
    if (!activeCompanyId) return;
    setLoading(true);
    setError(null);

    try {
      const data = await getBatches(activeCompanyId);
      setBatches(data);
    } catch {
      setError('Failed to load payroll batches from server.');
    } finally {
      setLoading(false);
    }
  }, [activeCompanyId]);

  useEffect(() => {
    void fetchBatches();
  }, [fetchBatches]);

  const handleRunPayroll = async (values: { year: number; month: number }) => {
    if (!activeCompanyId) return;
    setRunning(true);

    try {
      await runPayroll(activeCompanyId, values.year, values.month);
      message.success(`Payroll successfully completed for ${values.month}/${values.year}`);
      setIsModalOpen(false);
      void fetchBatches();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { message?: string } } };
      if (axiosErr.response?.status === 409) {
        message.error(axiosErr.response.data?.message || 'A completed batch already exists for this pay period.');
      } else {
        message.error(axiosErr.response?.data?.message || 'Failed to execute payroll run.');
      }
    } finally {
      setRunning(false);
    }
  };

  const columns: ColumnsType<PayrollBatchResponse> = [
    {
      title: 'Period',
      key: 'period',
      render: (_: unknown, record: PayrollBatchResponse) => (
        <Text className="batch-period">
          {record.month.toString().padStart(2, '0')} / {record.year}
        </Text>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: BatchStatus) => {
        const cfg = BATCH_STATUS_CONFIG[status] || { color: 'default', label: status };
        return <Tag color={cfg.color} className="status-tag">{cfg.label}</Tag>;
      },
    },
    {
      title: 'Total Gross Pay',
      dataIndex: 'total_gross_pay',
      key: 'total_gross_pay',
      align: 'right',
      render: (val: number) => <Text className="monetary-value">{formatCurrency(val)}</Text>,
    },
    {
      title: 'Total Deductions',
      dataIndex: 'total_deductions',
      key: 'total_deductions',
      align: 'right',
      render: (val: number) => <Text className="monetary-value deduction">{formatCurrency(val)}</Text>,
    },
    {
      title: 'Net Pay',
      dataIndex: 'total_net_pay',
      key: 'total_net_pay',
      align: 'right',
      render: (val: number) => <Text className="monetary-value net-pay">{formatCurrency(val)}</Text>,
    },
    {
      title: 'Employees',
      dataIndex: 'total_employees',
      key: 'total_employees',
      align: 'right',
      render: (val: number) => <Text style={{ color: '#f8fafc' }}>{val}</Text>,
    },
    {
      title: 'Run By',
      dataIndex: 'executed_by_username',
      key: 'executed_by_username',
      render: (val: string) => <Text style={{ color: '#94a3b8' }}>{val}</Text>,
    },
  ];

  return (
    <div className="console-page">
      <div className="console-header">
        <div className="console-title-group">
          <div className="console-icon">
            <PlayCircleOutlined />
          </div>
          <div>
            <Title level={4} className="console-title">Payroll Console</Title>
            <Text className="console-subtitle">
              Execution Ledger ·{' '}
              <Badge color="blue" text={<span className="stat-text">{batches.length} batches</span>} />
            </Text>
          </div>
        </div>

        <Space size={12}>
          <Button icon={<ReloadOutlined />} onClick={() => void fetchBatches()} loading={loading} className="refresh-btn" />
          <Button type="primary" icon={<PlayCircleOutlined />} onClick={() => setIsModalOpen(true)} className="run-payroll-btn">
            Run Payroll
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
          className="console-error"
          action={<Button size="small" onClick={() => void fetchBatches()}>Retry</Button>}
        />
      )}

      {!activeCompanyId && !loading && (
        <Alert
          message="No company selected"
          description="Select a company from the header to view payroll runs."
          type="info"
          showIcon
          className="console-error"
        />
      )}

      <div className="console-table-wrapper">
        <Table<PayrollBatchResponse>
          rowKey="batch_id"
          dataSource={batches}
          columns={columns}
          loading={loading}
          pagination={{ pageSize: 10 }}
          className="console-table"
          locale={{
            emptyText: 'No payroll batches have been run for this company yet. Click "Run Payroll" to initiate your first monthly pay cycle.',
          }}
          onRow={(record) => ({
            onClick: () => setSelectedBatch(record),
            className: 'clickable-row',
          })}
        />
      </div>

      <Modal
        title="Execute Monthly Payroll Run"
        open={isModalOpen}
        onCancel={() => setIsModalOpen(false)}
        footer={null}
        destroyOnClose
        className="run-payroll-modal"
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={(vals) => void handleRunPayroll(vals as { year: number; month: number })}
        >
          <Form.Item
            name="year"
            label="Year"
            rules={[{ required: true, message: 'Please enter a year' }]}
            initialValue={new Date().getFullYear()}
          >
            <InputNumber min={2020} max={2030} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="month"
            label="Month"
            rules={[{ required: true, message: 'Please select a month' }]}
            initialValue={new Date().getMonth() + 1}
          >
            <Select>
              {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
                <Select.Option key={m} value={m}>
                  {new Date(0, m - 1).toLocaleString('default', { month: 'long' })}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, textAlign: 'right', marginTop: 24 }}>
            <Button onClick={() => setIsModalOpen(false)} style={{ marginRight: 8 }} disabled={running}>
              Cancel
            </Button>
            <Button
              type="primary"
              htmlType="submit"
              loading={running}
              style={{ background: 'linear-gradient(135deg, #3b82f6, #2563eb)', border: 'none' }}
            >
              Confirm & Execute Batch
            </Button>
          </Form.Item>
        </Form>
      </Modal>

      <BatchDetailsDrawer
        open={!!selectedBatch}
        batch={selectedBatch}
        onClose={() => setSelectedBatch(null)}
      />
    </div>
  );
};

export default PayrollConsole;
