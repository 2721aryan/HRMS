import { useState, useEffect, useCallback } from 'react';
import {
  Tabs,
  Table,
  Button,
  Card,
  Typography,
  Space,
  Tag,
  Switch,
  Modal,
  Form,
  Input,
  message,
} from 'antd';
import {
  PlusOutlined,
  ApartmentOutlined,
  IdcardOutlined,
  EnvironmentOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import {
  fetchDepartments,
  createDepartment,
  toggleDepartment,
  fetchDesignations,
  createDesignation,
  toggleDesignation,
  fetchLocations,
  type Department,
  type Designation,
  type Location,
} from '../../api/org';

const { Title, Text } = Typography;

export default function OrganizationPage() {
  const [activeTab, setActiveTab] = useState('departments');
  const [departments, setDepartments] = useState<Department[]>([]);
  const [designations, setDesignations] = useState<Designation[]>([]);
  const [locations, setLocations] = useState<Location[]>([]);
  const [loading, setLoading] = useState(false);

  // Modals
  const [isDeptModalOpen, setIsDeptModalOpen] = useState(false);
  const [isDesigModalOpen, setIsDesigModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [deptForm] = Form.useForm();
  const [desigForm] = Form.useForm();

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [deptRes, desigRes, locRes] = await Promise.all([
        fetchDepartments(false),
        fetchDesignations(false),
        fetchLocations(),
      ]);
      setDepartments(deptRes);
      setDesignations(desigRes);
      setLocations(locRes);
    } catch {
      message.error('Failed to load organization settings');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleToggleDept = async (id: number) => {
    try {
      await toggleDepartment(id);
      message.success('Department status updated');
      loadData();
    } catch {
      message.error('Failed to toggle department');
    }
  };

  const handleToggleDesig = async (id: number) => {
    try {
      await toggleDesignation(id);
      message.success('Designation status updated');
      loadData();
    } catch {
      message.error('Failed to toggle designation');
    }
  };

  const handleCreateDept = async () => {
    try {
      const values = await deptForm.validateFields();
      setSubmitting(true);
      await createDepartment({ code: values.code, name: values.name });
      message.success('Department created!');
      setIsDeptModalOpen(false);
      deptForm.resetFields();
      loadData();
    } catch {
      message.error('Please check department details');
    } finally {
      setSubmitting(false);
    }
  };

  const handleCreateDesig = async () => {
    try {
      const values = await desigForm.validateFields();
      setSubmitting(true);
      await createDesignation({ title: values.title, grade: values.grade });
      message.success('Designation created!');
      setIsDesigModalOpen(false);
      desigForm.resetFields();
      loadData();
    } catch {
      message.error('Please check designation details');
    } finally {
      setSubmitting(false);
    }
  };

  const deptColumns = [
    {
      title: 'Code',
      dataIndex: 'code',
      key: 'code',
      render: (val: string) => <Tag color="geekblue" style={{ fontFamily: 'monospace' }}>{val}</Tag>,
    },
    {
      title: 'Department Name',
      dataIndex: 'name',
      key: 'name',
      render: (val: string) => <Text strong style={{ color: '#f8fafc' }}>{val}</Text>,
    },
    {
      title: 'Status',
      dataIndex: 'active',
      key: 'active',
      render: (active: boolean) => active ? <Tag color="success">Active</Tag> : <Tag color="default">Disabled</Tag>,
    },
    {
      title: 'Toggle',
      key: 'toggle',
      align: 'right' as const,
      render: (_: any, record: Department) => (
        <Switch
          checked={record.active}
          onChange={() => handleToggleDept(record.id)}
          size="small"
        />
      ),
    },
  ];

  const desigColumns = [
    {
      title: 'Designation Title',
      dataIndex: 'title',
      key: 'title',
      render: (val: string) => <Text strong style={{ color: '#f8fafc' }}>{val}</Text>,
    },
    {
      title: 'Grade',
      dataIndex: 'grade',
      key: 'grade',
      render: (val: string | null) => val ? <Tag color="purple">{val}</Tag> : <Text type="secondary">—</Text>,
    },
    {
      title: 'Status',
      dataIndex: 'active',
      key: 'active',
      render: (active: boolean) => active ? <Tag color="success">Active</Tag> : <Tag color="default">Disabled</Tag>,
    },
    {
      title: 'Toggle',
      key: 'toggle',
      align: 'right' as const,
      render: (_: any, record: Designation) => (
        <Switch
          checked={record.active}
          onChange={() => handleToggleDesig(record.id)}
          size="small"
        />
      ),
    },
  ];

  const locColumns = [
    {
      title: 'Location Name',
      dataIndex: 'name',
      key: 'name',
      render: (val: string) => <Text strong style={{ color: '#f8fafc' }}>{val}</Text>,
    },
    {
      title: 'City',
      dataIndex: 'city',
      key: 'city',
      render: (val: string | null) => val || '—',
    },
    {
      title: 'State',
      dataIndex: 'state',
      key: 'state',
      render: (val: string | null) => val || '—',
    },
    {
      title: 'Country',
      dataIndex: 'country',
      key: 'country',
    },
    {
      title: 'Status',
      dataIndex: 'active',
      key: 'active',
      render: (active: boolean) => active ? <Tag color="success">Active</Tag> : <Tag color="default">Disabled</Tag>,
    },
  ];

  const tabItems = [
    {
      key: 'departments',
      label: (
        <Space>
          <ApartmentOutlined />
          <span>Departments ({departments.length})</span>
        </Space>
      ),
      children: (
        <Card variant="borderless" style={{ background: 'rgba(15, 23, 42, 0.6)', border: '1px solid rgba(255,255,255,0.08)' }}>
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 16 }}>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setIsDeptModalOpen(true)}
              style={{ background: '#2563eb' }}
            >
              Add Department
            </Button>
          </div>
          <Table
            columns={deptColumns}
            dataSource={departments}
            rowKey="id"
            loading={loading}
            pagination={false}
          />
        </Card>
      ),
    },
    {
      key: 'designations',
      label: (
        <Space>
          <IdcardOutlined />
          <span>Designations ({designations.length})</span>
        </Space>
      ),
      children: (
        <Card variant="borderless" style={{ background: 'rgba(15, 23, 42, 0.6)', border: '1px solid rgba(255,255,255,0.08)' }}>
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 16 }}>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setIsDesigModalOpen(true)}
              style={{ background: '#2563eb' }}
            >
              Add Designation
            </Button>
          </div>
          <Table
            columns={desigColumns}
            dataSource={designations}
            rowKey="id"
            loading={loading}
            pagination={false}
          />
        </Card>
      ),
    },
    {
      key: 'locations',
      label: (
        <Space>
          <EnvironmentOutlined />
          <span>Offices ({locations.length})</span>
        </Space>
      ),
      children: (
        <Card variant="borderless" style={{ background: 'rgba(15, 23, 42, 0.6)', border: '1px solid rgba(255,255,255,0.08)' }}>
          <Table
            columns={locColumns}
            dataSource={locations}
            rowKey="id"
            loading={loading}
            pagination={false}
          />
        </Card>
      ),
    },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={2} style={{ color: '#f8fafc', margin: 0 }}>Organization Setup</Title>
          <Text type="secondary">Manage company organizational structure, designations, and corporate locations</Text>
        </div>
        <Button icon={<ReloadOutlined />} onClick={loadData}>
          Refresh
        </Button>
      </div>

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={tabItems}
        size="large"
      />

      {/* Add Department Modal */}
      <Modal
        title="Add New Department"
        open={isDeptModalOpen}
        onCancel={() => setIsDeptModalOpen(false)}
        onOk={handleCreateDept}
        confirmLoading={submitting}
        okText="Create Department"
      >
        <Form form={deptForm} layout="vertical" requiredMark={false}>
          <Form.Item name="code" label="Department Code" rules={[{ required: true, message: 'Code is required (e.g. ENG)' }]}>
            <Input placeholder="e.g. ENG, HR, MKT" />
          </Form.Item>
          <Form.Item name="name" label="Department Name" rules={[{ required: true, message: 'Name is required' }]}>
            <Input placeholder="e.g. Engineering" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Add Designation Modal */}
      <Modal
        title="Add New Designation"
        open={isDesigModalOpen}
        onCancel={() => setIsDesigModalOpen(false)}
        onOk={handleCreateDesig}
        confirmLoading={submitting}
        okText="Create Designation"
      >
        <Form form={desigForm} layout="vertical" requiredMark={false}>
          <Form.Item name="title" label="Job Title" rules={[{ required: true, message: 'Job title is required' }]}>
            <Input placeholder="e.g. Senior Software Engineer" />
          </Form.Item>
          <Form.Item name="grade" label="Level / Grade">
            <Input placeholder="e.g. L2, L3, M1" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
