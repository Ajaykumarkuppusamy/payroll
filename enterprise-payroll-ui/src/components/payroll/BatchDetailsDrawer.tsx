import React, { useState, useEffect, useCallback } from 'react';
import { Drawer, Table, Button, Tag, Typography, Alert, message, Tooltip } from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { FilePdfOutlined } from '@ant-design/icons';
import {
  getBatchRecords,
  getPayslip,
  type PayrollBatchResponse,
  type PayrollRecordResponse,
  type PdfStatus,
} from '../../api/payroll';
import './BatchDetailsDrawer.css';

const { Text, Title } = Typography;

interface BatchDetailsDrawerProps {
  open: boolean;
  batch: PayrollBatchResponse | null;
  onClose: () => void;
}

const PDF_STATUS_CONFIG: Record<PdfStatus, { color: string; label: string }> = {
  PENDING: { color: 'default', label: 'Pending' },
  GENERATING: { color: 'blue', label: 'Generating' },
  COMPLETED: { color: 'green', label: 'Available' },
  FAILED: { color: 'red', label: 'Failed' },
};

const formatCurrency = (amount: number): string =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(amount);

const BatchDetailsDrawer: React.FC<BatchDetailsDrawerProps> = ({ open, batch, onClose }) => {
  const [records, setRecords] = useState<PayrollRecordResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pagination, setPagination] = useState<TablePaginationConfig>({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
  });

  const fetchRecords = useCallback(async (page = 1, size = 10) => {
    if (!batch) return;
    setLoading(true);
    setError(null);

    try {
      const data = await getBatchRecords(batch.batch_id, page - 1, size);
      setRecords(data.content);
      setPagination((prev: TablePaginationConfig) => ({
        ...prev,
        current: data.number + 1,
        pageSize: data.size,
        total: data.totalElements,
      }));
    } catch {
      setError('Failed to load itemised employee records for this batch.');
    } finally {
      setLoading(false);
    }
  }, [batch]);

  useEffect(() => {
    if (open && batch) {
      void fetchRecords(1, pagination.pageSize || 10);
    } else {
      setRecords([]);
    }
  }, [open, batch, fetchRecords, pagination.pageSize]);

  const handleDownloadPayslip = async (recordId: number) => {
    try {
      const { url } = await getPayslip(recordId);
      window.open(url, '_blank', 'noopener,noreferrer');
    } catch {
      message.error('Failed to generate or fetch payslip URL.');
    }
  };

  const columns: ColumnsType<PayrollRecordResponse> = [
    {
      title: 'Employee',
      key: 'employee',
      render: (_: unknown, record: PayrollRecordResponse) => (
        <div>
          <div className="record-emp-name">{record.employee_name}</div>
          <Text code className="record-emp-code">{record.employee_code}</Text>
        </div>
      ),
    },
    {
      title: 'Gross Pay',
      dataIndex: 'gross_earnings',
      key: 'gross_earnings',
      align: 'right',
      render: (val: number) => <Text className="monetary-value">{formatCurrency(val)}</Text>,
    },
    {
      title: 'Deductions',
      dataIndex: 'total_deductions',
      key: 'total_deductions',
      align: 'right',
      render: (val: number) => <Text className="monetary-value deduction">{formatCurrency(val)}</Text>,
    },
    {
      title: 'Net Pay',
      dataIndex: 'net_salary',
      key: 'net_salary',
      align: 'right',
      render: (val: number) => <Text className="monetary-value net-pay">{formatCurrency(val)}</Text>,
    },
    {
      title: 'Payslip PDF',
      dataIndex: 'pdf_status',
      key: 'pdf_status',
      render: (status: PdfStatus) => {
        const cfg = PDF_STATUS_CONFIG[status] || { color: 'default', label: status };
        return <Tag color={cfg.color}>{cfg.label}</Tag>;
      },
    },
    {
      title: 'Action',
      key: 'action',
      align: 'center',
      render: (_: unknown, record: PayrollRecordResponse) => (
        <Tooltip title={record.pdf_status === 'COMPLETED' ? 'View Payslip PDF' : 'PDF not yet available'}>
          <Button
            type="primary"
            ghost
            icon={<FilePdfOutlined />}
            size="small"
            disabled={record.pdf_status !== 'COMPLETED'}
            onClick={() => void handleDownloadPayslip(record.record_id)}
          >
            Payslip
          </Button>
        </Tooltip>
      ),
    },
  ];

  return (
    <Drawer
      title={
        <div className="drawer-title-group">
          <Title level={4} style={{ margin: 0, color: '#f8fafc' }}>Batch Snapshot Records</Title>
          {batch && (
            <Text style={{ color: '#94a3b8' }}>
              Period: {batch.month.toString().padStart(2, '0')}/{batch.year} | Batch #{batch.batch_id}
            </Text>
          )}
        </div>
      }
      open={open}
      onClose={onClose}
      width={900}
      className="batch-drawer"
    >
      {error && <Alert message={error} type="error" showIcon style={{ marginBottom: 16 }} />}

      <Table<PayrollRecordResponse>
        rowKey="record_id"
        dataSource={records}
        columns={columns}
        loading={loading}
        pagination={pagination}
        onChange={(pag) => {
          void fetchRecords(pag.current, pag.pageSize);
        }}
        size="middle"
        locale={{
          emptyText: 'No employee records found in this batch.',
        }}
      />
    </Drawer>
  );
};

export default BatchDetailsDrawer;
