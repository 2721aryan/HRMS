import { useState, useEffect } from 'react';
import { Card, Row, Col, Typography, Button, Space, Progress, Tag, Avatar, Table } from 'antd';
import {
  TeamOutlined,
  UserAddOutlined,
  ApartmentOutlined,
  CheckCircleOutlined,
  RightOutlined,
  ClockCircleOutlined,
  CalendarOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { fetchEmployees, type EmployeeSummary } from '../../api/employee';
import { fetchDashboardStats, type DashboardStats } from '../../api/dashboard';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [dashStats, setDashStats] = useState<DashboardStats | null>(null);
  const [recentEmployees, setRecentEmployees] = useState<EmployeeSummary[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      fetchDashboardStats(),
      fetchEmployees({ page: 0, size: 5, sortBy: 'id', sortDirection: 'DESC' }),
    ])
      .then(([statsRes, empRes]) => {
        setDashStats(statsRes);
        setRecentEmployees(empRes.content);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const isPersonal = dashStats?.scope === 'PERSONAL';
  const isTeam = dashStats?.scope === 'TEAM';
  const isCompany = dashStats?.scope === 'COMPANY';

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
      render: (val: string | null) => val ? dayjs(val).format('DD MMM YYYY') : '—',
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
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
            <Title level={3} style={{ color: '#f8fafc', margin: 0 }}>
              Welcome back, {user?.fullName || user?.username}! 👋
            </Title>
            <Tag color={isCompany ? 'gold' : isTeam ? 'purple' : 'blue'}>
              {dashStats?.scope || 'ROLE'} SCOPE
            </Tag>
          </div>
          <Text style={{ color: '#94a3b8', fontSize: 14 }}>
            {isPersonal && 'Here is your personal attendance and leave summary for today.'}
            {isTeam && 'Here is your direct team attendance, status, and pending approvals summary.'}
            {isCompany && 'Here is the organization-wide workforce metrics and status overview.'}
          </Text>
        </div>
        <Space>
          {isPersonal ? (
            <>
              <Button type="primary" icon={<ClockCircleOutlined />} onClick={() => navigate('/attendance')} style={{ background: '#2563eb' }}>
                Attendance
              </Button>
              <Button icon={<CalendarOutlined />} onClick={() => navigate('/leaves')}>
                Leaves
              </Button>
            </>
          ) : (
            <>
              <Button
                type="primary"
                icon={<UserAddOutlined />}
                onClick={() => navigate('/employees')}
                style={{ background: 'linear-gradient(135deg, #2563eb, #7c3aed)', border: 0 }}
              >
                Employee Directory
              </Button>
              {isCompany && (
                <Button icon={<ApartmentOutlined />} onClick={() => navigate('/organization')}>
                  Org Structure
                </Button>
              )}
            </>
          )}
        </Space>
      </div>

      {/* Role Scoped Metrics */}
      <Row gutter={[16, 16]}>
        {isPersonal && (
          <>
            <Col xs={24} sm={12} lg={6}>
              <Card variant="borderless" style={{ background: 'rgba(30, 41, 59, 0.5)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <Text type="secondary">Today's Check-in</Text>
                    <Title level={3} style={{ color: dashStats.todayAttendanceStatus === 'PRESENT' ? '#10b981' : '#f59e0b', margin: '4px 0 0' }}>
                      {dashStats.todayAttendanceStatus === 'PRESENT' ? 'Clocked In' : 'Not Clocked In'}
                    </Title>
                  </div>
                  <div style={{ fontSize: 28, color: '#10b981', background: 'rgba(16,185,129,0.1)', padding: 12, borderRadius: 12 }}>
                    <CheckCircleOutlined />
                  </div>
                </div>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card variant="borderless" style={{ background: 'rgba(30, 41, 59, 0.5)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <Text type="secondary">Days Present (This Month)</Text>
                    <Title level={2} style={{ color: '#3b82f6', margin: '4px 0 0' }}>{dashStats.monthPresentDays ?? 0}</Title>
                  </div>
                  <div style={{ fontSize: 28, color: '#3b82f6', background: 'rgba(59,130,246,0.1)', padding: 12, borderRadius: 12 }}>
                    <ClockCircleOutlined />
                  </div>
                </div>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card variant="borderless" style={{ background: 'rgba(30, 41, 59, 0.5)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <Text type="secondary">Leave Balance</Text>
                    <Title level={2} style={{ color: '#a855f7', margin: '4px 0 0' }}>{dashStats.remainingLeaveDays ?? 0} days</Title>
                  </div>
                  <div style={{ fontSize: 28, color: '#a855f7', background: 'rgba(168,85,247,0.1)', padding: 12, borderRadius: 12 }}>
                    <CalendarOutlined />
                  </div>
                </div>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card variant="borderless" style={{ background: 'rgba(30, 41, 59, 0.5)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <Text type="secondary">Pending Requests</Text>
                    <Title level={2} style={{ color: '#06b6d4', margin: '4px 0 0' }}>{dashStats.myPendingRequestsCount ?? 0}</Title>
                  </div>
                  <div style={{ fontSize: 28, color: '#06b6d4', background: 'rgba(6,182,212,0.1)', padding: 12, borderRadius: 12 }}>
                    <ExclamationCircleOutlined />
                  </div>
                </div>
              </Card>
            </Col>
          </>
        )}

        {isTeam && (
          <>
            <Col xs={24} sm={12} lg={6}>
              <Card variant="borderless" style={{ background: 'rgba(30, 41, 59, 0.5)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <Text type="secondary">Direct Team Size</Text>
                    <Title level={2} style={{ color: '#3b82f6', margin: '4px 0 0' }}>{dashStats.teamSize ?? 0}</Title>
                  </div>
                  <div style={{ fontSize: 28, color: '#3b82f6', background: 'rgba(59,130,246,0.1)', padding: 12, borderRadius: 12 }}>
                    <TeamOutlined />
                  </div>
                </div>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card variant="borderless" style={{ background: 'rgba(30, 41, 59, 0.5)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <Text type="secondary">Team Present Today</Text>
                    <Title level={2} style={{ color: '#10b981', margin: '4px 0 0' }}>{dashStats.teamPresentToday ?? 0}</Title>
                  </div>
                  <div style={{ fontSize: 28, color: '#10b981', background: 'rgba(16,185,129,0.1)', padding: 12, borderRadius: 12 }}>
                    <CheckCircleOutlined />
                  </div>
                </div>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card variant="borderless" style={{ background: 'rgba(30, 41, 59, 0.5)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <Text type="secondary">Team On Leave</Text>
                    <Title level={2} style={{ color: '#a855f7', margin: '4px 0 0' }}>{dashStats.teamOnLeaveToday ?? 0}</Title>
                  </div>
                  <div style={{ fontSize: 28, color: '#a855f7', background: 'rgba(168,85,247,0.1)', padding: 12, borderRadius: 12 }}>
                    <CalendarOutlined />
                  </div>
                </div>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card variant="borderless" style={{ background: 'rgba(30, 41, 59, 0.5)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <Text type="secondary">Pending Approvals</Text>
                    <Title level={2} style={{ color: '#f59e0b', margin: '4px 0 0' }}>{dashStats.teamPendingApprovals ?? 0}</Title>
                  </div>
                  <div style={{ fontSize: 28, color: '#f59e0b', background: 'rgba(245,158,11,0.1)', padding: 12, borderRadius: 12 }}>
                    <ExclamationCircleOutlined />
                  </div>
                </div>
              </Card>
            </Col>
          </>
        )}

        {isCompany && (
          <>
            <Col xs={24} sm={12} lg={6}>
              <Card variant="borderless" style={{ background: 'rgba(30, 41, 59, 0.5)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <Text type="secondary">Total Headcount</Text>
                    <Title level={2} style={{ color: '#f8fafc', margin: '4px 0 0' }}>{dashStats.totalEmployees ?? '—'}</Title>
                  </div>
                  <div style={{ fontSize: 28, color: '#3b82f6', background: 'rgba(59,130,246,0.1)', padding: 12, borderRadius: 12 }}>
                    <TeamOutlined />
                  </div>
                </div>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card variant="borderless" style={{ background: 'rgba(30, 41, 59, 0.5)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <Text type="secondary">Active Workforce</Text>
                    <Title level={2} style={{ color: '#10b981', margin: '4px 0 0' }}>{dashStats.activeEmployees ?? '—'}</Title>
                  </div>
                  <div style={{ fontSize: 28, color: '#10b981', background: 'rgba(16,185,129,0.1)', padding: 12, borderRadius: 12 }}>
                    <CheckCircleOutlined />
                  </div>
                </div>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card variant="borderless" style={{ background: 'rgba(30, 41, 59, 0.5)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <Text type="secondary">Present Today</Text>
                    <Title level={2} style={{ color: '#a855f7', margin: '4px 0 0' }}>{dashStats.companyPresentToday ?? 0}</Title>
                  </div>
                  <div style={{ fontSize: 28, color: '#a855f7', background: 'rgba(168,85,247,0.1)', padding: 12, borderRadius: 12 }}>
                    <ClockCircleOutlined />
                  </div>
                </div>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card variant="borderless" style={{ background: 'rgba(30, 41, 59, 0.5)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <Text type="secondary">Pending Approvals</Text>
                    <Title level={2} style={{ color: '#06b6d4', margin: '4px 0 0' }}>{dashStats.companyPendingApprovals ?? 0}</Title>
                  </div>
                  <div style={{ fontSize: 28, color: '#06b6d4', background: 'rgba(6,182,212,0.1)', padding: 12, borderRadius: 12 }}>
                    <ExclamationCircleOutlined />
                  </div>
                </div>
              </Card>
            </Col>
          </>
        )}
      </Row>

      {/* Department Breakdown & Recent Hires */}
      {/* Department Breakdown & Directory */}
      <Row gutter={[16, 16]}>
        {isCompany && (
          <Col xs={24} lg={10}>
            <Card
              title={<span style={{ color: '#f8fafc' }}>Department Distribution</span>}
              variant="borderless"
              style={{ background: 'rgba(15, 23, 42, 0.6)', border: '1px solid rgba(255,255,255,0.08)', borderRadius: 12, height: '100%' }}
            >
              {dashStats?.departmentBreakdown && Object.keys(dashStats.departmentBreakdown).length > 0 ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                  {Object.entries(dashStats.departmentBreakdown).map(([dept, count]) => {
                    const pct = Math.round((count / (dashStats.activeEmployees || 1)) * 100);
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
        )}

        <Col xs={24} lg={isCompany ? 14 : 24}>
          <Card
            title={<span style={{ color: '#f8fafc' }}>{isPersonal ? 'Colleagues & Peers' : isTeam ? 'Direct Team Directory' : 'Recent Team Members'}</span>}
            extra={
              <Button type="link" onClick={() => navigate('/employees')}>
                View Directory <RightOutlined />
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
