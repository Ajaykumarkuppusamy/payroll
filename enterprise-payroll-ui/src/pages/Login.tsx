import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Card, Form, Input, Typography, Alert } from 'antd';
import { LockOutlined, UserOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { login as authLogin, type LoginRequest } from '../api/auth';
import useAuthStore from '../store/useAuthStore';
import { TOKEN_KEY } from '../api/axios';
import './Login.css';

const { Title, Text } = Typography;

/**
 * Login page for the Enterprise Payroll Platform.
 *
 * Handles form validation, API call, Zustand state hydration,
 * and JWT persistence to localStorage in a single flow.
 */
const Login: React.FC = () => {
  const [form] = Form.useForm<LoginRequest>();
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const { login: storeLogin } = useAuthStore();
  const navigate = useNavigate();

  const handleSubmit = async (values: LoginRequest) => {
    setLoading(true);
    setErrorMessage(null);

    try {
      const response = await authLogin(values);

      // 1. Persist token to localStorage (Axios interceptor will pick this up)
      localStorage.setItem(TOKEN_KEY, response.jwt_token);

      // 2. Hydrate global Zustand store with user identity & RBAC scope
      storeLogin(response.jwt_token, {
        id: response.user_id,
        email: response.email,
        role: response.role,
        allowedCountryIds: response.allowed_country_ids,
        allowedCompanyIds: response.allowed_company_ids,
      });

      // 3. Navigate to the protected dashboard
      navigate('/', { replace: true });
    } catch (err: unknown) {
      const axiosError = err as { response?: { status?: number; data?: { message?: string } } };
      if (axiosError.response?.status === 401) {
        setErrorMessage('Invalid credentials. Please check your login and password.');
      } else {
        setErrorMessage('Unable to connect to the server. Please try again later.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      {/* Animated background orbs */}
      <div className="login-bg">
        <div className="orb orb-1" />
        <div className="orb orb-2" />
        <div className="orb orb-3" />
      </div>

      <div className="login-container">
        {/* Brand header */}
        <div className="login-brand">
          <div className="brand-icon">
            <SafetyCertificateOutlined />
          </div>
          <Title level={2} className="brand-title">
            PayrollOS
          </Title>
          <Text className="brand-subtitle">Enterprise Payroll Platform</Text>
        </div>

        <Card className="login-card" bordered={false}>
          <div className="card-header">
            <Title level={4} className="card-title">
              Operator Sign In
            </Title>
            <Text className="card-description">
              Enter your credentials to access the payroll dashboard
            </Text>
          </div>

          {errorMessage && (
            <Alert
              message={errorMessage}
              type="error"
              showIcon
              closable
              onClose={() => setErrorMessage(null)}
              className="login-alert"
            />
          )}

          <Form
            form={form}
            name="login"
            onFinish={handleSubmit}
            layout="vertical"
            requiredMark={false}
            className="login-form"
          >
            <Form.Item
              name="login"
              label="Email / Username"
              rules={[
                { required: true, message: 'Please enter your email or username.' },
                { min: 3, message: 'Must be at least 3 characters.' },
              ]}
            >
              <Input
                prefix={<UserOutlined className="input-icon" />}
                placeholder="operator@company.com"
                size="large"
                autoComplete="username"
                id="login-username"
              />
            </Form.Item>

            <Form.Item
              name="password"
              label="Password"
              rules={[
                { required: true, message: 'Please enter your password.' },
                { min: 6, message: 'Password must be at least 6 characters.' },
              ]}
            >
              <Input.Password
                prefix={<LockOutlined className="input-icon" />}
                placeholder="••••••••"
                size="large"
                autoComplete="current-password"
                id="login-password"
              />
            </Form.Item>

            <Form.Item className="submit-item">
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                size="large"
                block
                id="login-submit"
                className="login-button"
              >
                {loading ? 'Authenticating…' : 'Sign In'}
              </Button>
            </Form.Item>
          </Form>
        </Card>

        <Text className="login-footer">
          © {new Date().getFullYear()} PayrollOS · Enterprise Edition
        </Text>
      </div>
    </div>
  );
};

export default Login;
