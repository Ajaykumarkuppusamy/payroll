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
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import {
  SearchOutlined,
  ReloadOutlined,
  UserAddOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import { getEmployees, type Employee, type EmploymentStatus } from '../api/employee';
import useAuthStore from '../store/useAuthStore';
import AddEmployeeModal from '../components/employees/AddEmployeeModal';
import './EmployeeDirectory.css';

const { Title, Text } = Typography;

// ── Status Badge Config ───────────────────────────────────────────────────────

const STATUS_CONFIG: Record<
  EmploymentStatus,
  { color: string; label: string }
> = {
  ACTIVE:     { color: 'green',   label: 'Active'     },
  ON_LEAVE:   { color: 'gold',    label: 'On Leave'   },
  PROBATION:  { color: 'blue',    label: 'Probation'  },
  TERMINATED: { color: 'red',     label: 'Terminated' },
  RESIGNED:   { color: 'default', label: 'Resigned'   },
};

// ── Currency Formatter ────────────────────────────────────────────────────────

const formatINR = (amount: number): string =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
  }).format(amount);

// ── Table Columns ─────────────────────────────────────────────────────────────

const buildColumns = (): ColumnsType<Employee> => [
  {
    title: 'Code',
    dataIndex: 'employee_code',
    key: 'employee_code',
    width: 110,
    sorter: (a, b) => a.employee_code.localeCompare(b.employee_code),
    render: (code: string) => (
      <span className="employee-code">{code}</span>
    ),
  },
  {
    title: 'Employee',
    key: 'name',
    sorter: (a, b) =>
      `${a.first_name} ${a.last_name}`.localeCompare(
        `${b.first_name} ${b.last_name}`,
      ),
    render: (_: unknown, record: Employee) => {
      const initials =
        `${record.first_name[0]}${record.last_name[0]}`.toUpperCase();
      return (
        <div className="employee-name-cell">
          <div className="employee-avatar">{initials}</div>
          <div>
            <div className="employee-full-name">
              {record.first_name} {record.last_name}
            </div>
            <div className="employee-hire-date">
              Hired {record.hire_date}
            </div>
          </div>
        </div>
      );
    },
  },
  {
    title: 'Email',
    dataIndex: 'email',
    key: 'email',
    render: (email: string) => (
      <a href={`mailto:${email}`} className="employee-email-link">
        {email}
      </a>
    ),
  },
  {
    title: 'Department',
    dataIndex: 'department_name',
    key: 'department_name',
    filters: [
      { text: 'Engineering',        value: 'Engineering'        },
      { text: 'Sales',              value: 'Sales'              },
      { text: 'Human Resources',    value: 'Human Resources'    },
      { text: 'Finance',            value: 'Finance'            },
      { text: 'Operations',         value: 'Operations'         },
      { text: 'Product Management', value: 'Product Management' },
      { text: 'Marketing',          value: 'Marketing'          },
      { text: 'Quality Assurance',  value: 'Quality Assurance'  },
    ],
    onFilter: (value, record) => record.department_name === value,
    render: (dept: string) => (
      <Tag color="geekblue" className="dept-tag">
        {dept}
      </Tag>
    ),
  },
  {
    title: 'Base Salary',
    dataIndex: 'base_salary',
    key: 'base_salary',
    align: 'right',
    sorter: (a, b) => a.base_salary - b.base_salary,
    render: (salary: number) => (
      <span className="salary-amount">{formatINR(salary)}</span>
    ),
  },
  {
    title: 'Status',
    dataIndex: 'employment_status',
    key: 'employment_status',
    width: 130,
    filters: [
      { text: 'Active',     value: 'ACTIVE'     },
      { text: 'On Leave',   value: 'ON_LEAVE'   },
      { text: 'Probation',  value: 'PROBATION'  },
      { text: 'Terminated', value: 'TERMINATED' },
      { text: 'Resigned',   value: 'RESIGNED'   },
    ],
    onFilter: (value, record) => record.employment_status === value,
    render: (status: EmploymentStatus) => {
      const cfg = STATUS_CONFIG[status] ?? {
        color: 'default',
        label: status,
      };
      return (
        <Tag color={cfg.color} className="status-tag">
          {cfg.label}
        </Tag>
      );
    },
  },
];

// ── Component ─────────────────────────────────────────────────────────────────

const EmployeeDirectory: React.FC = () => {
  const { activeCompanyId } = useAuthStore();
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchText, setSearchText] = useState('');
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [pagination, setPagination] = useState<TablePaginationConfig>({
    current: 1,
    pageSize: 10,
    showSizeChanger: true,
    showTotal: (t) => `${t} employees`,
  });

  const fetchEmployees = useCallback(async () => {
    if (!activeCompanyId) return;
    setLoading(true);
    setError(null);

    try {
      const data = await getEmployees(activeCompanyId);
      setEmployees(data);
    } catch {
      setError('Failed to fetch employee roster from the server.');
    } finally {
      setLoading(false);
    }
  }, [activeCompanyId]);

  useEffect(() => {
    void fetchEmployees();
  }, [fetchEmployees]);

  // ── Client-side search filter ──────────────────────────────────────────────

  const filteredData = searchText
    ? employees.filter((e) => {
        const q = searchText.toLowerCase();
        return (
          e.first_name.toLowerCase().includes(q) ||
          e.last_name.toLowerCase().includes(q) ||
          e.email.toLowerCase().includes(q) ||
          e.employee_code.toLowerCase().includes(q) ||
          e.department_name.toLowerCase().includes(q)
        );
      })
    : employees;

  const activeCount = employees.filter(
    (e) => e.employment_status === 'ACTIVE',
  ).length;

  return (
    <div className="directory-page">
      {/* Page Header */}
      <div className="directory-header">
        <div className="directory-title-group">
          <div className="directory-icon">
            <TeamOutlined />
          </div>
          <div>
            <Title level={4} className="directory-title">
              HR Directory
            </Title>
            <Text className="directory-subtitle">
              Workforce Roster ·{' '}
              <Badge
                color="green"
                text={
                  <span className="active-count">{activeCount} active</span>
                }
              />
            </Text>
          </div>
        </div>

        <Space size={12}>
          <Input
            prefix={<SearchOutlined className="search-icon" />}
            placeholder="Search by name, email, code, dept…"
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            allowClear
            className="directory-search"
            id="directory-search"
          />
          <Tooltip title="Refresh">
            <Button
              icon={<ReloadOutlined />}
              onClick={() => void fetchEmployees()}
              loading={loading}
              className="refresh-btn"
              id="directory-refresh"
            />
          </Tooltip>
          <Button
            type="primary"
            icon={<UserAddOutlined />}
            onClick={() => setIsAddModalOpen(true)}
            className="add-employee-btn"
            id="directory-add-employee"
          >
            Add Employee
          </Button>
        </Space>
      </div>

      {/* Error state */}
      {error && (
        <Alert
          message="Server Error"
          description={error}
          type="error"
          showIcon
          closable
          className="directory-error"
          action={
            <Button size="small" onClick={() => void fetchEmployees()}>
              Retry
            </Button>
          }
        />
      )}

      {/* No company selected */}
      {!activeCompanyId && !loading && (
        <Alert
          message="No company selected"
          description="Please select a company from the header to view employees."
          type="info"
          showIcon
          className="directory-error"
        />
      )}

      {/* Data Table */}
      <div className="directory-table-wrapper">
        <Table<Employee>
          rowKey="id"
          dataSource={filteredData}
          columns={buildColumns()}
          loading={loading}
          pagination={pagination}
          onChange={(pag) => setPagination(pag)}
          className="directory-table"
          rowClassName={(record) =>
            record.employment_status === 'TERMINATED' ||
            record.employment_status === 'RESIGNED'
              ? 'row-inactive'
              : ''
          }
          scroll={{ x: 900 }}
          locale={{
            emptyText: searchText
              ? 'No employees match your search.'
              : 'No employees found in this company.',
          }}
        />
      </div>

      {/* Onboard Employee Modal */}
      <AddEmployeeModal
        open={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        onSuccess={() => void fetchEmployees()}
      />
    </div>
  );
};

export default EmployeeDirectory;
