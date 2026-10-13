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
  Select,
  Radio,
  message,
  Statistic,
  Progress,
  Badge,
  Popconfirm,
} from 'antd';
import {
  CalendarOutlined,
  PlusOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { leaveApi, type LeaveType, type LeaveBalance, type LeaveRequestItem } from '../../api/leave';
import { fetchDepartments, type Department } from '../../api/org';
import { useAuth } from '../../auth/AuthContext';

const { Title, Text } = Typography;

export default function LeavePage() {
  const { user } = useAuth();
  const isAdminOrHR = user?.roles?.some((r: string) => r === 'ADMIN' || r === 'HR' || r === 'MANAGER');

  // State
  const [types, setTypes] = useState<LeaveType[]>([]);
  const [balances, setBalances] = useState<LeaveBalance[]>([]);
  const [myRequests, setMyRequests] = useState<LeaveRequestItem[]>([]);
  const [allRequests, setAllRequests] = useState<LeaveRequestItem[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);

  const [, setLoadingBalances] = useState(false);
  const [loadingMyRequests, setLoadingMyRequests] = useState(false);
  const [loadingAllRequests, setLoadingAllRequests] = useState(false);

  // Filters for Org Requests
  const [filterStatus, setFilterStatus] = useState<string | undefined>('PENDING');
  const [filterDept, setFilterDept] = useState<number | undefined>();

  // Modals
  const [applyModalVisible, setApplyModalVisible] = useState(false);
  const [applyForm] = Form.useForm();
  const [submittingApply, setSubmittingApply] = useState(false);
  const [calculatedDays, setCalculatedDays] = useState<number>(1);

  // Decision Modal
  const [decisionModalVisible, setDecisionModalVisible] = useState(false);
  const [activeReqItem, setActiveReqItem] = useState<LeaveRequestItem | null>(null);
  const [decisionAction, setDecisionAction] = useState<'APPROVE' | 'REJECT'>('APPROVE');
  const [decisionComment, setDecisionComment] = useState('');
  const [submittingDecision, setSubmittingDecision] = useState(false);

  const loadLeaveTypes = async () => {
    try {
      const data = await leaveApi.getTypes();
      setTypes(data);
    } catch {
      // ignore
    }
  };

  const loadBalances = async () => {
    try {
      setLoadingBalances(true);
      const data = await leaveApi.getMyBalances();
      setBalances(data);
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Failed to load balances');
    } finally {
      setLoadingBalances(false);
    }
  };

  const loadMyRequests = async () => {
    try {
      setLoadingMyRequests(true);
      const data = await leaveApi.getMyRequests();
      setMyRequests(data);
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Failed to load requests');
    } finally {
      setLoadingMyRequests(false);
    }
  };

  const loadAllRequests = async () => {
    if (!isAdminOrHR) return;
    try {
      setLoadingAllRequests(true);
      const res = await leaveApi.getRequests({
        status: filterStatus,
        departmentId: filterDept,
        page: 0,
        size: 50,
      });
      setAllRequests(res.content);
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Failed to load team requests');
    } finally {
      setLoadingAllRequests(false);
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
    loadLeaveTypes();
    loadBalances();
    loadMyRequests();
    loadDepartments();
  }, []);

  useEffect(() => {
    if (isAdminOrHR) {
      loadAllRequests();
    }
  }, [filterStatus, filterDept]);

  // Dynamic estimate of working days (Mon-Fri, Sat/Sun off)
  const calculateDaysEstimate = (dates: [dayjs.Dayjs, dayjs.Dayjs] | undefined, startPart: string, endPart: string) => {
    if (!dates || !dates[0] || !dates[1]) {
      setCalculatedDays(0);
      return;
    }
    let start = dates[0];
    const end = dates[1];
    let count = 0;

    let curr = start;
    while (!curr.isAfter(end, 'day')) {
      const day = curr.day(); // 0 is Sun, 6 is Sat
      if (day !== 0 && day !== 6) {
        if (curr.isSame(start, 'day') && curr.isSame(end, 'day')) {
          if (startPart !== 'FULL_DAY' || endPart !== 'FULL_DAY') {
            count += 0.5;
          } else {
            count += 1.0;
          }
        } else if (curr.isSame(start, 'day') && startPart !== 'FULL_DAY') {
          count += 0.5;
        } else if (curr.isSame(end, 'day') && endPart !== 'FULL_DAY') {
          count += 0.5;
        } else {
          count += 1.0;
        }
      }
      curr = curr.add(1, 'day');
    }
    setCalculatedDays(count);
  };

  const handleApplySubmit = async (values: any) => {
    try {
      setSubmittingApply(true);
      const [start, end] = values.dateRange;

      await leaveApi.apply({
        leaveTypeId: values.leaveTypeId,
        startDate: start.format('YYYY-MM-DD'),
        endDate: end.format('YYYY-MM-DD'),
        startDayPart: values.startDayPart,
        endDayPart: values.endDayPart,
        reason: values.reason,
      });

      message.success('Leave application submitted successfully');
      setApplyModalVisible(false);
      applyForm.resetFields();
      loadBalances();
      loadMyRequests();
      if (isAdminOrHR) loadAllRequests();
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Failed to submit leave application');
    } finally {
      setSubmittingApply(false);
    }
  };

  const handleCancelLeave = async (id: number) => {
    try {
      await leaveApi.cancel(id);
      message.success('Leave request cancelled');
      loadBalances();
      loadMyRequests();
      if (isAdminOrHR) loadAllRequests();
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Failed to cancel leave');
    }
  };

  const handleDecisionSubmit = async () => {
    if (!activeReqItem) return;
    try {
      setSubmittingDecision(true);
      if (decisionAction === 'APPROVE') {
        await leaveApi.approve(activeReqItem.id, decisionComment);
        message.success('Leave approved successfully');
      } else {
        await leaveApi.reject(activeReqItem.id, decisionComment);
        message.info('Leave request rejected');
      }
      setDecisionModalVisible(false);
      setDecisionComment('');
      loadBalances();
      loadMyRequests();
      loadAllRequests();
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Action failed');
    } finally {
      setSubmittingDecision(false);
    }
  };

  const renderStatusTag = (st: string) => {
    switch (st) {
      case 'APPROVED':
        return <Tag color="success">Approved</Tag>;
      case 'PENDING':
        return <Tag color="warning">Pending Approval</Tag>;
      case 'REJECTED':
        return <Tag color="error">Rejected</Tag>;
      case 'CANCELLED':
        return <Tag color="default">Cancelled</Tag>;
      default:
        return <Tag>{st}</Tag>;
    }
  };

  const myColumns = [
    {
      title: 'Leave Type',
      key: 'type',
      render: (_: any, r: LeaveRequestItem) => (
        <div>
          <Text strong style={{ color: '#f8fafc' }}>{r.leaveTypeName}</Text>
          <br />
          <Tag color="geekblue" style={{ fontSize: 11 }}>{r.leaveTypeCode}</Tag>
        </div>
      ),
    },
    {
      title: 'Duration',
      key: 'dates',
      render: (_: any, r: LeaveRequestItem) => (
        <div>
          <Text style={{ color: '#f8fafc' }}>
            {dayjs(r.startDate).format('DD MMM YYYY')} – {dayjs(r.endDate).format('DD MMM YYYY')}
          </Text>
          <br />
          <Text type="secondary" style={{ fontSize: 12 }}>
            {r.totalDays} working {r.totalDays === 1 ? 'day' : 'days'}
          </Text>
        </div>
      ),
    },
    {
      title: 'Reason',
      dataIndex: 'reason',
      key: 'reason',
      ellipsis: true,
      render: (t: string) => t || '—',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (st: string) => renderStatusTag(st),
    },
    {
      title: 'Approver / Remarks',
      key: 'approver',
      render: (_: any, r: LeaveRequestItem) => (
        <div>
          {r.approverName && <Text type="secondary" style={{ fontSize: 12 }}>{r.approverName}</Text>}
          {r.decisionComment && (
            <div>
              <Text type="secondary" style={{ fontSize: 11, fontStyle: 'italic' }}>"{r.decisionComment}"</Text>
            </div>
          )}
          {!r.approverName && !r.decisionComment && '—'}
        </div>
      ),
    },
    {
      title: 'Action',
      key: 'action',
      render: (_: any, r: LeaveRequestItem) => {
        if (r.status === 'PENDING') {
          return (
            <Popconfirm
              title="Cancel this leave request?"
              description="Your pending quota balance will be restored immediately."
              onConfirm={() => handleCancelLeave(r.id)}
              okText="Yes, Cancel"
              cancelText="No"
            >
              <Button danger size="small">
                Cancel
              </Button>
            </Popconfirm>
          );
        }
        return '—';
      },
    },
  ];

  const teamColumns = [
    {
      title: 'Employee',
      key: 'emp',
      render: (_: any, r: LeaveRequestItem) => (
        <div>
          <Text strong style={{ color: '#f8fafc' }}>{r.employeeName}</Text>
          <br />
          <Text type="secondary" style={{ fontSize: 12 }}>{r.employeeCode} · {r.departmentName || 'General'}</Text>
        </div>
      ),
    },
    {
      title: 'Leave Type',
      key: 'type',
      render: (_: any, r: LeaveRequestItem) => (
        <div>
          <Text strong style={{ color: '#f8fafc' }}>{r.leaveTypeName}</Text>
          <br />
          <Tag color="geekblue" style={{ fontSize: 11 }}>{r.leaveTypeCode}</Tag>
        </div>
      ),
    },
    {
      title: 'Duration',
      key: 'dates',
      render: (_: any, r: LeaveRequestItem) => (
        <div>
          <span>{dayjs(r.startDate).format('DD MMM YYYY')} – {dayjs(r.endDate).format('DD MMM YYYY')}</span>
          <br />
          <Text strong style={{ color: '#38bdf8' }}>{r.totalDays} working {r.totalDays === 1 ? 'day' : 'days'}</Text>
        </div>
      ),
    },
    {
      title: 'Reason',
      dataIndex: 'reason',
      key: 'reason',
      ellipsis: true,
      render: (t: string) => t || '—',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (st: string) => renderStatusTag(st),
    },
    {
      title: 'Action',
      key: 'action',
      render: (_: any, r: LeaveRequestItem) => {
        if (r.status !== 'PENDING') {
          return r.approverName ? (
            <Text type="secondary" style={{ fontSize: 12 }}>By {r.approverName}</Text>
          ) : '—';
        }
        return (
          <Space>
            <Button
              type="primary"
              size="small"
              style={{ background: '#10b981', borderColor: '#10b981' }}
              onClick={() => {
                setActiveReqItem(r);
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
                setActiveReqItem(r);
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
      {/* Top Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <Title level={3} style={{ color: '#f8fafc', margin: 0 }}>
            Leave Management
          </Title>
          <Text type="secondary">
            Annual leave quotas, dynamic working day deduction, and approval tracking.
          </Text>
        </div>

        <Button
          type="primary"
          size="large"
          icon={<PlusOutlined />}
          onClick={() => {
            applyForm.resetFields();
            setCalculatedDays(1);
            setApplyModalVisible(true);
          }}
          style={{
            background: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)',
            border: 'none',
            borderRadius: 10,
            fontWeight: 600,
            boxShadow: '0 4px 14px rgba(37,99,235,0.3)',
          }}
        >
          Apply for Leave
        </Button>
      </div>

      {/* Quota Balance Cards */}
      <Row gutter={[16, 16]}>
        {balances.map((b) => {
          const totalAllotted = Number(b.opening) + Number(b.accrued) + Number(b.adjusted);
          const percentUsed = totalAllotted > 0 ? Math.min(100, Math.round((Number(b.used) / totalAllotted) * 100)) : 0;
          const isLop = b.leaveTypeCode === 'LOP';

          return (
            <Col xs={24} sm={12} lg={6} key={b.id}>
              <Card
                variant="borderless"
                style={{
                  background: 'linear-gradient(135deg, rgba(30,41,59,0.8) 0%, rgba(15,23,42,0.9) 100%)',
                  border: '1px solid rgba(255,255,255,0.08)',
                  borderRadius: 14,
                  boxShadow: '0 4px 20px rgba(0,0,0,0.2)',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
                  <div>
                    <Text strong style={{ color: '#f8fafc', fontSize: 16 }}>
                      {b.leaveTypeName}
                    </Text>
                    <br />
                    <Tag color="cyan" style={{ fontSize: 11, marginTop: 4 }}>
                      {b.leaveTypeCode}
                    </Tag>
                  </div>
                  {!isLop && (
                    <Statistic
                      value={b.available}
                      suffix={<span style={{ fontSize: 13, color: '#94a3b8' }}>days left</span>}
                      valueStyle={{ color: '#38bdf8', fontWeight: 700 }}
                    />
                  )}
                </div>

                {!isLop ? (
                  <>
                    <Progress
                      percent={percentUsed}
                      strokeColor={{ '0%': '#38bdf8', '100%': '#3b82f6' }}
                      trailColor="rgba(255,255,255,0.1)"
                      showInfo={false}
                    />
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 8, fontSize: 12, color: '#94a3b8' }}>
                      <span>Used: <strong style={{ color: '#f8fafc' }}>{b.used}</strong></span>
                      {Number(b.pending) > 0 && (
                        <span>Pending: <strong style={{ color: '#f59e0b' }}>{b.pending}</strong></span>
                      )}
                      <span>Allotted: <strong style={{ color: '#f8fafc' }}>{totalAllotted}</strong></span>
                    </div>
                  </>
                ) : (
                  <div style={{ paddingTop: 8 }}>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      Loss of Pay taken this year:
                    </Text>
                    <div style={{ fontSize: 24, fontWeight: 700, color: '#ef4444' }}>
                      {b.used} days
                    </div>
                  </div>
                )}
              </Card>
            </Col>
          );
        })}
      </Row>

      {/* Tabs */}
      <Card
        variant="borderless"
        style={{
          background: 'rgba(30,41,59,0.7)',
          border: '1px solid rgba(255,255,255,0.08)',
          borderRadius: 16,
        }}
      >
        <Tabs
          defaultActiveKey="my-requests"
          tabBarStyle={{ borderBottom: '1px solid rgba(255,255,255,0.08)' }}
          items={[
            {
              key: 'my-requests',
              label: (
                <Space>
                  <CalendarOutlined />
                  <span>My Leave History</span>
                </Space>
              ),
              children: (
                <div>
                  <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 16 }}>
                    <Button icon={<ReloadOutlined />} onClick={loadMyRequests} loading={loadingMyRequests}>
                      Refresh
                    </Button>
                  </div>
                  <Table
                    dataSource={myRequests}
                    columns={myColumns}
                    rowKey="id"
                    loading={loadingMyRequests}
                    pagination={{ pageSize: 10 }}
                  />
                </div>
              ),
            },
            ...(isAdminOrHR
              ? [
                  {
                    key: 'team-requests',
                    label: (
                      <Space>
                        <TeamOutlined />
                        <span>Team Approvals</span>
                        {allRequests.filter(r => r.status === 'PENDING').length > 0 && (
                          <Badge
                            count={allRequests.filter(r => r.status === 'PENDING').length}
                            style={{ backgroundColor: '#f59e0b' }}
                          />
                        )}
                      </Space>
                    ),
                    children: (
                      <div>
                        <div style={{ display: 'flex', gap: 16, marginBottom: 16, flexWrap: 'wrap', alignItems: 'center' }}>
                          <Select
                            placeholder="Filter Status"
                            style={{ width: 180 }}
                            value={filterStatus}
                            onChange={setFilterStatus}
                            options={[
                              { label: 'Pending Approvals', value: 'PENDING' },
                              { label: 'Approved', value: 'APPROVED' },
                              { label: 'Rejected', value: 'REJECTED' },
                              { label: 'All Requests', value: undefined },
                            ]}
                          />

                          <Select
                            placeholder="Filter Department"
                            allowClear
                            style={{ width: 180 }}
                            value={filterDept}
                            onChange={setFilterDept}
                            options={departments.map(d => ({ label: d.name, value: d.id }))}
                          />

                          <Button icon={<ReloadOutlined />} onClick={loadAllRequests} loading={loadingAllRequests}>
                            Refresh
                          </Button>
                        </div>

                        <Table
                          dataSource={allRequests}
                          columns={teamColumns}
                          rowKey="id"
                          loading={loadingAllRequests}
                          pagination={{ pageSize: 15 }}
                        />
                      </div>
                    ),
                  },
                ]
              : []),
            {
              key: 'policy',
              label: (
                <Space>
                  <SafetyCertificateOutlined />
                  <span>Company Leave Policy</span>
                </Space>
              ),
              children: (
                <Row gutter={[16, 16]}>
                  {types.map((t) => (
                    <Col xs={24} md={12} lg={8} key={t.id}>
                      <Card
                        variant="borderless"
                        style={{
                          background: 'rgba(15,23,42,0.6)',
                          border: '1px solid rgba(255,255,255,0.06)',
                          borderRadius: 12,
                        }}
                      >
                        <Title level={5} style={{ color: '#f8fafc', margin: 0 }}>
                          {t.name} ({t.code})
                        </Title>
                        <div style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 6, fontSize: 13, color: '#94a3b8' }}>
                          <div>Annual Quota: <strong style={{ color: '#f8fafc' }}>{t.annualQuota} days</strong></div>
                          <div>Accrual Type: <Tag color="blue">{t.accrualType}</Tag></div>
                          <div>Paid Leave: <Tag color={t.paid ? 'success' : 'warning'}>{t.paid ? 'Yes' : 'Loss of Pay'}</Tag></div>
                          <div>Half Day Permitted: <Tag>{t.allowHalfDay ? 'Allowed' : 'Full Day Only'}</Tag></div>
                          <div style={{ fontSize: 11, color: '#64748b', marginTop: 4 }}>
                            * Saturday and Sunday are default weekly off days and are excluded from deduction.
                          </div>
                        </div>
                      </Card>
                    </Col>
                  ))}
                </Row>
              ),
            },
          ]}
        />
      </Card>

      {/* Apply Leave Modal */}
      <Modal
        title="Apply for Leave"
        open={applyModalVisible}
        onCancel={() => setApplyModalVisible(false)}
        footer={null}
        destroyOnClose
        width={560}
      >
        <Form
          form={applyForm}
          layout="vertical"
          onFinish={handleApplySubmit}
          initialValues={{
            startDayPart: 'FULL_DAY',
            endDayPart: 'FULL_DAY',
          }}
          onValuesChange={(_, all) => {
            calculateDaysEstimate(all.dateRange, all.startDayPart, all.endDayPart);
          }}
        >
          <Form.Item
            name="leaveTypeId"
            label="Leave Type"
            rules={[{ required: true, message: 'Please select a leave type' }]}
          >
            <Select placeholder="Choose type of leave">
              {types.map((t) => {
                const bal = balances.find(b => b.leaveTypeId === t.id);
                const avail = bal ? bal.available : t.annualQuota;
                return (
                  <Select.Option key={t.id} value={t.id}>
                    {t.name} ({t.code}) — {t.code === 'LOP' ? 'Loss of Pay' : `${avail} days available`}
                  </Select.Option>
                );
              })}
            </Select>
          </Form.Item>

          <Form.Item
            name="dateRange"
            label="Leave Dates (Start – End)"
            rules={[{ required: true, message: 'Please select leave dates' }]}
          >
            <DatePicker.RangePicker
              style={{ width: '100%' }}
              disabledDate={(current) => current && current.isBefore(dayjs().subtract(30, 'day'))}
            />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="startDayPart" label="Start Date Session">
                <Radio.Group>
                  <Radio.Button value="FULL_DAY">Full Day</Radio.Button>
                  <Radio.Button value="FIRST_HALF">First Half</Radio.Button>
                  <Radio.Button value="SECOND_HALF">Second Half</Radio.Button>
                </Radio.Group>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="endDayPart" label="End Date Session">
                <Radio.Group>
                  <Radio.Button value="FULL_DAY">Full Day</Radio.Button>
                  <Radio.Button value="FIRST_HALF">First Half</Radio.Button>
                  <Radio.Button value="SECOND_HALF">Second Half</Radio.Button>
                </Radio.Group>
              </Form.Item>
            </Col>
          </Row>

          <div
            style={{
              padding: '12px 16px',
              borderRadius: 8,
              background: 'rgba(56,189,248,0.1)',
              border: '1px solid rgba(56,189,248,0.2)',
              marginBottom: 20,
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
            }}
          >
            <Text style={{ color: '#f8fafc' }}>
              Calculated working days (excluding Sat/Sun):
            </Text>
            <Text strong style={{ color: '#38bdf8', fontSize: 18 }}>
              {calculatedDays} {calculatedDays === 1 ? 'day' : 'days'}
            </Text>
          </div>

          <Form.Item
            name="reason"
            label="Reason for Leave"
            rules={[{ required: true, message: 'Please state the reason for your leave' }]}
          >
            <Input.TextArea rows={3} placeholder="Provide details for manager approval..." />
          </Form.Item>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 16 }}>
            <Button onClick={() => setApplyModalVisible(false)}>Cancel</Button>
            <Button
              type="primary"
              htmlType="submit"
              loading={submittingApply}
              disabled={calculatedDays <= 0}
            >
              Submit Application
            </Button>
          </div>
        </Form>
      </Modal>

      {/* Decision Modal */}
      <Modal
        title={`${decisionAction === 'APPROVE' ? 'Approve' : 'Reject'} Leave Request`}
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
              ? `Approve ${activeReqItem?.totalDays} days ${activeReqItem?.leaveTypeName} for ${activeReqItem?.employeeName}?`
              : `Reject leave request for ${activeReqItem?.employeeName}?`}
          </Text>
        </div>
        <Input.TextArea
          rows={3}
          placeholder="Optional remarks/comments for employee..."
          value={decisionComment}
          onChange={(e) => setDecisionComment(e.target.value)}
        />
      </Modal>
    </div>
  );
}
