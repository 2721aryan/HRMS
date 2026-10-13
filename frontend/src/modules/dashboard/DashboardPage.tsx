import { useState, useEffect } from 'react';
import { Card, Row, Col, Typography, Button, Space, Progress, Tag, Avatar, Table } from 'antd';
import {
  TeamOutlined,
  UserAddOutlined,
  ApartmentOutlined,
  CheckCircleOutlined,
  RightOutlined,
  RiseOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { fetchEmployeeStats, fetchEmployees, type EmployeeStats, type EmployeeSummary } from '../../api/employee';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [stats, setStats] = useState<EmployeeStats | null>(null);
  const [recentEmployees, setRecentEmployees] = useState<EmployeeSummary[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      fetchEmployeeStats(),
      fetchEmployees({ page: 0, size: 5, sortBy: 'id', sortDirection: 'DESC' }),
    ])
      .then(([statsRes, empRes]) => {
        setStats(statsRes);
        setRecentEmployees(empRes.content);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const recentColumns = [
    {
      title: 'Member',
      key: 'name',
      render: (_: any, r: EmployeeSummary) => (
        <Space>
          <Avatar style={{ backgroundColor: '#3b82f6' }}>{r.firstName[0]?.toUpperCase()}</Avatar>
          <div>
            <Text strong style={{ color: '#f8fafc' }}>{r.fullName}</Text>
            <br />
            <Text type="secondary" style={{ fontSize: 12 }}>{r.employeeCode}</Text>
          </div>
        </Space>
      ),
    },
    {
      title: 'Department',
      dataIndex: 'departmentName',
      key: 'dept',
      render: (val: string | null) => val ? <Tag color="blue">{val}</Tag> : '—',
    },
    {
      title: 'Joined',
      dataIndex: 'dateOfJoining',
      key: 'date',
      render: (val: string) => dayjs(val).format('DD MMM YYYY'),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (s: string) => s === 'ACTIVE' ? <Tag color="success">Active</Tag> : <Tag color="warning">{s}</Tag>,
    },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Welcome banner */}
      <div
        style={{
          background: 'linear-gradient(135deg, rgba(37,99,235,0.2) 0%, rgba(124,58,237,0.15) 100%)',
          border: '1px solid rgba(255,255,255,0.08)',
          borderRadius: 16,
          padding: '24px 28px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 16,
        }}
      >
        <div>
          <Title level={3} style={{ color: '#f8fafc', margin: 0 }}>
            Welcome back, {user?.fullName || user?.username}! 👋
          </Title>
          <Text style={{ color: '#94a3b8', fontSize: 14 }}>
            Phase 1 HRMS Foundation & Employee Master is live. Here is your organization summary for today.
          </Text>
        </div>
        <Space>
          <Button
            type="primary"
            icon={<UserAddOutlined />}
            onClick={() => navigate('/employees')}
            style={{ background: 'linear-gradient(135deg, #2563eb, #7c3aed)', border: 0 }}
          >
            Employee Directory
          </Button>
          <Button icon={<ApartmentOutlined />} onClick={() => navigate('/organization')}>
            Org Structure
          </Button>
        </Space>
      </div>

      {/* Primary Metrics */}
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card
            variant="borderless"
            style={{ background: 'rgba(30, 41, 59, 0.5)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12 }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <Text type="secondary">Total Headcount</Text>
                <Title level={2} style={{ color: '#f8fafc', margin: '4px 0 0' }}>{stats?.total ?? '—'}</Title>
              </div>
              <div style={{ fontSize: 28, color: '#3b82f6', background: 'rgba(59,130,246,0.1)', padding: 12, borderRadius: 12 }}>
                <TeamOutlined />
              </div>
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card
            variant="borderless"
            style={{ background: 'rgba(30, 41, 59, 0.5)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12 }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <Text type="secondary">Active Workforce</Text>
                <Title level={2} style={{ color: '#10b981', margin: '4px 0 0' }}>{stats?.active ?? '—'}</Title>
              </div>
              <div style={{ fontSize: 28, color: '#10b981', background: 'rgba(16,185,129,0.1)', padding: 12, borderRadius: 12 }}>
                <CheckCircleOutlined />
              </div>
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card
            variant="borderless"
            style={{ background: 'rgba(30, 41, 59, 0.5)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12 }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <Text type="secondary">Departments</Text>
                <Title level={2} style={{ color: '#a855f7', margin: '4px 0 0' }}>
                  {stats ? Object.keys(stats.byDepartment).length : '—'}
                </Title>
              </div>
              <div style={{ fontSize: 28, color: '#a855f7', background: 'rgba(168,85,247,0.1)', padding: 12, borderRadius: 12 }}>
                <ApartmentOutlined />
              </div>
            </div>
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card
            variant="borderless"
            style={{ background: 'rgba(30, 41, 59, 0.5)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12 }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <Text type="secondary">Retention Rate</Text>
                <Title level={2} style={{ color: '#06b6d4', margin: '4px 0 0' }}>
                  {stats && stats.total > 0 ? Math.round((stats.active / stats.total) * 100) : 100}%
                </Title>
              </div>
              <div style={{ fontSize: 28, color: '#06b6d4', background: 'rgba(6,182,212,0.1)', padding: 12, borderRadius: 12 }}>
                <RiseOutlined />
              </div>
            </div>
          </Card>
        </Col>
      </Row>

      {/* Department Breakdown & Recent Hires */}
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={10}>
          <Card
            title={<span style={{ color: '#f8fafc' }}>Department Distribution</span>}
            variant="borderless"
            style={{ background: 'rgba(15, 23, 42, 0.6)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12, height: '100%' }}
          >
            {stats && Object.keys(stats.byDepartment).length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                {Object.entries(stats.byDepartment).map(([dept, count]) => {
                  const pct = Math.round((count / (stats.active || 1)) * 100);
                  return (
                    <div key={dept}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                        <Text strong style={{ color: '#e2e8f0' }}>{dept}</Text>
                        <Text type="secondary">{count} staff ({pct}%)</Text>
                      </div>
                      <Progress percent={pct} strokeColor={{ '0%': '#3b82f6', '100%': '#8b5cf6' }} showInfo={false} />
                    </div>
                  );
                })}
              </div>
            ) : (
              <Text type="secondary">No department data available yet.</Text>
            )}
          </Card>
        </Col>

        <Col xs={24} lg={14}>
          <Card
            title={<span style={{ color: '#f8fafc' }}>Recent Team Members</span>}
            extra={
              <Button type="link" onClick={() => navigate('/employees')}>
                View All <RightOutlined />
              </Button>
            }
            variant="borderless"
            style={{ background: 'rgba(15, 23, 42, 0.6)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12 }}
          >
            <Table
              columns={recentColumns}
              dataSource={recentEmployees}
              rowKey="id"
              pagination={false}
              loading={loading}
              size="small"
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
}
