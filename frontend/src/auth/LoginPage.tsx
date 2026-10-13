import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Form, Input, Button, Alert, Typography } from 'antd';
import { UserOutlined, LockOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { useAuth } from './AuthContext';
import './LoginPage.css';

const { Title, Text } = Typography;

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as any)?.from?.pathname ?? '/dashboard';
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const onFinish = async (values: { username: string; password: string }) => {
    setError(null);
    setLoading(true);
    try {
      const result = await login(values);
      if (result.mustChangePassword) {
        navigate('/change-password', { replace: true });
      } else {
        navigate(from, { replace: true });
      }
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Invalid username or password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-root">
      <div className="login-backdrop" />
      <div className="login-card">
        <div className="login-brand">
          <div className="login-logo">
            <SafetyCertificateOutlined />
          </div>
          <Title level={3} className="login-title">HRMS</Title>
          <Text className="login-subtitle">Human Resource Management System</Text>
        </div>

        {error && (
          <Alert message={error} type="error" showIcon closable
            onClose={() => setError(null)} className="login-alert" />
        )}

        <Form layout="vertical" onFinish={onFinish} size="large" requiredMark={false}>
          <Form.Item name="username" rules={[{ required: true, message: 'Enter your username' }]}>
            <Input prefix={<UserOutlined className="input-icon" />}
              placeholder="Username / Work Email" autoComplete="username" />
          </Form.Item>

          <Form.Item name="password" rules={[{ required: true, message: 'Enter your password' }]}>
            <Input.Password prefix={<LockOutlined className="input-icon" />}
              placeholder="Password" autoComplete="current-password" />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" loading={loading} block className="login-btn">
              Sign In
            </Button>
          </Form.Item>
        </Form>

        <Text className="login-footer">
          Forgot your password? Contact your HR administrator.
        </Text>
      </div>
    </div>
  );
}
