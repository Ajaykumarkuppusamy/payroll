import React, { useState, useEffect, useCallback } from 'react';
import { Select, Button, Spin, Alert, Typography, Tag, Divider, Empty } from 'antd';
import {
  FilePdfOutlined,
  FileExcelOutlined,
  PrinterOutlined,
  UserOutlined,
  BankOutlined,
  CalendarOutlined,
  DownloadOutlined,
} from '@ant-design/icons';
import useAuthStore from '../store/useAuthStore';
import { getEmployees, type Employee } from '../api/employee';
import { getBatches, getBatchRecords, type PayrollBatchResponse, type PayrollRecordResponse } from '../api/payroll';
import { downloadPayslipPdf, downloadPayrollExcel } from '../api/reports';
import './Reports.css';

const { Title, Text } = Typography;

const formatINR = (v: number) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(v);

const MONTH_NAMES = [
  '', 'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

const Reports: React.FC = () => {
  const { activeCompanyId } = useAuthStore();

  const [employees, setEmployees] = useState<Employee[]>([]);
  const [batches, setBatches]     = useState<PayrollBatchResponse[]>([]);
  const [selectedEmployee, setSelectedEmployee] = useState<number | null>(null);
  const [selectedBatch, setSelectedBatch]       = useState<number | null>(null);
  const [record, setRecord]       = useState<PayrollRecordResponse | null>(null);
  const [loading, setLoading]     = useState(false);
  const [pdfLoading, setPdfLoading]     = useState(false);
  const [excelLoading, setExcelLoading] = useState(false);
  const [error, setError]         = useState<string | null>(null);

  /* Load employees + completed batches on mount */
  useEffect(() => {
    if (!activeCompanyId) return;
    setLoading(true);
    Promise.all([
      getEmployees(activeCompanyId),
      getBatches(activeCompanyId),
    ])
      .then(([emps, bats]) => {
        setEmployees(emps);
        setBatches(bats.filter((b) => b.status === 'COMPLETED'));
      })
      .catch(() => setError('Failed to load data. Ensure the backend is running.'))
      .finally(() => setLoading(false));
  }, [activeCompanyId]);

  /* Load payslip record when employee + batch are both selected */
  const loadRecord = useCallback(async () => {
    if (!selectedBatch || !selectedEmployee) return;
    setLoading(true);
    setError(null);
    setRecord(null);
    try {
      const page = await getBatchRecords(selectedBatch);
      const records: PayrollRecordResponse[] = page.content ?? (page as unknown as PayrollRecordResponse[]);
      const match = records.find((r) => r.employee_id === selectedEmployee);
      if (match) {
        setRecord(match);
      } else {
        setError('No payroll record found for this employee in the selected batch.');
      }
    } catch {
      setError('Failed to load payslip. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [selectedBatch, selectedEmployee]);

  useEffect(() => { void loadRecord(); }, [loadRecord]);

  const handleDownloadPdf = async () => {
    if (!selectedBatch || !selectedEmployee) return;
    setPdfLoading(true);
    try {
      await downloadPayslipPdf(selectedBatch, selectedEmployee);
    } catch {
      setError('PDF download failed. Ensure Jasper Reports and MinIO are configured in the backend.');
    } finally {
      setPdfLoading(false);
    }
  };

  const handleDownloadExcel = async () => {
    if (!selectedBatch) return;
    setExcelLoading(true);
    try {
      await downloadPayrollExcel(selectedBatch);
    } catch {
      setError('Excel download failed. Ensure the backend reports endpoint is available.');
    } finally {
      setExcelLoading(false);
    }
  };

  const emp   = employees.find((e) => e.id === selectedEmployee);
  const batch = batches.find((b) => b.batch_id === selectedBatch);

  /* Details are line items on the record */
  const details  = (record?.details ?? []) as Array<{
    componentCode: string;
    componentName: string;
    componentType: 'EARNING' | 'DEDUCTION';
    amount: number;
    isStatutory: boolean;
  }>;
  const earnings   = details.filter((d) => d.componentType === 'EARNING');
  const deductions = details.filter((d) => d.componentType === 'DEDUCTION');

  return (
    <div className="reports-page">
      {/* ── Header ─────────────────────────────────────── */}
      <div className="reports-header">
        <div className="reports-title-group">
          <div className="reports-icon"><FilePdfOutlined /></div>
          <div>
            <Title level={4} className="reports-title">Reports &amp; Payslips</Title>
            <Text className="reports-subtitle">Download payslips and payroll summaries</Text>
          </div>
        </div>
        <div className="reports-actions">
          <Button
            icon={<FileExcelOutlined />}
            className="excel-btn"
            loading={excelLoading}
            disabled={!selectedBatch}
            onClick={handleDownloadExcel}
          >
            Download Payroll Excel
          </Button>
        </div>
      </div>

      {error && (
        <Alert type="error" message={error} showIcon closable onClose={() => setError(null)} />
      )}

      {/* ── Filter Panel ────────────────────────────────── */}
      <div className="reports-filters">
        <div className="filter-card">
          <div className="filter-group">
            <label className="filter-label">
              <CalendarOutlined /> &nbsp;Payroll Batch (Completed)
            </label>
            <Select
              value={selectedBatch ?? undefined}
              onChange={(v: number) => { setSelectedBatch(v); setRecord(null); setError(null); }}
              placeholder="Select payroll period..."
              className="filter-select"
              allowClear
              loading={loading && batches.length === 0}
              options={batches.map((b) => ({
                value: b.batch_id,
                label: `${MONTH_NAMES[b.month]} ${b.year}  •  ${formatINR(b.total_net_pay)}`,
              }))}
            />
          </div>
          <div className="filter-group">
            <label className="filter-label">
              <UserOutlined /> &nbsp;Employee
            </label>
            <Select
              value={selectedEmployee ?? undefined}
              onChange={(v: number) => { setSelectedEmployee(v); setRecord(null); setError(null); }}
              placeholder="Select employee..."
              className="filter-select"
              allowClear
              showSearch
              optionFilterProp="label"
              loading={loading && employees.length === 0}
              options={employees.map((e) => ({
                value: e.id,
                label: `${e.employee_code}  —  ${e.first_name} ${e.last_name}`,
              }))}
            />
          </div>
        </div>
      </div>

      {/* ── Loading ──────────────────────────────────────── */}
      {loading && (
        <div className="reports-loading"><Spin size="large" /></div>
      )}

      {/* ── No record message ─────────────────────────── */}
      {!loading && selectedBatch && selectedEmployee && !record && !error && (
        <Empty description="No payslip found for this selection." className="reports-empty" />
      )}

      {/* ═══════════════════════════════════════════════ */}
      {/* PAYSLIP PREVIEW                                */}
      {/* ═══════════════════════════════════════════════ */}
      {!loading && record && emp && batch && (
        <div className="payslip-preview">

          {/* Header */}
          <div className="payslip-header">
            <div className="payslip-company-block">
              <div className="payslip-company-logo">💼</div>
              <div>
                <div className="payslip-company-name">
                  {emp.email.split('@')[1]?.split('.')[0]?.toUpperCase() ?? 'COMPANY'}
                </div>
                <div className="payslip-company-sub">
                  Payslip for the month of {MONTH_NAMES[batch.month]} {batch.year}
                </div>
              </div>
            </div>
            <div className="payslip-actions">
              <Button
                type="primary"
                icon={<FilePdfOutlined />}
                className="payslip-pdf-btn"
                loading={pdfLoading}
                onClick={handleDownloadPdf}
              >
                Export PDF
              </Button>
              <Button
                icon={<PrinterOutlined />}
                className="payslip-print-btn"
                onClick={() => window.print()}
              >
                Print
              </Button>
            </div>
          </div>

          {/* Employee Info Grid */}
          <div className="payslip-emp-grid">
            <div className="payslip-info-block">
              <div className="payslip-info-label">Employee Name</div>
              <div className="payslip-info-value">{emp.first_name} {emp.last_name}</div>
            </div>
            <div className="payslip-info-block">
              <div className="payslip-info-label">Employee Code</div>
              <div className="payslip-info-value mono">{emp.employee_code}</div>
            </div>
            <div className="payslip-info-block">
              <div className="payslip-info-label">Department</div>
              <div className="payslip-info-value">{emp.department_name}</div>
            </div>
            <div className="payslip-info-block">
              <div className="payslip-info-label">Email</div>
              <div className="payslip-info-value">{emp.email}</div>
            </div>
            <div className="payslip-info-block">
              <div className="payslip-info-label">Employment Status</div>
              <div className="payslip-info-value">
                <Tag color={emp.employment_status === 'ACTIVE' ? 'green' : 'orange'}>
                  {emp.employment_status}
                </Tag>
              </div>
            </div>
            <div className="payslip-info-block">
              <div className="payslip-info-label">Pay Period</div>
              <div className="payslip-info-value">{MONTH_NAMES[batch.month]} {batch.year}</div>
            </div>
            <div className="payslip-info-block">
              <div className="payslip-info-label">Hire Date</div>
              <div className="payslip-info-value">{emp.hire_date}</div>
            </div>
            <div className="payslip-info-block">
              <div className="payslip-info-label">Base Salary</div>
              <div className="payslip-info-value mono">{formatINR(emp.base_salary)}</div>
            </div>
          </div>

          <Divider className="payslip-divider" />

          {/* Earnings & Deductions */}
          <div className="payslip-breakdown-grid">
            {/* Earnings */}
            <div className="payslip-section">
              <div className="payslip-section-title earnings-title">💰 Earnings</div>
              <table className="payslip-table">
                <thead>
                  <tr>
                    <th>Component</th>
                    <th className="text-right">Amount</th>
                  </tr>
                </thead>
                <tbody>
                  {earnings.length > 0 ? earnings.map((d) => (
                    <tr key={d.componentCode}>
                      <td>
                        {d.componentName}
                        {d.isStatutory && (
                          <Tag color="blue" style={{ marginLeft: 6, fontSize: 10 }}>Statutory</Tag>
                        )}
                      </td>
                      <td className="text-right amount-earning">{formatINR(d.amount)}</td>
                    </tr>
                  )) : (
                    <tr><td colSpan={2} style={{ color: '#94a3b8', textAlign: 'center', padding: '16px 0' }}>No earnings data</td></tr>
                  )}
                </tbody>
                <tfoot>
                  <tr className="payslip-subtotal">
                    <td><strong>Gross Earnings</strong></td>
                    <td className="text-right amount-earning">
                      <strong>{formatINR(record.gross_earnings)}</strong>
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>

            {/* Deductions */}
            <div className="payslip-section">
              <div className="payslip-section-title deductions-title">📉 Deductions</div>
              <table className="payslip-table">
                <thead>
                  <tr>
                    <th>Component</th>
                    <th className="text-right">Amount</th>
                  </tr>
                </thead>
                <tbody>
                  {deductions.length > 0 ? deductions.map((d) => (
                    <tr key={d.componentCode}>
                      <td>
                        {d.componentName}
                        {d.isStatutory && (
                          <Tag color="orange" style={{ marginLeft: 6, fontSize: 10 }}>Statutory</Tag>
                        )}
                      </td>
                      <td className="text-right amount-deduction">{formatINR(d.amount)}</td>
                    </tr>
                  )) : (
                    <tr><td colSpan={2} style={{ color: '#94a3b8', textAlign: 'center', padding: '16px 0' }}>No deductions data</td></tr>
                  )}
                </tbody>
                <tfoot>
                  <tr className="payslip-subtotal">
                    <td><strong>Total Deductions</strong></td>
                    <td className="text-right amount-deduction">
                      <strong>{formatINR(record.total_deductions)}</strong>
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>

          {/* Net Pay Banner */}
          <div className="payslip-net-pay">
            <div className="net-pay-label">Net Pay (Take Home)</div>
            <div className="net-pay-value">{formatINR(record.net_salary)}</div>
            <div className="net-pay-sub">
              Computer-generated payslip — {MONTH_NAMES[batch.month]} {batch.year}
            </div>
          </div>

          {/* Footer */}
          <div className="payslip-footer">
            <Tag color="green">COMPLETED</Tag>
            <span className="payslip-footer-note">
              This is a computer-generated payslip. No signature required.
            </span>
          </div>
        </div>
      )}

      {/* ── Report Catalogue (shown when no payslip loaded) ─── */}
      {!record && !loading && (
        <div className="report-catalogue">
          <div className="catalogue-title">Available Reports</div>
          <div className="catalogue-grid">
            <div className="report-card">
              <div className="report-card-icon pdf-icon"><FilePdfOutlined /></div>
              <div className="report-card-title">Employee Payslip (PDF)</div>
              <div className="report-card-desc">
                Individual payslip for an employee for a specific payroll month. Includes earnings breakdown, deductions, and net pay via JasperReports.
              </div>
              <div className="report-card-how">
                Select a batch + employee above, then click <strong>Export PDF</strong>.
              </div>
            </div>

            <div className="report-card">
              <div className="report-card-icon excel-icon"><FileExcelOutlined /></div>
              <div className="report-card-title">Payroll Summary (Excel)</div>
              <div className="report-card-desc">
                Full payroll summary for all employees in a batch — gross, deductions, net pay per employee. Suitable for accounts and finance teams.
              </div>
              <div className="report-card-how">
                Select a completed batch above, then click <strong>Download Payroll Excel</strong>.
              </div>
            </div>

            <div className="report-card">
              <div className="report-card-icon print-icon"><DownloadOutlined /></div>
              <div className="report-card-title">Department Headcount (PDF)</div>
              <div className="report-card-desc">
                Summary of active employees by department — headcount, salary ranges, and status breakdown. Useful for HR reporting and compliance.
              </div>
              <div className="report-card-how">
                <BankOutlined /> Coming soon — generates department-level HR summary.
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Reports;
