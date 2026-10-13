import { useState, useEffect } from 'react';
import {
  Card,
  Row,
  Col,
  Typography,
  Button,
  Tag,
  Table,
  Tabs,
  Space,
  DatePicker,
  Modal,
  Form,
  Input,
  TimePicker,
  Select,
  message,
  Statistic,
  Tooltip,
  Badge,
} from 'antd';
import {
  ClockCircleOutlined,
  LoginOutlined,
  LogoutOutlined,
  ExclamationCircleOutlined,
  HistoryOutlined,
  TeamOutlined,
  AuditOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import duration from 'dayjs/plugin/duration';
import { attendanceApi, type TodayAttendance, type AttendanceRecord, type AttendanceStats, type RegularizationItem } from '../../api/attendance';
import { fetchDepartments, type Department } from '../../api/org';
import { useAuth } from '../../auth/AuthContext';

dayjs.extend(duration);

const { Title, Text } = Typography;

export default function AttendancePage() {
  const { user } = useAuth();
  const isAdminOrHR = user?.roles?.some((r: string) => r === 'ADMIN' || r === 'HR' || r === 'MANAGER');

  // State
  const [today, setToday] = useState<TodayAttendance | null>(null);
  const [stats, setStats] = useState<AttendanceStats | null>(null);
  const [myRecords, setMyRecords] = useState<AttendanceRecord[]>([]);
  const [companyRecords, setCompanyRecords] = useState<AttendanceRecord[]>([]);
  const [regularizations, setRegularizations] = useState<RegularizationItem[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);

  const [, setLoadingToday] = useState(false);
  const [loadingPunch, setLoadingPunch] = useState(false);
  const [loadingRecords, setLoadingRecords] = useState(false);
  const [loadingCompany, setLoadingCompany] = useState(false);
  const [loadingRegs, setLoadingRegs] = useState(false);

  // Filters
  const [myMonth, setMyMonth] = useState(dayjs());
  const [compDateRange, setCompDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs]>([
    dayjs().startOf('month'),
    dayjs(),
  ]);
  const [selectedDept, setSelectedDept] = useState<number | undefined>();
  const [selectedStatus, setSelectedStatus] = useState<string | undefined>();

  // Modals
  const [regModalVisible, setRegModalVisible] = useState(false);
  const [regForm] = Form.useForm();
  const [submittingReg, setSubmittingReg] = useState(false);

  const [decisionModalVisible, setDecisionModalVisible] = useState(false);
  const [activeRegItem, setActiveRegItem] = useState<RegularizationItem | null>(null);
  const [decisionAction, setDecisionAction] = useState<'APPROVE' | 'REJECT'>('APPROVE');
  const [decisionComment, setDecisionComment] = useState('');
  const [submittingDecision, setSubmittingDecision] = useState(false);

  // Live clock
  const [currentTime, setCurrentTime] = useState(dayjs());

  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(dayjs()), 1000);
    return () => clearInterval(timer);
  }, []);

  const loadToday = async () => {
    try {
      setLoadingToday(true);
      const data = await attendanceApi.getToday();
      setToday(data);
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Failed to load today status');
    } finally {
      setLoadingToday(false);
    }
  };

  const loadStats = async () => {
    if (!isAdminOrHR) return;
    try {
      const data = await attendanceApi.getStats();
      setStats(data);
    } catch {
      // ignore
    }
  };

  const loadMyRecords = async () => {
    try {
      setLoadingRecords(true);
      const from = myMonth.startOf('month').format('YYYY-MM-DD');
      const to = myMonth.endOf('month').format('YYYY-MM-DD');
      const data = await attendanceApi.getMyRecords(from, to);
      setMyRecords(data);
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Failed to load timesheet');
    } finally {
      setLoadingRecords(false);
    }
  };

  const loadCompanyRecords = async () => {
    if (!isAdminOrHR) return;
    try {
      setLoadingCompany(true);
      const res = await attendanceApi.getRecords({
        startDate: compDateRange[0].format('YYYY-MM-DD'),
        endDate: compDateRange[1].format('YYYY-MM-DD'),
        departmentId: selectedDept,
        status: selectedStatus,
        page: 0,
        size: 50,
      });
      setCompanyRecords(res.content);
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Failed to load company records');
    } finally {
      setLoadingCompany(false);
    }
  };

  const loadRegularizations = async () => {
    try {
      setLoadingRegs(true);
      if (isAdminOrHR) {
        const res = await attendanceApi.getRegularizations();
        setRegularizations(res.content);
      } else {
        const res = await attendanceApi.getMyRegularizations();
        setRegularizations(res);
      }
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Failed to load regularizations');
    } finally {
      setLoadingRegs(false);
    }
  };

  const loadDepartments = async () => {
    if (!isAdminOrHR) return;
    try {
      const depts = await fetchDepartments();
      setDepartments(depts);
    } catch {
      // ignore
    }
  };

  useEffect(() => {
    loadToday();
    loadStats();
    loadMyRecords();
    loadDepartments();
    loadRegularizations();
  }, []);

  useEffect(() => {
    loadMyRecords();
  }, [myMonth]);

  useEffect(() => {
    if (isAdminOrHR) {
      loadCompanyRecords();
    }
  }, [compDateRange, selectedDept, selectedStatus]);

  // Punch actions
  const handlePunchIn = async () => {
    try {
      setLoadingPunch(true);
      const res = await attendanceApi.punchIn();
      setToday(res);
      message.success('Successfully clocked in!');
      loadMyRecords();
      loadStats();
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Clock in failed');
    } finally {
      setLoadingPunch(false);
    }
  };

  const handlePunchOut = async () => {
    try {
      setLoadingPunch(true);
      const res = await attendanceApi.punchOut();
      setToday(res);
      message.success('Successfully clocked out!');
      loadMyRecords();
      loadStats();
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Clock out failed');
    } finally {
      setLoadingPunch(false);
    }
  };

  // Submit regularization
  const handleRegularizationSubmit = async (values: any) => {
    try {
      setSubmittingReg(true);
      const dateStr = values.attendanceDate.format('YYYY-MM-DD');
      const inStr = values.checkIn ? `${dateStr}T${values.checkIn.format('HH:mm:ss')}Z` : undefined;
      const outStr = values.checkOut ? `${dateStr}T${values.checkOut.format('HH:mm:ss')}Z` : undefined;

      await attendanceApi.requestRegularization({
        attendanceDate: dateStr,
        requestedCheckIn: inStr,
        requestedCheckOut: outStr,
        reason: values.reason,
      });

      message.success('Regularization request submitted');
      setRegModalVisible(false);
      regForm.resetFields();
      loadRegularizations();
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Failed to submit regularization');
    } finally {
      setSubmittingReg(false);
    }
  };

  // Decision on regularization
  const handleDecisionSubmit = async () => {
    if (!activeRegItem) return;
    try {
      setSubmittingDecision(true);
      if (decisionAction === 'APPROVE') {
        await attendanceApi.approveRegularization(activeRegItem.id, decisionComment);
        message.success('Regularization approved');
      } else {
        await attendanceApi.rejectRegularization(activeRegItem.id, decisionComment);
        message.info('Regularization rejected');
      }
      setDecisionModalVisible(false);
      setDecisionComment('');
      loadRegularizations();
      loadMyRecords();
      if (isAdminOrHR) loadCompanyRecords();
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Action failed');
    } finally {
      setSubmittingDecision(false);
    }
  };

  const renderStatusTag = (st: string, late?: boolean) => {
    switch (st) {
      case 'PRESENT':
        return (
          <Space size={4}>
            <Tag color="success">Present</Tag>
            {late && <Tag color="warning">Late</Tag>}
          </Space>
        );
      case 'HALF_DAY':
        return <Tag color="orange">Half Day</Tag>;
      case 'ON_LEAVE':
        return <Tag color="processing">On Leave</Tag>;
      case 'HOLIDAY':
        return <Tag color="cyan">Holiday</Tag>;
      case 'WEEKLY_OFF':
        return <Tag color="default">Weekly Off</Tag>;
      case 'ABSENT':
        return <Tag color="error">Absent</Tag>;
      default:
        return <Tag>{st}</Tag>;
    }
  };

  // Columns for My Records
  const myColumns = [
    {
      title: 'Date',
      dataIndex: 'attendanceDate',
      key: 'attendanceDate',
      render: (val: string) => (
        <Text strong style={{ color: '#f8fafc' }}>
          {dayjs(val).format('ddd, DD MMM YYYY')}
        </Text>
      ),
    },
    {
      title: 'Check In',
      dataIndex: 'checkIn',
      key: 'checkIn',
      render: (val: string | null) => (val ? dayjs(val).format('hh:mm A') : '—'),
    },
    {
      title: 'Check Out',
      dataIndex: 'checkOut',
      key: 'checkOut',
      render: (val: string | null) => (val ? dayjs(val).format('hh:mm A') : '—'),
    },
    {
      title: 'Working Hours',
      dataIndex: 'workMinutes',
      key: 'workMinutes',
      render: (mins: number) => {
        if (!mins) return '—';
        const h = Math.floor(mins / 60);
        const m = mins % 60;
        return `${h}h ${m}m`;
      },
    },
    {
      title: 'Status',
      key: 'status',
      render: (_: any, r: AttendanceRecord) => renderStatusTag(r.status, r.late),
    },
    {
      title: 'Remarks',
      dataIndex: 'remarks',
      key: 'remarks',
      render: (r: string) => r || '—',
    },
  ];

  // Columns for Company Records
  const companyColumns = [
    {
      title: 'Employee',
      key: 'employee',
      render: (_: any, r: AttendanceRecord) => (
        <div>
          <Text strong style={{ color: '#f8fafc' }}>{r.employeeName}</Text>
          <br />
          <Text type="secondary" style={{ fontSize: 12 }}>{r.employeeCode} · {r.departmentName || 'General'}</Text>
        </div>
      ),
    },
    {
      title: 'Date',
      dataIndex: 'attendanceDate',
      key: 'attendanceDate',
      render: (val: string) => dayjs(val).format('DD MMM YYYY'),
    },
    {
      title: 'In / Out',
      key: 'times',
      render: (_: any, r: AttendanceRecord) => (
        <div>
          <span>{r.checkIn ? dayjs(r.checkIn).format('hh:mm A') : '—'}</span>
          <span style={{ margin: '0 6px', color: 'rgba(255,255,255,0.3)' }}>→</span>
          <span>{r.checkOut ? dayjs(r.checkOut).format('hh:mm A') : '—'}</span>
        </div>
      ),
    },
    {
      title: 'Hours',
      dataIndex: 'workMinutes',
      key: 'workMinutes',
      render: (mins: number) => {
        if (!mins) return '—';
        return `${Math.floor(mins / 60)}h ${mins % 60}m`;
      },
    },
    {
      title: 'Status',
      key: 'status',
      render: (_: any, r: AttendanceRecord) => renderStatusTag(r.status, r.late),
    },
    {
      title: 'Source',
      dataIndex: 'source',
      key: 'source',
      render: (s: string) => <Tag color="blue" style={{ fontSize: 11 }}>{s}</Tag>,
    },
  ];

  // Columns for Regularizations
  const regColumns = [
    {
      title: 'Employee',
      key: 'employee',
      render: (_: any, r: RegularizationItem) => (
        <div>
          <Text strong style={{ color: '#f8fafc' }}>{r.employeeName}</Text>
          <br />
          <Text type="secondary" style={{ fontSize: 12 }}>{r.employeeCode}</Text>
        </div>
      ),
    },
    {
      title: 'Target Date',
      dataIndex: 'attendanceDate',
      key: 'attendanceDate',
      render: (val: string) => dayjs(val).format('ddd, DD MMM YYYY'),
    },
    {
      title: 'Requested Times',
      key: 'requestedTimes',
      render: (_: any, r: RegularizationItem) => (
        <div>
          <span>In: {r.requestedCheckIn ? dayjs(r.requestedCheckIn).format('hh:mm A') : '—'}</span>
          <br />
          <span>Out: {r.requestedCheckOut ? dayjs(r.requestedCheckOut).format('hh:mm A') : '—'}</span>
        </div>
      ),
    },
    {
      title: 'Reason',
      dataIndex: 'reason',
      key: 'reason',
      ellipsis: true,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (st: string) => {
        const color = st === 'APPROVED' ? 'success' : st === 'REJECTED' ? 'error' : 'warning';
        return <Tag color={color}>{st}</Tag>;
      },
    },
    {
      title: 'Action',
      key: 'action',
      render: (_: any, r: RegularizationItem) => {
        if (r.status !== 'PENDING' || !isAdminOrHR) {
          return r.decidedAt ? (
            <Tooltip title={r.decisionComment ? `Comment: ${r.decisionComment}` : undefined}>
              <Text type="secondary" style={{ fontSize: 12 }}>
                {r.approverName ? `By ${r.approverName}` : ''}
              </Text>
            </Tooltip>
          ) : '—';
        }
        return (
          <Space>
            <Button
              type="primary"
              size="small"
              style={{ background: '#10b981', borderColor: '#10b981' }}
              onClick={() => {
                setActiveRegItem(r);
                setDecisionAction('APPROVE');
                setDecisionModalVisible(true);
              }}
            >
              Approve
            </Button>
            <Button
              danger
              size="small"
              onClick={() => {
                setActiveRegItem(r);
                setDecisionAction('REJECT');
                setDecisionModalVisible(true);
              }}
            >
              Reject
            </Button>
          </Space>
        );
      },
    },
  ];

  return (
    <div style={{ maxWidth: 1400, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Top Header & Live Punch Card */}
      <Row gutter={[20, 20]} align="stretch">
        {/* Live Clock & Punch Action */}
        <Col xs={24} lg={14}>
          <Card
            variant="borderless"
            style={{
              background: 'linear-gradient(135deg, rgba(30,41,59,0.85) 0%, rgba(15,23,42,0.95) 100%)',
              border: '1px solid rgba(255,255,255,0.1)',
              borderRadius: 16,
              boxShadow: '0 8px 32px rgba(0,0,0,0.3)',
              height: '100%',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
            }}
          >
            <Row align="middle" justify="space-between" gutter={[16, 16]}>
              <Col xs={24} sm={14}>
                <Space direction="vertical" size={4}>
                  <Space align="center">
                    <ClockCircleOutlined style={{ color: '#38bdf8', fontSize: 20 }} />
                    <Title level={4} style={{ color: '#f8fafc', margin: 0 }}>
                      Live Attendance Punch
                    </Title>
                    {today?.hasCheckedIn && !today?.hasCheckedOut && (
                      <Badge status="processing" text={<span style={{ color: '#10b981', fontWeight: 600 }}>Active Shift</span>} />
                    )}
                  </Space>
                  <Text style={{ color: '#38bdf8', fontSize: 28, fontWeight: 700, fontFamily: 'monospace' }}>
                    {currentTime.format('hh:mm:ss A')}
                  </Text>
                  <Text type="secondary">
                    {currentTime.format('dddd, MMMM D, YYYY')} · Shift: {today?.shiftName || 'General Shift (09:30 - 18:30)'}
                  </Text>
                  {today?.late && (
                    <Tag color="warning" icon={<ExclamationCircleOutlined />}>
                      Clocked In After Grace Period
                    </Tag>
                  )}
                </Space>
              </Col>

              <Col xs={24} sm={10} style={{ textAlign: 'right' }}>
                <Space direction="vertical" size={12} style={{ width: '100%', alignItems: 'flex-end' }}>
                  {!today?.hasCheckedIn ? (
                    <Button
                      type="primary"
                      size="large"
                      icon={<LoginOutlined />}
                      loading={loadingPunch}
                      onClick={handlePunchIn}
                      style={{
                        height: 52,
                        padding: '0 36px',
                        fontSize: 16,
                        fontWeight: 600,
                        background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
                        border: 'none',
                        borderRadius: 12,
                        boxShadow: '0 4px 20px rgba(16,185,129,0.4)',
                      }}
                    >
                      Clock In Now
                    </Button>
                  ) : !today?.hasCheckedOut ? (
                    <Button
                      type="primary"
                      size="large"
                      danger
                      icon={<LogoutOutlined />}
                      loading={loadingPunch}
                      onClick={handlePunchOut}
                      style={{
                        height: 52,
                        padding: '0 36px',
                        fontSize: 16,
                        fontWeight: 600,
                        background: 'linear-gradient(135deg, #ef4444 0%, #dc2626 100%)',
                        border: 'none',
                        borderRadius: 12,
                        boxShadow: '0 4px 20px rgba(239,68,68,0.4)',
                      }}
                    >
                      Clock Out
                    </Button>
                  ) : (
                    <div style={{ textAlign: 'right' }}>
                      <Tag color="success" style={{ padding: '6px 12px', fontSize: 13, borderRadius: 8 }}>
                        ✓ Shift Completed for Today
                      </Tag>
                    </div>
                  )}

                  <div style={{ display: 'flex', gap: 16, color: '#94a3b8', fontSize: 13 }}>
                    <div>
                      In: <strong style={{ color: '#f8fafc' }}>{today?.checkIn ? dayjs(today.checkIn).format('hh:mm A') : '—'}</strong>
                    </div>
                    <div>
                      Out: <strong style={{ color: '#f8fafc' }}>{today?.checkOut ? dayjs(today.checkOut).format('hh:mm A') : '—'}</strong>
                    </div>
                    <div>
                      Worked: <strong style={{ color: '#38bdf8' }}>{today ? `${Math.floor(today.workMinutes / 60)}h ${today.workMinutes % 60}m` : '0h 0m'}</strong>
                    </div>
                  </div>
                </Space>
              </Col>
            </Row>
          </Card>
        </Col>

        {/* Today's Overview Stats (if Admin/HR) or Quick Actions */}
        <Col xs={24} lg={10}>
          <Card
            variant="borderless"
            style={{
              background: 'rgba(30,41,59,0.6)',
              border: '1px solid rgba(255,255,255,0.08)',
              borderRadius: 16,
              height: '100%',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <Title level={5} style={{ color: '#f8fafc', margin: 0 }}>
                {isAdminOrHR ? "Today's Organization Pulse" : "Attendance Regularization"}
              </Title>
              <Button
                type="primary"
                ghost
                size="small"
                icon={<AuditOutlined />}
                onClick={() => setRegModalVisible(true)}
              >
                Request Regularization
              </Button>
            </div>

            {isAdminOrHR && stats ? (
              <Row gutter={[16, 16]}>
                <Col span={6}>
                  <Statistic
                    title={<span style={{ color: '#94a3b8', fontSize: 12 }}>Present</span>}
                    value={stats.presentCount}
                    valueStyle={{ color: '#10b981', fontWeight: 700 }}
                  />
                </Col>
                <Col span={6}>
                  <Statistic
                    title={<span style={{ color: '#94a3b8', fontSize: 12 }}>Late</span>}
                    value={stats.lateCount}
                    valueStyle={{ color: '#f59e0b', fontWeight: 700 }}
                  />
                </Col>
                <Col span={6}>
                  <Statistic
                    title={<span style={{ color: '#94a3b8', fontSize: 12 }}>On Leave</span>}
                    value={stats.onLeaveCount}
                    valueStyle={{ color: '#38bdf8', fontWeight: 700 }}
                  />
                </Col>
                <Col span={6}>
                  <Statistic
                    title={<span style={{ color: '#94a3b8', fontSize: 12 }}>Absent</span>}
                    value={stats.absentCount}
                    valueStyle={{ color: '#ef4444', fontWeight: 700 }}
                  />
                </Col>
              </Row>
            ) : (
              <div style={{ color: '#94a3b8', fontSize: 13, lineHeight: 1.6 }}>
                Forgot to clock in or had connectivity issues? Submit a regularization request with your intended punch times to have your manager or HR review and correct your record.
              </div>
            )}
          </Card>
        </Col>
      </Row>

      {/* Main Tabbed Interface */}
      <Card
        variant="borderless"
        style={{
          background: 'rgba(30,41,59,0.7)',
          border: '1px solid rgba(255,255,255,0.08)',
          borderRadius: 16,
        }}
      >
        <Tabs
          defaultActiveKey="my-timesheet"
          tabBarStyle={{ borderBottom: '1px solid rgba(255,255,255,0.08)' }}
          items={[
            {
              key: 'my-timesheet',
              label: (
                <Space>
                  <HistoryOutlined />
                  <span>My Monthly Timesheet</span>
                </Space>
              ),
              children: (
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16, flexWrap: 'wrap', gap: 12 }}>
                    <Space align="center">
                      <Text type="secondary">Filter Month:</Text>
                      <DatePicker
                        picker="month"
                        value={myMonth}
                        onChange={(d) => d && setMyMonth(d)}
                        allowClear={false}
                      />
                    </Space>
                    <Button icon={<ReloadOutlined />} onClick={loadMyRecords} loading={loadingRecords}>
                      Refresh
                    </Button>
                  </div>

                  <Table
                    dataSource={myRecords}
                    columns={myColumns}
                    rowKey="id"
                    loading={loadingRecords}
                    pagination={{ pageSize: 15 }}
                    style={{ background: 'transparent' }}
                  />
                </div>
              ),
            },
            ...(isAdminOrHR
              ? [
                  {
                    key: 'company-attendance',
                    label: (
                      <Space>
                        <TeamOutlined />
                        <span>Company Records</span>
                      </Space>
                    ),
                    children: (
                      <div>
                        <div style={{ display: 'flex', gap: 16, marginBottom: 16, flexWrap: 'wrap', alignItems: 'center' }}>
                          <DatePicker.RangePicker
                            value={compDateRange}
                            onChange={(dates) => dates && dates[0] && dates[1] && setCompDateRange([dates[0], dates[1]])}
                            allowClear={false}
                          />

                          <Select
                            placeholder="Department"
                            allowClear
                            style={{ width: 180 }}
                            value={selectedDept}
                            onChange={setSelectedDept}
                            options={departments.map(d => ({ label: d.name, value: d.id }))}
                          />

                          <Select
                            placeholder="Status"
                            allowClear
                            style={{ width: 140 }}
                            value={selectedStatus}
                            onChange={setSelectedStatus}
                            options={[
                              { label: 'Present', value: 'PRESENT' },
                              { label: 'Half Day', value: 'HALF_DAY' },
                              { label: 'On Leave', value: 'ON_LEAVE' },
                              { label: 'Absent', value: 'ABSENT' },
                            ]}
                          />

                          <Button icon={<ReloadOutlined />} onClick={loadCompanyRecords} loading={loadingCompany}>
                            Refresh
                          </Button>
                        </div>

                        <Table
                          dataSource={companyRecords}
                          columns={companyColumns}
                          rowKey="id"
                          loading={loadingCompany}
                          pagination={{ pageSize: 20 }}
                        />
                      </div>
                    ),
                  },
                ]
              : []),
            {
              key: 'regularizations',
              label: (
                <Space>
                  <AuditOutlined />
                  <span>Regularizations</span>
                  {regularizations.filter(r => r.status === 'PENDING').length > 0 && (
                    <Badge count={regularizations.filter(r => r.status === 'PENDING').length} style={{ backgroundColor: '#f59e0b' }} />
                  )}
                </Space>
              ),
              children: (
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                    <Text type="secondary">
                      {isAdminOrHR ? 'Review organization regularization requests' : 'Your regularization requests'}
                    </Text>
                    <Button icon={<ReloadOutlined />} onClick={loadRegularizations} loading={loadingRegs}>
                      Refresh
                    </Button>
                  </div>

                  <Table
                    dataSource={regularizations}
                    columns={regColumns}
                    rowKey="id"
                    loading={loadingRegs}
                    pagination={{ pageSize: 10 }}
                  />
                </div>
              ),
            },
          ]}
        />
      </Card>

      {/* Regularization Request Modal */}
      <Modal
        title="Request Attendance Regularization"
        open={regModalVisible}
        onCancel={() => setRegModalVisible(false)}
        footer={null}
        destroyOnClose
      >
        <Form form={regForm} layout="vertical" onFinish={handleRegularizationSubmit}>
          <Form.Item
            name="attendanceDate"
            label="Attendance Date"
            rules={[{ required: true, message: 'Please select date' }]}
            initialValue={dayjs()}
          >
            <DatePicker style={{ width: '100%' }} maxDate={dayjs()} />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="checkIn"
                label="Requested Check-In"
                rules={[{ required: true, message: 'Select check-in time' }]}
              >
                <TimePicker format="HH:mm" style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="checkOut"
                label="Requested Check-Out"
                rules={[{ required: true, message: 'Select check-out time' }]}
              >
                <TimePicker format="HH:mm" style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="reason"
            label="Reason for Regularization"
            rules={[{ required: true, message: 'Please provide reason' }]}
          >
            <Input.TextArea rows={3} placeholder="e.g. Forgot to clock in due to network error..." />
          </Form.Item>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 16 }}>
            <Button onClick={() => setRegModalVisible(false)}>Cancel</Button>
            <Button type="primary" htmlType="submit" loading={submittingReg}>
              Submit Request
            </Button>
          </div>
        </Form>
      </Modal>

      {/* Decision Modal (Approve / Reject) */}
      <Modal
        title={`${decisionAction === 'APPROVE' ? 'Approve' : 'Reject'} Regularization`}
        open={decisionModalVisible}
        onCancel={() => setDecisionModalVisible(false)}
        onOk={handleDecisionSubmit}
        confirmLoading={submittingDecision}
        okText={decisionAction === 'APPROVE' ? 'Confirm Approval' : 'Confirm Rejection'}
        okButtonProps={{ danger: decisionAction === 'REJECT' }}
      >
        <div style={{ marginBottom: 16 }}>
          <Text>
            {decisionAction === 'APPROVE'
              ? `Approve regularization for ${activeRegItem?.employeeName} on ${activeRegItem?.attendanceDate}?`
              : `Reject regularization request for ${activeRegItem?.employeeName}?`}
          </Text>
        </div>
        <Input.TextArea
          rows={3}
          placeholder="Optional remarks/comments..."
          value={decisionComment}
          onChange={(e) => setDecisionComment(e.target.value)}
        />
      </Modal>
    </div>
  );
}
