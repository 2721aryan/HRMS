import { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu, Avatar, Dropdown, Button, Tag, Space, Typography } from 'antd';
import {
  DashboardOutlined,
  TeamOutlined,
  ApartmentOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  LogoutOutlined,
  UserOutlined,
  SafetyCertificateOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons';
import { useAuth } from '../../auth/AuthContext';
import './AppLayout.css';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

export default function AppLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleMenuClick = ({ key }: { key: string }) => {
    navigate(key);
  };

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  const userMenuItems = [
    {
      key: 'profile',
      label: (
        <div>
          <Text strong>{user?.fullName ?? user?.username}</Text>
          <br />
          <Text type="secondary" style={{ fontSize: 12 }}>{user?.username}</Text>
        </div>
      ),
      disabled: true,
    },
    { type: 'divider' as const },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: 'Sign Out',
      danger: true,
      onClick: handleLogout,
    },
  ];

  const menuItems = [
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: 'Dashboard',
    },
    {
      key: '/employees',
      icon: <TeamOutlined />,
      label: 'Employees',
    },
    {
      key: '/organization',
      icon: <ApartmentOutlined />,
      label: 'Organization',
    },
    {
      key: '/attendance',
      icon: <ClockCircleOutlined />,
      label: 'Attendance',
    },
    {
      key: '/leaves',
      icon: <CalendarOutlined />,
      label: 'Leaves',
    },
  ];

  // Resolve current active menu item
  const selectedKeys = [location.pathname.startsWith('/employees') ? '/employees' : location.pathname];

  return (
    <Layout className="app-layout">
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        trigger={null}
        width={240}
        className="app-sider"
      >
        <div className="sider-logo-container">
          <div className="sider-logo-icon">
            <SafetyCertificateOutlined />
          </div>
          {!collapsed && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span className="sider-logo-text">HRMS</span>
              <span className="sider-logo-badge">Pro</span>
            </div>
          )}
        </div>

        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={selectedKeys}
          items={menuItems}
          onClick={handleMenuClick}
          style={{ background: 'transparent', marginTop: 12, borderRight: 0 }}
        />

        {!collapsed && user && (
          <div className="sider-user-footer">
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <Space>
                <Avatar style={{ backgroundColor: '#3b82f6' }} icon={<UserOutlined />}>
                  {user.fullName ? user.fullName[0].toUpperCase() : 'U'}
                </Avatar>
                <div style={{ lineHeight: 1.2 }}>
                  <Text style={{ color: '#fff', fontSize: 13 }} strong>
                    {user.fullName || user.username}
                  </Text>
                  <br />
                  <Tag color="blue" style={{ fontSize: 10, margin: 0, padding: '0 4px' }}>
                    {user.roles?.[0] || 'USER'}
                  </Tag>
                </div>
              </Space>
              <Button
                type="text"
                size="small"
                icon={<LogoutOutlined />}
                onClick={handleLogout}
                style={{ color: '#94a3b8' }}
                title="Logout"
              />
            </div>
          </div>
        )}
      </Sider>

      <Layout style={{ background: '#090d16' }}>
        <Header className="app-header">
          <div className="header-left">
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed(!collapsed)}
              style={{ color: '#94a3b8', fontSize: 16 }}
            />
          </div>

          <div className="header-right">
            <Dropdown menu={{ items: userMenuItems }} trigger={['click']} placement="bottomRight">
              <div className="header-user">
                <Avatar style={{ backgroundColor: '#2563eb' }}>
                  {user?.fullName ? user.fullName[0].toUpperCase() : 'A'}
                </Avatar>
                <div className="header-user-info">
                  <span className="header-user-name">{user?.fullName || user?.username}</span>
                  <span className="header-user-role">{user?.roles?.join(', ')}</span>
                </div>
              </div>
            </Dropdown>
          </div>
        </Header>

        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
