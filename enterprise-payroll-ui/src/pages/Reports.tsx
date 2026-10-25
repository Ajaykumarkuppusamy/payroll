import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Button, Spin, Alert, Typography, Tag, Select, Card, Empty } from 'antd';
import {
  FilePdfOutlined,
  PlayCircleOutlined,
  DownloadOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  BarChartOutlined,
  TeamOutlined,
  SafetyCertificateOutlined,
  CalendarOutlined,
  UserOutlined,
  ApartmentOutlined,
} from '@ant-design/icons';
import useAuthStore from '../store/useAuthStore';
import { getEmployees, type Employee } from '../api/employee';
import { getBatches, type PayrollBatchResponse } from '../api/payroll';
import {
  fetchReportCatalog,
  executeReport,
  getReportPdfBlob,
  downloadReportFile,
  type ReportCatalogItem,
  type ExecuteReportResponse,
} from '../api/reports';
import './Reports.css';

const { Title, Text, Paragraph } = Typography;

const MONTH_NAMES = [
  '', 'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

const formatINR = (v: number) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(v);

const Reports: React.FC = () => {
  const { activeCompanyId } = useAuthStore();

  // Catalog & Options
  const [catalog, setCatalog] = useState<ReportCatalogItem[]>([]);
  const [selectedReportCode, setSelectedReportCode] = useState<string>('EMP_PAYSLIP');
  const [batches, setBatches] = useState<PayrollBatchResponse[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);

  // Selected Parameters
  const [selectedBatchId, setSelectedBatchId] = useState<number | null>(null);
  const [selectedEmployeeId, setSelectedEmployeeId] = useState<number | null>(null);

  // Execution & Preview State
  const [executing, setExecuting] = useState(false);
  const [loadingData, setLoadingData] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [currentJob, setCurrentJob] = useState<ExecuteReportResponse | null>(null);
  const [previewBlobUrl, setPreviewBlobUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Load catalog on mount
  useEffect(() => {
    fetchReportCatalog()
      .then((items) => {
        setCatalog(items);
        if (items.length > 0 && !selectedReportCode) {
          setSelectedReportCode(items[0].code);
        }
      })
      .catch(() => setError('Failed to load report catalog. Please verify backend service status.'));
  }, [selectedReportCode]);

  // Load company batches and employees when company changes
  useEffect(() => {
    if (!activeCompanyId) return;
    setLoadingData(true);
    setError(null);
    Promise.all([
      getBatches(activeCompanyId),
      getEmployees(activeCompanyId),
    ])
      .then(([bList, eList]) => {
        const completed = bList.filter((b) => b.status === 'COMPLETED');
        setBatches(completed);
        setEmployees(eList);
        if (completed.length > 0 && !selectedBatchId) {
          setSelectedBatchId(completed[0].batch_id);
        }
        if (eList.length > 0 && !selectedEmployeeId) {
          setSelectedEmployeeId(eList[0].id);
        }
      })
      .catch(() => setError('Failed to retrieve payroll batches or employees for the active company.'))
      .finally(() => setLoadingData(false));
  }, [activeCompanyId, selectedBatchId, selectedEmployeeId]);

  // Cleanup object URLs when preview changes
  useEffect(() => {
    return () => {
      if (previewBlobUrl) {
        window.URL.revokeObjectURL(previewBlobUrl);
      }
    };
  }, [previewBlobUrl]);

  // Current active report item from catalog
  const activeReport = useMemo(
    () => catalog.find((c) => c.code === selectedReportCode),
    [catalog, selectedReportCode]
  );

  // Reset preview when switching reports
  const handleSelectReport = (code: string) => {
    if (code === selectedReportCode) return;
    setSelectedReportCode(code);
    setCurrentJob(null);
    if (previewBlobUrl) {
      window.URL.revokeObjectURL(previewBlobUrl);
      setPreviewBlobUrl(null);
    }
    setError(null);
  };

  // Run Report: Staging -> Jasper compile/fill -> MinIO -> Blob Preview
  const handleExecuteReport = async () => {
    if (!activeCompanyId) {
      setError('Please select an active company first.');
      return;
    }

    if (selectedReportCode === 'EMP_PAYSLIP' && (!selectedBatchId || !selectedEmployeeId)) {
      setError('Both a Payroll Batch and an Employee are required for generating an individual payslip.');
      return;
    }

    if (selectedReportCode === 'PAYROLL_SUMMARY' && !selectedBatchId) {
      setError('A Payroll Batch is required for generating the company payroll register.');
      return;
    }

    setExecuting(true);
    setError(null);

    try {
      const response = await executeReport({
        reportCode: selectedReportCode,
        companyId: activeCompanyId,
        batchId: selectedBatchId ?? undefined,
        employeeId: selectedEmployeeId ?? undefined,
        format: 'PDF',
      });

      setCurrentJob(response);

      // Fetch the generated PDF blob for in-browser streaming preview
      const pdfBlob = await getReportPdfBlob(response.jobId);
      if (previewBlobUrl) {
        window.URL.revokeObjectURL(previewBlobUrl);
      }
      const newBlobUrl = window.URL.createObjectURL(pdfBlob);
      setPreviewBlobUrl(newBlobUrl);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Report generation failed. Please try again.';
      setError(msg);
    } finally {
      setExecuting(false);
    }
  };

  // Export PDF Button handler
  const handleExportPdf = async () => {
    if (!currentJob) return;
    setDownloading(true);
    try {
      const fileName = `${selectedReportCode.toLowerCase()}_${currentJob.jobId.slice(0, 8)}.pdf`;
      await downloadReportFile(currentJob.jobId, fileName);
    } catch {
      setError('Failed to download exported PDF. Please try again.');
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="reports-page-container">
      {/* ── Top Header Banner ─────────────────────────────────── */}
      <div className="reports-hero-banner">
        <div className="reports-hero-left">
          <div className="reports-hero-icon">
            <FilePdfOutlined />
          </div>
          <div>
            <Title level={3} className="reports-hero-title">
              Enterprise Reporting Center
            </Title>
            <Text className="reports-hero-subtitle">
              Staging-backed JasperReports engine with direct MinIO artifact storage &amp; high-fidelity preview
            </Text>
          </div>
        </div>
        <div className="reports-hero-right">
          <Tag color="cyan" className="reports-tag-pill">
            <SafetyCertificateOutlined /> Enterprise Jasper 6.21
          </Tag>
          <Tag color="blue" className="reports-tag-pill">
            <ApartmentOutlined /> Active Company ID: {activeCompanyId ?? 'N/A'}
          </Tag>
        </div>
      </div>

      {error && (
        <Alert
          type="error"
          message="Report Processing Notice"
          description={error}
          showIcon
          closable
          onClose={() => setError(null)}
          className="reports-alert"
        />
      )}

      {/* ── STEP 1: Report Selection Grid ─────────────────────── */}
      <div className="reports-section-card">
        <div className="reports-section-header">
          <div className="step-indicator">Step 1</div>
          <Title level={4} className="section-title">Select Report Template</Title>
        </div>

        <div className="report-catalog-grid">
          {/* Payslip */}
          <Card
            hoverable
            className={`report-option-card ${selectedReportCode === 'EMP_PAYSLIP' ? 'selected' : ''}`}
            onClick={() => handleSelectReport('EMP_PAYSLIP')}
          >
            <div className="report-card-top">
              <div className="report-badge-icon payslip-badge">
                <FilePdfOutlined />
              </div>
              {selectedReportCode === 'EMP_PAYSLIP' && (
                <Tag color="success" className="selected-tag"><CheckCircleOutlined /> Selected</Tag>
              )}
            </div>
            <div className="report-card-title">Employee Monthly Payslip</div>
            <div className="report-card-category">Payroll &amp; Compensation</div>
            <Paragraph className="report-card-description">
              Individual statutory payslip with dynamic earnings, allowances, statutory deductions (PF, Tax), and net salary snapshot.
            </Paragraph>
            <div className="report-card-params-pill">
              Parameters: Company + Pay Period + Employee
            </div>
          </Card>

          {/* Payroll Summary */}
          <Card
            hoverable
            className={`report-option-card ${selectedReportCode === 'PAYROLL_SUMMARY' ? 'selected' : ''}`}
            onClick={() => handleSelectReport('PAYROLL_SUMMARY')}
          >
            <div className="report-card-top">
              <div className="report-badge-icon summary-badge">
                <BarChartOutlined />
              </div>
              {selectedReportCode === 'PAYROLL_SUMMARY' && (
                <Tag color="success" className="selected-tag"><CheckCircleOutlined /> Selected</Tag>
              )}
            </div>
            <div className="report-card-title">Payroll Master Register</div>
            <div className="report-card-category">Audit &amp; Financials</div>
            <Paragraph className="report-card-description">
              Complete company-wide payroll register detailing all employee disbursements, bank accounts, PAN/tax IDs, gross earnings, and batch totals.
            </Paragraph>
            <div className="report-card-params-pill">
              Parameters: Company + Pay Period
            </div>
          </Card>

          {/* Headcount Cost */}
          <Card
            hoverable
            className={`report-option-card ${selectedReportCode === 'HEADCOUNT_COST' ? 'selected' : ''}`}
            onClick={() => handleSelectReport('HEADCOUNT_COST')}
          >
            <div className="report-card-top">
              <div className="report-badge-icon cost-badge">
                <TeamOutlined />
              </div>
              {selectedReportCode === 'HEADCOUNT_COST' && (
                <Tag color="success" className="selected-tag"><CheckCircleOutlined /> Selected</Tag>
              )}
            </div>
            <div className="report-card-title">Department Cost &amp; Headcount</div>
            <div className="report-card-category">Executive Analytics</div>
            <Paragraph className="report-card-description">
              Departmental breakdown of active personnel, total base salary payroll expenditure, and average salary benchmarks.
            </Paragraph>
            <div className="report-card-params-pill">
              Parameters: Company
            </div>
          </Card>
        </div>
      </div>

      {/* ── STEP 2: Dynamic Parameters Panel ─────────────────── */}
      <div className="reports-section-card">
        <div className="reports-section-header">
          <div className="step-indicator">Step 2</div>
          <div>
            <Title level={4} className="section-title">Configure Parameters</Title>
            <Text className="section-subtitle">
              Configuring filters for: <strong>{activeReport?.title || selectedReportCode}</strong>
            </Text>
          </div>
        </div>

        <div className="parameter-form-row">
          {/* Payroll Batch Selector (needed for EMP_PAYSLIP and PAYROLL_SUMMARY) */}
          {(selectedReportCode === 'EMP_PAYSLIP' || selectedReportCode === 'PAYROLL_SUMMARY') && (
            <div className="parameter-item">
              <label className="parameter-label">
                <CalendarOutlined /> Payroll Period (Completed Batches)
              </label>
              <Select
                value={selectedBatchId ?? undefined}
                onChange={(val) => {
                  setSelectedBatchId(val);
                  setPreviewBlobUrl(null);
                  setCurrentJob(null);
                }}
                placeholder="Choose completed payroll batch..."
                className="parameter-select"
                loading={loadingData}
                options={batches.map((b) => ({
                  value: b.batch_id,
                  label: `${MONTH_NAMES[b.month]} ${b.year}  —  Batch #${b.batch_id} (${formatINR(b.total_net_pay)})`,
                }))}
              />
            </div>
          )}

          {/* Employee Selector (needed for EMP_PAYSLIP) */}
          {selectedReportCode === 'EMP_PAYSLIP' && (
            <div className="parameter-item">
              <label className="parameter-label">
                <UserOutlined /> Target Employee
              </label>
              <Select
                value={selectedEmployeeId ?? undefined}
                onChange={(val) => {
                  setSelectedEmployeeId(val);
                  setPreviewBlobUrl(null);
                  setCurrentJob(null);
                }}
                placeholder="Search and choose employee..."
                showSearch
                optionFilterProp="label"
                className="parameter-select"
                loading={loadingData}
                options={employees.map((e) => ({
                  value: e.id,
                  label: `${e.employee_code}  •  ${e.first_name} ${e.last_name} (${e.department_name || 'General'})`,
                }))}
              />
            </div>
          )}

          {/* Action Button: Run / Generate Report */}
          <div className="parameter-action-group">
            <Button
              type="primary"
              size="large"
              icon={<PlayCircleOutlined />}
              onClick={handleExecuteReport}
              loading={executing}
              className="run-report-btn"
            >
              {executing ? 'Executing Staging & Jasper...' : 'Generate Report'}
            </Button>
          </div>
        </div>
      </div>

      {/* ── STEP 3: Report Preview & Export ──────────────────── */}
      {previewBlobUrl && currentJob && (
        <div className="reports-section-card preview-section-card">
          <div className="preview-action-bar">
            <div className="preview-meta-group">
              <div className="preview-badge-status">
                <Tag color="success" className="job-status-pill">
                  <CheckCircleOutlined /> GENERATED
                </Tag>
                <Text strong className="preview-job-title">
                  {activeReport?.title}
                </Text>
              </div>
              <Text className="preview-job-id">
                Job ID: <code>{currentJob.jobId}</code>
              </Text>
            </div>

            {/* ONLY Export PDF is provided; Print button has been permanently removed */}
            <div className="preview-buttons">
              <Button
                type="primary"
                size="large"
                icon={<DownloadOutlined />}
                onClick={handleExportPdf}
                loading={downloading}
                className="export-pdf-primary-btn"
              >
                Export PDF
              </Button>
              <Button
                icon={<ReloadOutlined />}
                size="large"
                onClick={handleExecuteReport}
                loading={executing}
                className="regenerate-btn"
              >
                Regenerate
              </Button>
            </div>
          </div>

          {/* In-Browser PDF Viewer */}
          <div className="pdf-iframe-container">
            <iframe
              src={previewBlobUrl}
              title="JasperReports Document Preview"
              className="pdf-preview-iframe"
            />
          </div>
        </div>
      )}

      {/* Placeholder when report hasn't been generated yet */}
      {!previewBlobUrl && !executing && (
        <div className="report-standby-banner">
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={
              <div>
                <Text strong style={{ fontSize: '15px', color: '#334155' }}>
                  No Active Report Preview
                </Text>
                <div style={{ color: '#64748b', marginTop: 4 }}>
                  Select a template and parameters above, then click <strong>Generate Report</strong> to snapshot data into the staging table and compile your Jasper PDF.
                </div>
              </div>
            }
          />
        </div>
      )}

      {executing && (
        <div className="report-generating-overlay">
          <Spin size="large" />
          <div className="generating-text-title">Generating Enterprise Report</div>
          <div className="generating-text-sub">
            Invoking PostgreSQL staging procedure &rarr; compiling JasperReports layout &rarr; saving artifact to MinIO storage...
          </div>
        </div>
      )}
    </div>
  );
};

export default Reports;
