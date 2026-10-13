import { useState, useEffect, useCallback } from 'react';
import {
  Table,
  Button,
  Input,
  Select,
  Tag,
  Space,
  Card,
  Typography,
  Modal,
  Form,
  DatePicker,
  Row,
  Col,
  Drawer,
  Descriptions,
  message,
  Avatar,
  Segmented,
  Tooltip,
  Dropdown,
} from 'antd';
import {
  UserAddOutlined,
  SearchOutlined,
  ReloadOutlined,
  EyeOutlined,
  MoreOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  StopOutlined,
  TeamOutlined,
  IdcardOutlined,
  FileExcelOutlined,
} from '@ant-design/icons';
import { apiClient } from '../../api/client';
import dayjs from 'dayjs';
import {
  fetchEmployees,
  fetchEmployeeById,
  createEmployee,
  updateEmployeeStatus,
  fetchEmployeeStats,
  type EmployeeSummary,
  type EmployeeDetail,
  type CreateEmployeePayload,
  type EmployeeStats,
} from '../../api/employee';
import {
  fetchDepartments,
  fetchDesignations,
  fetchLocations,
  type Department,
  type Designation,
  type Location,
} from '../../api/org';
import { useAuth } from '../../auth/AuthContext';
import './EmployeeListPage.css';

const { Title, Text } = Typography;

export default function EmployeeListPage() {
  const { user } = useAuth();
  const canManageEmployees = user?.roles?.some((r: string) => r === 'ADMIN' || r === 'HR');
  const canViewStats = user?.roles?.some((r: string) => r === 'ADMIN' || r === 'HR' || r === 'MANAGER');
  const [employees, setEmployees] = useState<EmployeeSummary[]>([]);
  const [stats, setStats] = useState<EmployeeStats | null>(null);
  const [loading, setLoading] = useState(false);
  const [totalElements, setTotalElements] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Filters
  const [search, setSearch] = useState('');
  const [selectedDept, setSelectedDept] = useState<number | undefined>(undefined);
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');

  // Metadata for dropdowns
  const [departments, setDepartments] = useState<Department[]>([]);
  const [designations, setDesignations] = useState<Designation[]>([]);
  const [locations, setLocations] = useState<Location[]>([]);

  // Modals & Drawers
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [selectedEmpDetail, setSelectedEmpDetail] = useState<EmployeeDetail | null>(null);
  const [isDetailDrawerOpen, setIsDetailDrawerOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);

  const [form] = Form.useForm();

  // Load dropdown options
  useEffect(() => {
    fetchDepartments(true).then(setDepartments).catch(() => {});
    fetchDesignations(true).then(setDesignations).catch(() => {});
    fetchLocations().then(setLocations).catch(() => {});
  }, []);

  // Fetch employees list
  const loadEmployees = useCallback(async () => {
    setLoading(true);
    try {
      const res = await fetchEmployees({
        search: search.trim() || undefined,
        departmentId: selectedDept,
        status: selectedStatus === 'ALL' ? undefined : selectedStatus,
        page: currentPage - 1,
        size: pageSize,
      });
      setEmployees(res.content);
      setTotalElements(res.totalElements);
    } catch {
      message.error('Failed to load employees list');
    } finally {
      setLoading(false);
    }
  }, [search, selectedDept, selectedStatus, currentPage, pageSize]);

  // Fetch stats
  const loadStats = useCallback(async () => {
    if (!canViewStats) return;
    try {
      const s = await fetchEmployeeStats();
      setStats(s);
    } catch {
      // stats failure is not blocking
    }
  }, [canViewStats]);

  useEffect(() => {
    loadEmployees();
  }, [loadEmployees]);

  useEffect(() => {
    loadStats();
  }, [loadStats]);

  const handleOpenDetail = async (id: number) => {
    setIsDetailDrawerOpen(true);
    setDetailLoading(true);
    try {
      const detail = await fetchEmployeeById(id);
      setSelectedEmpDetail(detail);
    } catch {
      message.error('Failed to load employee details');
      setIsDetailDrawerOpen(false);
    } finally {
      setDetailLoading(false);
    }
  };

  const handleStatusChange = async (id: number, newStatus: string) => {
    try {
      await updateEmployeeStatus(id, newStatus);
      message.success(`Status updated to ${newStatus}`);
      loadEmployees();
      loadStats();
    } catch {
      message.error('Failed to update employee status');
    }
  };

  const [exporting, setExporting] = useState(false);

  const handleExportExcel = async () => {
    try {
      setExporting(true);
      const res = await apiClient.get('/employees/export', { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'employees.xlsx');
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
      message.success('Employee directory exported successfully');
    } catch {
      message.error('Failed to export employee directory');
    } finally {
      setExporting(false);
    }
  };

  const handleCreateSubmit = async () => {
    try {
      const values = await form.validateFields();
      setIsSubmitting(true);

      const payload: CreateEmployeePayload = {
        employeeCode: values.employeeCode || undefined,
        firstName: values.firstName,
        lastName: values.lastName,
        workEmail: values.workEmail,
        personalEmail: values.personalEmail,
        phone: values.phone,
        dateOfBirth: values.dateOfBirth ? values.dateOfBirth.format('YYYY-MM-DD') : undefined,
        gender: values.gender,
        maritalStatus: values.maritalStatus,
        addressLine: values.addressLine,
        city: values.city,
        state: values.state,
        postalCode: values.postalCode,
        emergencyContactName: values.emergencyContactName,
        emergencyContactPhone: values.emergencyContactPhone,
        departmentId: values.departmentId,
        designationId: values.designationId,
        locationId: values.locationId,
        employmentType: values.employmentType || 'FULL_TIME',
        dateOfJoining: values.dateOfJoining.format('YYYY-MM-DD'),
        probationEndDate: values.probationEndDate ? values.probationEndDate.format('YYYY-MM-DD') : undefined,
      };

      await createEmployee(payload);
      message.success('Employee created successfully!');
      setIsAddModalOpen(false);
      form.resetFields();
      loadEmployees();
      loadStats();
    } catch {
      message.error('Please check form fields and try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const getStatusTag = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return <Tag color="success">Active</Tag>;
      case 'ON_NOTICE':
        return <Tag color="warning">On Notice</Tag>;
      case 'INACTIVE':
        return <Tag color="error">Inactive</Tag>;
      default:
        return <Tag>{status}</Tag>;
    }
  };

  const getAvatarColor = (name: string) => {
    const colors = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ec4899', '#06b6d4'];
    let hash = 0;
    for (let i = 0; i < name.length; i++) hash += name.charCodeAt(i);
    return colors[Math.abs(hash) % colors.length];
  };

  const columns = [
    {
      title: 'Employee',
      key: 'employee',
      render: (_: any, record: EmployeeSummary) => (
        <div className="employee-avatar-group">
          <Avatar style={{ backgroundColor: getAvatarColor(record.fullName) }}>
            {record.firstName[0]?.toUpperCase() || 'E'}
          </Avatar>
          <div>
            <div className="emp-name">{record.fullName}</div>
            <div className="emp-code">{record.employeeCode}</div>
          </div>
        </div>
      ),
    },
    {
      title: 'Department',
      dataIndex: 'departmentName',
      key: 'departmentName',
      render: (val: string | null) => val ? <Tag color="blue">{val}</Tag> : <Text type="secondary">—</Text>,
    },
    {
      title: 'Designation',
      dataIndex: 'designationTitle',
      key: 'designationTitle',
      render: (val: string | null) => val || <Text type="secondary">—</Text>,
    },
    {
      title: 'Contact',
      key: 'contact',
      render: (_: any, record: EmployeeSummary) => (
        <div>
          <div style={{ fontSize: 13, color: '#93c5fd' }}>{record.workEmail}</div>
          {record.phone && <div style={{ fontSize: 12, color: '#64748b' }}>{record.phone}</div>}
        </div>
      ),
    },
    {
      title: 'Type',
      dataIndex: 'employmentType',
      key: 'employmentType',
      render: (val: string) => (
        <Tag style={{ fontSize: 11 }}>{val.replace('_', ' ')}</Tag>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (val: string) => getStatusTag(val),
    },
    {
      title: 'Joined',
      dataIndex: 'dateOfJoining',
      key: 'dateOfJoining',
      render: (val: string) => dayjs(val).format('DD MMM YYYY'),
    },
    {
      title: 'Actions',
      key: 'actions',
      align: 'right' as const,
      render: (_: any, record: EmployeeSummary) => {
        const statusMenuItems = [
          {
            key: 'ACTIVE',
            label: 'Set as Active',
            icon: <CheckCircleOutlined style={{ color: '#10b981' }} />,
            disabled: record.status === 'ACTIVE',
            onClick: () => handleStatusChange(record.id, 'ACTIVE'),
          },
          {
            key: 'ON_NOTICE',
            label: 'Set as On Notice',
            icon: <ExclamationCircleOutlined style={{ color: '#f59e0b' }} />,
            disabled: record.status === 'ON_NOTICE',
            onClick: () => handleStatusChange(record.id, 'ON_NOTICE'),
          },
          {
            key: 'INACTIVE',
            label: 'Deactivate Employee',
            icon: <StopOutlined style={{ color: '#ef4444' }} />,
            danger: true,
            disabled: record.status === 'INACTIVE',
            onClick: () => handleStatusChange(record.id, 'INACTIVE'),
          },
        ];

        return (
          <Space size={8}>
            <Tooltip title="View Profile">
              <Button
                type="text"
                icon={<EyeOutlined />}
                onClick={() => handleOpenDetail(record.id)}
                style={{ color: '#60a5fa' }}
              />
            </Tooltip>
            <Dropdown menu={{ items: statusMenuItems }} trigger={['click']}>
              <Button type="text" icon={<MoreOutlined />} style={{ color: '#94a3b8' }} />
            </Dropdown>
          </Space>
        );
      },
    },
  ];

  return (
    <div className="employee-page">
      {/* Page Header */}
      <div className="page-header">
        <div>
          <Title level={2} className="page-title">Employee Directory</Title>
          <div className="page-subtitle">Manage workforce records, employment status, and profile information</div>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => { loadEmployees(); loadStats(); }}>
            Refresh
          </Button>
          <Button
            icon={<FileExcelOutlined />}
            loading={exporting}
            onClick={handleExportExcel}
            style={{ color: '#10b981', borderColor: 'rgba(16,185,129,0.3)' }}
          >
            Export Excel
          </Button>
          {canManageEmployees && (
            <Button
              type="primary"
              icon={<UserAddOutlined />}
              onClick={() => setIsAddModalOpen(true)}
              style={{ background: 'linear-gradient(135deg, #2563eb, #7c3aed)', border: 0 }}
            >
              Add Employee
            </Button>
          )}
        </Space>
      </div>

      {/* Metric Cards */}
      {canViewStats && (
        <div className="stat-grid">
        <Card className="stat-card" variant="borderless">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <Text type="secondary" style={{ fontSize: 13 }}>Total Headcount</Text>
              <Title level={3} style={{ color: '#f8fafc', margin: '4px 0 0' }}>{stats?.total ?? '—'}</Title>
            </div>
            <div style={{ fontSize: 24, color: '#3b82f6', background: 'rgba(59,130,246,0.1)', padding: 12, borderRadius: 10 }}>
              <TeamOutlined />
            </div>
          </div>
        </Card>

        <Card className="stat-card" variant="borderless">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <Text type="secondary" style={{ fontSize: 13 }}>Active Staff</Text>
              <Title level={3} style={{ color: '#10b981', margin: '4px 0 0' }}>{stats?.active ?? '—'}</Title>
            </div>
            <div style={{ fontSize: 24, color: '#10b981', background: 'rgba(16,185,129,0.1)', padding: 12, borderRadius: 10 }}>
              <CheckCircleOutlined />
            </div>
          </div>
        </Card>

        <Card className="stat-card" variant="borderless">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <Text type="secondary" style={{ fontSize: 13 }}>On Notice</Text>
              <Title level={3} style={{ color: '#f59e0b', margin: '4px 0 0' }}>{stats?.onNotice ?? '—'}</Title>
            </div>
            <div style={{ fontSize: 24, color: '#f59e0b', background: 'rgba(245,158,11,0.1)', padding: 12, borderRadius: 10 }}>
              <ExclamationCircleOutlined />
            </div>
          </div>
        </Card>

        <Card className="stat-card" variant="borderless">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <Text type="secondary" style={{ fontSize: 13 }}>Deactivated</Text>
              <Title level={3} style={{ color: '#ef4444', margin: '4px 0 0' }}>{stats?.inactive ?? '—'}</Title>
            </div>
            <div style={{ fontSize: 24, color: '#ef4444', background: 'rgba(239,68,68,0.1)', padding: 12, borderRadius: 10 }}>
              <StopOutlined />
            </div>
          </div>
        </Card>
      </div>
      )}

      {/* Filter and Search Bar */}
      <div className="filter-bar">
        <Space wrap size={12}>
          <Input
            placeholder="Search by name, email or code..."
            prefix={<SearchOutlined style={{ color: '#64748b' }} />}
            value={search}
            onChange={(e) => { setSearch(e.target.value); setCurrentPage(1); }}
            style={{ width: 280 }}
            allowClear
          />

          <Select
            placeholder="Filter Department"
            allowClear
            value={selectedDept}
            onChange={(val) => { setSelectedDept(val); setCurrentPage(1); }}
            style={{ width: 200 }}
            options={departments.map((d) => ({ label: d.name, value: d.id }))}
          />
        </Space>

        <Segmented
          options={[
            { label: 'All', value: 'ALL' },
            { label: 'Active', value: 'ACTIVE' },
            { label: 'On Notice', value: 'ON_NOTICE' },
            { label: 'Inactive', value: 'INACTIVE' },
          ]}
          value={selectedStatus}
          onChange={(val) => { setSelectedStatus(val as string); setCurrentPage(1); }}
        />
      </div>

      {/* Main Table */}
      <Card className="table-card" variant="borderless">
        <Table
          columns={columns}
          dataSource={employees}
          rowKey="id"
          loading={loading}
          pagination={{
            current: currentPage,
            pageSize: pageSize,
            total: totalElements,
            showSizeChanger: true,
            onChange: (p, s) => { setCurrentPage(p); setPageSize(s); },
          }}
        />
      </Card>

      {/* Add Employee Modal */}
      <Modal
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, color: '#f8fafc' }}>
            <IdcardOutlined style={{ color: '#3b82f6' }} />
            <span>Add New Employee</span>
          </div>
        }
        open={isAddModalOpen}
        onCancel={() => setIsAddModalOpen(false)}
        onOk={handleCreateSubmit}
        confirmLoading={isSubmitting}
        okText="Create Employee"
        width={720}
        destroyOnClose
      >
        <Form form={form} layout="vertical" requiredMark={false} initialValues={{ employmentType: 'FULL_TIME', dateOfJoining: dayjs() }}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="firstName" label="First Name" rules={[{ required: true, message: 'First name is required' }]}>
                <Input placeholder="e.g. Rahul" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="lastName" label="Last Name">
                <Input placeholder="e.g. Nair" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="workEmail" label="Work Email" rules={[{ required: true, type: 'email', message: 'Valid work email required' }]}>
                <Input placeholder="rahul.nair@company.com" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="phone" label="Phone Number">
                <Input placeholder="+91 9876543210" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="departmentId" label="Department" rules={[{ required: true, message: 'Please select a department' }]}>
                <Select
                  placeholder="Select Department"
                  options={departments.map((d) => ({ label: d.name, value: d.id }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="designationId" label="Designation" rules={[{ required: true, message: 'Please select a designation' }]}>
                <Select
                  placeholder="Select Designation"
                  options={designations.map((d) => ({ label: d.title, value: d.id }))}
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="locationId" label="Office Location">
                <Select
                  placeholder="Select Location"
                  options={locations.map((l) => ({ label: l.name, value: l.id }))}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="employmentType" label="Employment Type">
                <Select
                  options={[
                    { label: 'Full Time', value: 'FULL_TIME' },
                    { label: 'Part Time', value: 'PART_TIME' },
                    { label: 'Contract', value: 'CONTRACT' },
                    { label: 'Intern', value: 'INTERN' },
                  ]}
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="dateOfJoining" label="Date of Joining" rules={[{ required: true, message: 'Joining date is required' }]}>
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="probationEndDate" label="Probation End Date">
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="emergencyContactName" label="Emergency Contact Name">
                <Input placeholder="e.g. Priya Nair (Spouse)" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="emergencyContactPhone" label="Emergency Contact Phone">
                <Input placeholder="+91 9876500000" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      {/* Employee Profile Detail Drawer */}
      <Drawer
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <Avatar size="large" style={{ backgroundColor: selectedEmpDetail ? getAvatarColor(selectedEmpDetail.fullName) : '#3b82f6' }}>
              {selectedEmpDetail?.firstName?.[0]?.toUpperCase()}
            </Avatar>
            <div>
              <div style={{ color: '#f8fafc', fontWeight: 600, fontSize: 16 }}>{selectedEmpDetail?.fullName}</div>
              <div style={{ color: '#94a3b8', fontSize: 12 }}>{selectedEmpDetail?.employeeCode}</div>
            </div>
          </div>
        }
        open={isDetailDrawerOpen}
        onClose={() => setIsDetailDrawerOpen(false)}
        width={560}
        loading={detailLoading}
      >
        {selectedEmpDetail && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
            <Descriptions title="Job Details" column={2} bordered size="small">
              <Descriptions.Item label="Department">{selectedEmpDetail.departmentName ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="Designation">{selectedEmpDetail.designationTitle ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="Office Location">{selectedEmpDetail.locationName ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="Status">{getStatusTag(selectedEmpDetail.status)}</Descriptions.Item>
              <Descriptions.Item label="Employment Type">{selectedEmpDetail.employmentType.replace('_', ' ')}</Descriptions.Item>
              <Descriptions.Item label="Reporting Manager">{selectedEmpDetail.managerName ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="Date Joined">{dayjs(selectedEmpDetail.dateOfJoining).format('DD MMM YYYY')}</Descriptions.Item>
              <Descriptions.Item label="Probation Ends">{selectedEmpDetail.probationEndDate ? dayjs(selectedEmpDetail.probationEndDate).format('DD MMM YYYY') : '—'}</Descriptions.Item>
            </Descriptions>

            <Descriptions title="Contact Information" column={1} bordered size="small">
              <Descriptions.Item label="Work Email">{selectedEmpDetail.workEmail}</Descriptions.Item>
              <Descriptions.Item label="Personal Email">{selectedEmpDetail.personalEmail ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="Phone">{selectedEmpDetail.phone ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="City / State">{[selectedEmpDetail.city, selectedEmpDetail.state].filter(Boolean).join(', ') || '—'}</Descriptions.Item>
            </Descriptions>

            <Descriptions title="Emergency Contact" column={1} bordered size="small">
              <Descriptions.Item label="Contact Name">{selectedEmpDetail.emergencyContactName ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="Contact Phone">{selectedEmpDetail.emergencyContactPhone ?? '—'}</Descriptions.Item>
            </Descriptions>
          </div>
        )}
      </Drawer>
    </div>
  );
}
