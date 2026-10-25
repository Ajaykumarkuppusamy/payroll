import React, { useEffect, useState, useCallback } from 'react';
import { Typography, Spin, Alert, Button, Tag } from 'antd';
import {
  DashboardOutlined,
  TeamOutlined,
  DollarCircleOutlined,
  RiseOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  ArrowRightOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import useAuthStore from '../store/useAuthStore';
import { getDashboardData, type DashboardData } from '../api/dashboard';
import './Dashboard.css';

const { Title, Text } = Typography;

const formatINR = (val: number): string =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(val);

const Dashboard: React.FC = () => {
  const { activeCompanyId } = useAuthStore();
  const navigate = useNavigate();
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchDashboard = useCallback(async () => {
    if (!activeCompanyId) return;
    setLoading(true);
    setError(null);

    try {
      const res = await getDashboardData(activeCompanyId);
      setData(res);
    } catch {
      setError('Unable to load dashboard metrics. Verify backend connectivity.');
    } finally {
      setLoading(false);
    }
  }, [activeCompanyId]);

  useEffect(() => {
    void fetchDashboard();
  }, [fetchDashboard]);

  if (loading && !data) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '50vh' }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div className="dashboard-page">
      {/* ── Page Header ────────────────────────────────────────────── */}
      <div className="dashboard-header">
        <div className="dashboard-title-group">
          <div className="dashboard-icon">
            <DashboardOutlined />
          </div>
          <div>
            <Title level={4} className="dashboard-title">
              Payroll Command Center
            </Title>
            <Text className="dashboard-subtitle">
              Live enterprise overview, workforce analytics, and payroll ledger status
            </Text>
          </div>
        </div>

        <div className="dashboard-actions">
          <Button
            icon={<ReloadOutlined />}
            onClick={() => void fetchDashboard()}
            className="quick-action-btn quick-action-secondary"
            loading={loading}
          >
            Refresh
          </Button>
          <Button
            type="primary"
            icon={<PlayCircleOutlined />}
            onClick={() => navigate('/payroll-console')}
            className="quick-action-btn quick-action-primary"
          >
            Payroll Console
          </Button>
        </div>
      </div>

      {error && (
        <Alert
          type="error"
          message="Analytics Service Unavailable"
          description={error}
          showIcon
          style={{ borderRadius: 12 }}
        />
      )}

      {/* ── KPI Cards ──────────────────────────────────────────────── */}
      <div className="kpi-grid">
        {/* Card 1: Total Employees */}
        <div className="kpi-card">
          <div className="kpi-card-glow glow-indigo" />
          <div className="kpi-header">
            <span className="kpi-label">Workforce Roster</span>
            <div className="kpi-icon-badge badge-indigo">
              <TeamOutlined />
            </div>
          </div>
          <div className="kpi-value">{data?.total_employees ?? 0}</div>
          <div className="kpi-subtext">
            <span className="kpi-subtag" style={{ color: '#34d399' }}>
              ● {data?.active_employees ?? 0} Active
            </span>
            <span className="kpi-subtag" style={{ color: '#fbbf24' }}>
              ● {data?.on_leave_employees ?? 0} Leave
            </span>
            <span className="kpi-subtag" style={{ color: '#60a5fa' }}>
              ● {data?.probation_employees ?? 0} Probation
            </span>
          </div>
        </div>

        {/* Card 2: Monthly Base Payroll */}
        <div className="kpi-card">
          <div className="kpi-card-glow glow-emerald" />
          <div className="kpi-header">
            <span className="kpi-label">Active Base Payroll</span>
            <div className="kpi-icon-badge badge-emerald">
              <DollarCircleOutlined />
            </div>
          </div>
          <div className="kpi-value" style={{ color: '#34d399' }}>
            {formatINR(data?.monthly_payroll_estimate ?? 0)}
          </div>
          <div className="kpi-subtext">
            Calculated across {data?.active_employees ?? 0} eligible employees
          </div>
        </div>

        {/* Card 3: Average Salary */}
        <div className="kpi-card">
          <div className="kpi-card-glow glow-cyan" />
          <div className="kpi-header">
            <span className="kpi-label">Average Base Salary</span>
            <div className="kpi-icon-badge badge-cyan">
              <RiseOutlined />
            </div>
          </div>
          <div className="kpi-value" style={{ color: '#38bdf8' }}>
            {formatINR(data?.avg_salary ?? 0)}
          </div>
          <div className="kpi-subtext">
            Per active salaried employee / month
          </div>
        </div>

        {/* Card 4: Last Payroll Run */}
        <div className="kpi-card">
          <div className="kpi-card-glow glow-purple" />
          <div className="kpi-header">
            <span className="kpi-label">Last Payroll Cycle</span>
            <div className="kpi-icon-badge badge-purple">
              <PlayCircleOutlined />
            </div>
          </div>
          {data?.last_batch ? (
            <>
              <div className="kpi-value">
                {data.last_batch.month}/{data.last_batch.year}
              </div>
              <div className="kpi-subtext">
                <Tag
                  color={
                    data.last_batch.status === 'COMPLETED'
                      ? 'green'
                      : data.last_batch.status === 'PROCESSING'
                        ? 'processing'
                        : 'default'
                  }
                  icon={
                    data.last_batch.status === 'COMPLETED' ? (
                      <CheckCircleOutlined />
                    ) : (
                      <ClockCircleOutlined />
                    )
                  }
                >
                  {data.last_batch.status}
                </Tag>
                <span>Net: {formatINR(data.last_batch.total_net_pay)}</span>
              </div>
            </>
          ) : (
            <>
              <div className="kpi-value" style={{ fontSize: 18, color: '#94a3b8' }}>
                No Runs Yet
              </div>
              <div className="kpi-subtext">
                <Button
                  type="link"
                  size="small"
                  onClick={() => navigate('/payroll-console')}
                  style={{ padding: 0, color: '#818cf8' }}
                >
                  Run initial payroll cycle →
                </Button>
              </div>
            </>
          )}
        </div>
      </div>

      {/* ── Content Grid ───────────────────────────────────────────── */}
      <div className="dashboard-content-grid">
        {/* Department Breakdown */}
        <div className="dashboard-card">
          <div className="card-title-row">
            <h3 className="card-title">Department Distribution</h3>
            <span
              className="card-action-link"
              onClick={() => navigate('/employees')}
            >
              View Directory <ArrowRightOutlined />
            </span>
          </div>

          <div className="dept-list">
            {(data?.department_distribution ?? []).map((dept) => {
              const total = data?.total_employees || 1;
              const pct = Math.round((dept.count / total) * 100);
              return (
                <div key={dept.department_name} className="dept-item">
                  <div className="dept-info">
                    <span className="dept-name">{dept.department_name}</span>
                    <span className="dept-count-badge">
                      {dept.count} staff ({pct}%)
                    </span>
                  </div>
                  <div className="dept-progress-bar-bg">
                    <div
                      className="dept-progress-bar-fill"
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Recent Onboarded Employees */}
        <div className="dashboard-card">
          <div className="card-title-row">
            <h3 className="card-title">Recent Joiners</h3>
            <span
              className="card-action-link"
              onClick={() => navigate('/employees')}
            >
              Full Roster <ArrowRightOutlined />
            </span>
          </div>

          <table className="recent-emp-table">
            <thead>
              <tr>
                <th>Employee</th>
                <th>Department</th>
                <th>Status</th>
                <th>Hire Date</th>
              </tr>
            </thead>
            <tbody>
              {(data?.recent_employees ?? []).map((emp) => {
                const initials = `${emp.first_name[0]}${emp.last_name[0]}`.toUpperCase();
                return (
                  <tr key={emp.id}>
                    <td>
                      <div className="recent-emp-cell">
                        <div className="emp-tiny-avatar">{initials}</div>
                        <div>
                          <div style={{ fontWeight: 500 }}>
                            {emp.first_name} {emp.last_name}
                          </div>
                          <span className="emp-code-chip">{emp.employee_code}</span>
                        </div>
                      </div>
                    </td>
                    <td>
                      <Tag color="geekblue">{emp.department_name}</Tag>
                    </td>
                    <td>
                      <Tag
                        color={
                          emp.employment_status === 'ACTIVE'
                            ? 'green'
                            : emp.employment_status === 'ON_LEAVE'
                              ? 'gold'
                              : emp.employment_status === 'PROBATION'
                                ? 'blue'
                                : 'red'
                        }
                      >
                        {emp.employment_status}
                      </Tag>
                    </td>
                    <td style={{ color: '#94a3b8', fontSize: 12 }}>
                      {emp.hire_date}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
