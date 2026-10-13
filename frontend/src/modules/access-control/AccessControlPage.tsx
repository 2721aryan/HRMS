import { useState, useEffect, useCallback } from 'react';
import {
  Card,
  Table,
  Button,
  Typography,
  Space,
  Tag,
  Modal,
  Form,
  Input,
  Select,
  Checkbox,
  message,
  Avatar,
  Badge,
  Popconfirm,
  Tooltip,
} from 'antd';
import {
  SafetyCertificateOutlined,
  UserAddOutlined,
  ReloadOutlined,
  KeyOutlined,
  EditOutlined,
  CrownOutlined,
  TeamOutlined,
  CheckCircleOutlined,
  LockOutlined,
  UnlockOutlined,
  UserOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import {
  fetchUsers,
  createUser,
  updateUserRoles,
  toggleUserStatus,
  resetUserPassword,
  type UserItem,
} from '../../api/user';
import { fetchEmployees, type EmployeeSummary } from '../../api/employee';
import type { Role } from '../../types';

const { Title, Text, Paragraph } = Typography;

const ROLE_OPTIONS = [
  { label: '👑 ADMIN — System Admin (Full configuration & user control)', value: 'ADMIN' },
  { label: '💼 HR — HR Manager (Manage employees, org setup, leaves)', value: 'HR' },
  { label: '👔 MANAGER — Team Manager (Approve team attendance & leaves)', value: 'MANAGER' },
  { label: '👤 EMPLOYEE — Staff (Self-service punch-in & leave requests)', value: 'EMPLOYEE' },
];

export default function AccessControlPage() {
  const [users, setUsers] = useState<UserItem[]>([]);
  const [employees, setEmployees] = useState<EmployeeSummary[]>([]);
  const [loading, setLoading] = useState(false);

  // Modals state
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isRoleModalOpen, setIsRoleModalOpen] = useState(false);
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<UserItem | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [createForm] = Form.useForm();
  const [roleForm] = Form.useForm();
  const [passwordForm] = Form.useForm();

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [uData, empData] = await Promise.all([
        fetchUsers(),
        fetchEmployees({ page: 0, size: 100 }),
      ]);
      setUsers(uData);
      setEmployees(empData.content);
    } catch {
      message.error('Failed to load user access control list');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Handle Create User
  const handleCreateUser = async () => {
    try {
      const values = await createForm.validateFields();
      setSubmitting(true);
      await createUser({
        employeeId: values.employeeId,
        username: values.username.trim(),
        password: values.password,
        roles: values.roles,
      });
      message.success('User credentials and role privileges created successfully');
      setIsCreateModalOpen(false);
      createForm.resetFields();
      loadData();
    } catch (err: any) {
      if (err?.response?.data?.message) {
        message.error(err.response.data.message);
      }
    } finally {
      setSubmitting(false);
    }
  };

  // Handle Edit Roles
  const handleEditRoles = (user: UserItem) => {
    setSelectedUser(user);
    roleForm.setFieldsValue({ roles: user.roles });
    setIsRoleModalOpen(true);
  };

  const handleSaveRoles = async () => {
    if (!selectedUser) return;
    try {
      const values = await roleForm.validateFields();
      setSubmitting(true);
      await updateUserRoles(selectedUser.id, values.roles);
      message.success(`Privileges for ${selectedUser.username} updated. Active sessions refreshed.`);
      setIsRoleModalOpen(false);
      loadData();
    } catch (err: any) {
      if (err?.response?.data?.message) {
        message.error(err.response.data.message);
      }
    } finally {
      setSubmitting(false);
    }
  };

  // Handle Toggle Status
  const handleToggleStatus = async (user: UserItem) => {
    try {
      await toggleUserStatus(user.id, !user.active);
      message.success(`Account for ${user.username} ${user.active ? 'deactivated' : 'activated'}`);
      loadData();
    } catch {
      message.error('Failed to update account status');
    }
  };

  // Handle Reset Password
  const handleOpenResetPassword = (user: UserItem) => {
    setSelectedUser(user);
    passwordForm.resetFields();
    setIsPasswordModalOpen(true);
  };

  const handleResetPassword = async () => {
    if (!selectedUser) return;
    try {
      const values = await passwordForm.validateFields();
      setSubmitting(true);
      await resetUserPassword(selectedUser.id, values.newPassword);
      message.success(`Password reset successfully for ${selectedUser.username}`);
      setIsPasswordModalOpen(false);
    } catch (err: any) {
      if (err?.response?.data?.message) {
        message.error(err.response.data.message);
      }
    } finally {
      setSubmitting(false);
    }
  };

  // Role tag renderer
  const renderRoleTag = (role: Role) => {
    switch (role) {
      case 'ADMIN':
        return <Tag color="red" key={role} icon={<CrownOutlined />}>ADMIN</Tag>;
      case 'HR':
        return <Tag color="purple" key={role}>HR</Tag>;
      case 'MANAGER':
        return <Tag color="blue" key={role}>MANAGER</Tag>;
      case 'EMPLOYEE':
        return <Tag color="green" key={role}>EMPLOYEE</Tag>;
      default:
        return <Tag key={role}>{role}</Tag>;
    }
  };

  // Filter employees who do not yet have a login account
  const unprovisionedEmployees = employees.filter(
    (e) => !users.some((u) => u.employeeId === e.id)
  );

  // Metrics
  const adminCount = users.filter((u) => u.roles.includes('ADMIN')).length;
  const hrCount = users.filter((u) => u.roles.includes('HR')).length;
  const managerCount = users.filter((u) => u.roles.includes('MANAGER')).length;
  const activeCount = users.filter((u) => u.active).length;

  const columns = [
    {
      title: 'Employee',
      key: 'employee',
      render: (_: any, record: UserItem) => (
        <Space>
          <Avatar style={{ backgroundColor: record.roles.includes('ADMIN') ? '#ef4444' : '#3b82f6' }} icon={<UserOutlined />}>
            {record.employeeName ? record.employeeName[0].toUpperCase() : 'U'}
          </Avatar>
          <div>
            <Text strong style={{ color: '#f8fafc', display: 'block' }}>{record.employeeName}</Text>
            <Tag color="default" style={{ fontSize: 11, margin: 0, padding: '0 4px' }}>{record.employeeCode}</Tag>
          </div>
        </Space>
      ),
    },
    {
      title: 'Login Username',
      dataIndex: 'username',
      key: 'username',
      render: (username: string) => (
        <Text copyable style={{ color: '#60a5fa', fontFamily: 'monospace', fontWeight: 600 }}>
          {username}
        </Text>
      ),
    },
    {
      title: 'Assigned Roles & Privileges',
      dataIndex: 'roles',
      key: 'roles',
      render: (roles: Role[]) => (
        <Space size={[0, 4]} wrap>
          {roles.map(renderRoleTag)}
        </Space>
      ),
    },
    {
      title: 'Account Status',
      dataIndex: 'active',
      key: 'active',
      render: (active: boolean) => (
        <Badge
          status={active ? 'success' : 'error'}
          text={<span style={{ color: active ? '#10b981' : '#ef4444' }}>{active ? 'Active' : 'Disabled'}</span>}
        />
      ),
    },
    {
      title: 'Last Login',
      dataIndex: 'lastLoginAt',
      key: 'lastLoginAt',
      render: (last: string | null) => (
        <Text type="secondary" style={{ fontSize: 12 }}>
          {last ? dayjs(last).format('DD MMM YYYY, HH:mm') : 'Never'}
        </Text>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: UserItem) => (
        <Space size="middle">
          <Tooltip title="Modify assigned roles & privileges">
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleEditRoles(record)}
            >
              Edit Roles
            </Button>
          </Tooltip>

          <Tooltip title="Reset account password">
            <Button
              type="text"
              size="small"
              icon={<KeyOutlined />}
              onClick={() => handleOpenResetPassword(record)}
            >
              Reset Key
            </Button>
          </Tooltip>

          {record.username !== 'admin' && (
            <Popconfirm
              title={record.active ? 'Disable this login account?' : 'Activate this login account?'}
              description={record.active ? 'The user will immediately be barred from logging in.' : 'The user will be allowed to log in.'}
              onConfirm={() => handleToggleStatus(record)}
              okText="Yes"
              cancelText="No"
            >
              <Button
                type="text"
                size="small"
                danger={record.active}
                icon={record.active ? <LockOutlined /> : <UnlockOutlined />}
              >
                {record.active ? 'Disable' : 'Enable'}
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: '24px' }}>
      {/* Page Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
        <div>
          <Title level={2} style={{ color: '#f8fafc', margin: 0, display: 'flex', alignItems: 'center', gap: 10 }}>
            <SafetyCertificateOutlined style={{ color: '#8b5cf6' }} />
            Access Control & Role Privileges
          </Title>
          <Paragraph type="secondary" style={{ margin: '6px 0 0', fontSize: 14 }}>
            Manage workforce login accounts, provision system credentials, and configure role privileges across Phase 1 modules.
          </Paragraph>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={loadData} loading={loading}>
            Refresh
          </Button>
          <Button
            type="primary"
            icon={<UserAddOutlined />}
            onClick={() => setIsCreateModalOpen(true)}
            style={{ background: 'linear-gradient(135deg, #3b82f6, #8b5cf6)', border: 0 }}
          >
            Provision User Login
          </Button>
        </Space>
      </div>

      {/* Overview Stat Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 16, marginBottom: 24 }}>
        <Card variant="borderless" style={{ background: '#1e293b', borderRadius: 12 }}>
          <Text type="secondary" style={{ fontSize: 12 }}>Total Accounts</Text>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 4 }}>
            <Title level={3} style={{ color: '#f8fafc', margin: 0 }}>{users.length}</Title>
            <TeamOutlined style={{ fontSize: 24, color: '#3b82f6' }} />
          </div>
        </Card>

        <Card variant="borderless" style={{ background: '#1e293b', borderRadius: 12 }}>
          <Text type="secondary" style={{ fontSize: 12 }}>System Administrators</Text>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 4 }}>
            <Title level={3} style={{ color: '#ef4444', margin: 0 }}>{adminCount}</Title>
            <CrownOutlined style={{ fontSize: 24, color: '#ef4444' }} />
          </div>
        </Card>

        <Card variant="borderless" style={{ background: '#1e293b', borderRadius: 12 }}>
          <Text type="secondary" style={{ fontSize: 12 }}>Managers & HR Leads</Text>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 4 }}>
            <Title level={3} style={{ color: '#a855f7', margin: 0 }}>{hrCount + managerCount}</Title>
            <SafetyCertificateOutlined style={{ fontSize: 24, color: '#a855f7' }} />
          </div>
        </Card>

        <Card variant="borderless" style={{ background: '#1e293b', borderRadius: 12 }}>
          <Text type="secondary" style={{ fontSize: 12 }}>Active Logins</Text>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 4 }}>
            <Title level={3} style={{ color: '#10b981', margin: 0 }}>{activeCount}</Title>
            <CheckCircleOutlined style={{ fontSize: 24, color: '#10b981' }} />
          </div>
        </Card>
      </div>

      {/* Main Table Card */}
      <Card
        variant="borderless"
        style={{ background: '#1e293b', borderRadius: 12 }}
        title={<span style={{ color: '#f8fafc', fontWeight: 600 }}>Configured User Logins ({users.length})</span>}
      >
        <Table
          dataSource={users}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      {/* ─── Modal 1: Provision User Login ─────────────────────────── */}
      <Modal
        title={<span><UserAddOutlined style={{ color: '#3b82f6', marginRight: 8 }} />Provision Employee Login</span>}
        open={isCreateModalOpen}
        onOk={handleCreateUser}
        confirmLoading={submitting}
        onCancel={() => setIsCreateModalOpen(false)}
        okText="Create User"
        width={540}
      >
        <Form form={createForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="employeeId"
            label="Select Employee"
            rules={[{ required: true, message: 'Please select an employee' }]}
          >
            <Select
              placeholder="Search employee by name or code..."
              showSearch
              filterOption={(input, option) =>
                (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
              }
              options={unprovisionedEmployees.map((e) => ({
                value: e.id,
                label: `${e.fullName} (${e.employeeCode}) — ${e.departmentName ?? 'No Dept'}`,
              }))}
              onChange={(empId) => {
                const emp = employees.find((e) => e.id === empId);
                if (emp) {
                  createForm.setFieldsValue({
                    username: emp.workEmail ? emp.workEmail.split('@')[0] : emp.firstName.toLowerCase(),
                  });
                }
              }}
            />
          </Form.Item>

          <Form.Item
            name="username"
            label="Login Username"
            rules={[{ required: true, message: 'Please enter a username' }]}
          >
            <Input placeholder="e.g. john.doe" />
          </Form.Item>

          <Form.Item
            name="password"
            label="Initial Password"
            rules={[
              { required: true, message: 'Please set an initial password' },
              { min: 6, message: 'Password must be at least 6 characters' },
            ]}
          >
            <Input.Password placeholder="Enter a secure password..." />
          </Form.Item>

          <Form.Item
            name="roles"
            label="Assigned Privileges & Roles"
            rules={[{ required: true, message: 'Select at least one role' }]}
            initialValue={['EMPLOYEE']}
          >
            <Checkbox.Group
              options={ROLE_OPTIONS}
              style={{ display: 'flex', flexDirection: 'column', gap: 10 }}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* ─── Modal 2: Edit User Roles ───────────────────────────────── */}
      <Modal
        title={<span><SafetyCertificateOutlined style={{ color: '#8b5cf6', marginRight: 8 }} />Edit Roles & Privileges</span>}
        open={isRoleModalOpen}
        onOk={handleSaveRoles}
        confirmLoading={submitting}
        onCancel={() => setIsRoleModalOpen(false)}
        okText="Save Privileges"
        width={540}
      >
        {selectedUser && (
          <div style={{ marginBottom: 16 }}>
            <Text type="secondary">Modifying permissions for:</Text>
            <div style={{ fontSize: 16, fontWeight: 600, color: '#f8fafc', marginTop: 4 }}>
              {selectedUser.employeeName} ({selectedUser.username})
            </div>
          </div>
        )}
        <Form form={roleForm} layout="vertical">
          <Form.Item
            name="roles"
            label="Assigned Roles"
            rules={[{ required: true, message: 'At least one role must be selected' }]}
          >
            <Checkbox.Group
              options={ROLE_OPTIONS}
              style={{ display: 'flex', flexDirection: 'column', gap: 10 }}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* ─── Modal 3: Reset Password ───────────────────────────────── */}
      <Modal
        title={<span><KeyOutlined style={{ color: '#f59e0b', marginRight: 8 }} />Reset Account Password</span>}
        open={isPasswordModalOpen}
        onOk={handleResetPassword}
        confirmLoading={submitting}
        onCancel={() => setIsPasswordModalOpen(false)}
        okText="Update Password"
      >
        {selectedUser && (
          <div style={{ marginBottom: 16 }}>
            <Text type="secondary">Resetting password for:</Text>
            <div style={{ fontSize: 15, fontWeight: 600, color: '#f8fafc', marginTop: 4 }}>
              {selectedUser.employeeName} (@{selectedUser.username})
            </div>
          </div>
        )}
        <Form form={passwordForm} layout="vertical">
          <Form.Item
            name="newPassword"
            label="New Password"
            rules={[
              { required: true, message: 'Please enter a new password' },
              { min: 6, message: 'Password must be at least 6 characters' },
            ]}
          >
            <Input.Password placeholder="Enter new password..." />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
