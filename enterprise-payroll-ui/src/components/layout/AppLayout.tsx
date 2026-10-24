import React, { useState, useEffect } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  Layout,
  Menu,
  Select,
  Button,
  Avatar,
  Typography,
  Tooltip,
  Divider,
  Tag,
} from 'antd';
import {
  DashboardOutlined,
  TeamOutlined,
  CalculatorOutlined,
  PlayCircleOutlined,
  FileTextOutlined,
  LogoutOutlined,
  SafetyCertificateOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SwapOutlined,
} from '@ant-design/icons';
import useAuthStore from '../../store/useAuthStore';
import { TOKEN_KEY } from '../../api/axios';
import { getMyCompanies, type Company } from '../../api/company';
import './AppLayout.css';

const { Sider, Header, Content } = Layout;
const { Text } = Typography;

// ── Nav Item Config ───────────────────────────────────────────────────────────

const NAV_ITEMS = [
  {
    key: '/dashboard',
    icon: <DashboardOutlined />,
    label: 'Dashboard',
  },
  {
    key: '/employees',
    icon: <TeamOutlined />,
    label: 'HR Directory',
  },
  {
    key: '/formula-studio',
    icon: <CalculatorOutlined />,
    label: 'Formula Studio',
  },
  {
    key: '/payroll-console',
    icon: <PlayCircleOutlined />,
    label: 'Payroll Console',
  },
  {
    key: '/reports',
    icon: <FileTextOutlined />,
    label: 'Reports',
  },
];

// ── Role Badge ────────────────────────────────────────────────────────────────

const ROLE_COLOR: Record<string, string> = {
  SUPER_ADMIN: 'geekblue',
  COMPANY_HR: 'purple',
  PAYROLL_OPERATOR: 'cyan',
};

// ── Component ─────────────────────────────────────────────────────────────────

const AppLayout: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false);
  const [companies, setCompanies] = useState<Company[]>([]);
  const { user, activeCompanyId, setActiveCompany, logout } = useAuthStore();
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    getMyCompanies()
      .then((data) => {
        setCompanies(data);
      })
      .catch(() => {
        // quiet fallback
      });
  }, []);

  const handleLogout = () => {
    localStorage.removeItem(TOKEN_KEY);
    logout();
    navigate('/login', { replace: true });
  };

  const handleNavSelect = ({ key }: { key: string }) => {
    navigate(key);
  };

  const roleColor = ROLE_COLOR[user?.role ?? ''] ?? 'default';
  const initials = user?.email
    ? user.email.slice(0, 2).toUpperCase()
    : '??';

  const companyOptions =
    companies.length > 0
      ? companies.map((c) => ({
          value: c.id,
          label: `${c.name} (${c.currency_code})`,
        }))
      : (user?.allowedCompanyIds ?? []).map((id) => ({
          value: id,
          label: `Company #${id}`,
        }));

  return (
    <Layout className="app-layout">
      {/* ── Sidebar ────────────────────────────────────────────────────── */}
      <Sider
        collapsible
        collapsed={collapsed}
        trigger={null}
        width={240}
        collapsedWidth={72}
        className="app-sider"
      >
        {/* Brand */}
        <div className={`sider-brand ${collapsed ? 'sider-brand--collapsed' : ''}`}>
          <div className="brand-logo">
            <SafetyCertificateOutlined />
          </div>
          {!collapsed && (
            <div className="brand-text">
              <span className="brand-name">PayrollOS</span>
              <span className="brand-tagline">Enterprise Platform</span>
            </div>
          )}
        </div>

        <Divider className="sider-divider" />

        {/* Nav Items */}
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={NAV_ITEMS}
          onClick={handleNavSelect}
          className="app-menu"
          inlineCollapsed={collapsed}
          style={{ background: 'transparent' }}
        />

        {/* Collapse Toggle */}
        <div className="sider-collapse-btn" onClick={() => setCollapsed(!collapsed)}>
          {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          {!collapsed && <span>Collapse</span>}
        </div>
      </Sider>

      <Layout>
        {/* ── Header ─────────────────────────────────────────────────── */}
        <Header className="app-header">
          {/* Page title (derived from active route) */}
          <div className="header-left">
            <Text className="header-page-title">
              {NAV_ITEMS.find((i) => i.key === location.pathname)?.label ?? 'PayrollOS'}
            </Text>
          </div>

          <div className="header-right">
            {/* Company Switcher */}
            {(companyOptions.length > 1 || (user && user.allowedCompanyIds.length > 1)) && (
              <div className="company-switcher">
                <SwapOutlined className="switcher-icon" />
                <Select
                  value={activeCompanyId}
                  onChange={setActiveCompany}
                  size="small"
                  className="company-select"
                  popupMatchSelectWidth={false}
                  options={companyOptions}
                />
              </div>
            )}

            {/* User badge */}
            <div className="header-user">
              <Avatar className="user-avatar" size={32}>
                {initials}
              </Avatar>
              <div className="user-info">
                <Text className="user-email" ellipsis>
                  {user?.email}
                </Text>
                <Tag color={roleColor} className="user-role-tag">
                  {user?.role?.replace(/_/g, ' ')}
                </Tag>
              </div>
            </div>

            <Divider type="vertical" className="header-divider" />

            {/* Logout */}
            <Tooltip title="Sign out" placement="bottomRight">
              <Button
                type="text"
                icon={<LogoutOutlined />}
                onClick={handleLogout}
                className="logout-btn"
                id="header-logout"
                danger
              />
            </Tooltip>
          </div>
        </Header>

        {/* ── Content ────────────────────────────────────────────────── */}
        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
};

export default AppLayout;
