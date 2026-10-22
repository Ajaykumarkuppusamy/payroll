import React, { useEffect, useState } from 'react';
import {
  Modal,
  Form,
  Input,
  InputNumber,
  Select,
  DatePicker,
  Button,
  message,
  Row,
  Col,
  Divider,
} from 'antd';
import {
  UserAddOutlined,
  BankOutlined,
  IdcardOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import {
  createEmployee,
  getDepartments,
  type Department,
  type EmploymentStatus,
} from '../../api/employee';
import useAuthStore from '../../store/useAuthStore';

interface AddEmployeeModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

interface FormValues {
  first_name: string;
  last_name: string;
  email: string;
  department_id: number;
  base_salary: number;
  employment_status: EmploymentStatus;
  hire_date: dayjs.Dayjs;
  bank_account_number?: string;
  bank_name?: string;
  bank_ifsc_code?: string;
  tax_id?: string;
}

const AddEmployeeModal: React.FC<AddEmployeeModalProps> = ({
  open,
  onClose,
  onSuccess,
}) => {
  const { activeCompanyId } = useAuthStore();
  const [form] = Form.useForm<FormValues>();
  const [submitting, setSubmitting] = useState(false);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [loadingDepts, setLoadingDepts] = useState(false);

  useEffect(() => {
    if (open && activeCompanyId) {
      setLoadingDepts(true);
      getDepartments(activeCompanyId)
        .then((depts) => setDepartments(depts))
        .catch(() => {
          message.error('Failed to load departments');
        })
        .finally(() => setLoadingDepts(false));

      form.resetFields();
      form.setFieldsValue({
        employment_status: 'ACTIVE',
        hire_date: dayjs(),
      });
    }
  }, [open, activeCompanyId, form]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (!activeCompanyId) return;

      setSubmitting(true);
      await createEmployee(activeCompanyId, {
        first_name: values.first_name,
        last_name: values.last_name,
        email: values.email,
        department_id: values.department_id,
        base_salary: values.base_salary,
        employment_status: values.employment_status,
        hire_date: values.hire_date.format('YYYY-MM-DD'),
        bank_account_number: values.bank_account_number,
        bank_name: values.bank_name,
        bank_ifsc_code: values.bank_ifsc_code,
        tax_id: values.tax_id,
      });

      message.success('Employee onboarded successfully with statutory salary structure!');
      form.resetFields();
      onSuccess();
      onClose();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      message.error(
        axiosErr.response?.data?.message || 'Failed to onboard employee',
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, color: '#f8fafc' }}>
          <UserAddOutlined style={{ color: '#818cf8', fontSize: 20 }} />
          <span>Onboard New Employee</span>
        </div>
      }
      open={open}
      onCancel={onClose}
      width={680}
      footer={[
        <Button key="cancel" onClick={onClose} disabled={submitting}>
          Cancel
        </Button>,
        <Button
          key="submit"
          type="primary"
          loading={submitting}
          onClick={handleSubmit}
          style={{
            background: 'linear-gradient(135deg, #4f46e5, #7c3aed)',
            border: 'none',
          }}
        >
          Onboard Employee
        </Button>,
      ]}
      styles={{
        body: {
          background: '#0f172a',
          paddingTop: 8,
        },
        header: {
          background: '#0f172a',
          borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
          paddingBottom: 14,
        },
      }}
    >
      <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
        {/* Basic Info */}
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="first_name"
              label={<span style={{ color: '#cbd5e1' }}>First Name</span>}
              rules={[{ required: true, message: 'First name is required' }]}
            >
              <Input placeholder="e.g. Rahul" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="last_name"
              label={<span style={{ color: '#cbd5e1' }}>Last Name</span>}
              rules={[{ required: true, message: 'Last name is required' }]}
            >
              <Input placeholder="e.g. Sharma" />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="email"
              label={<span style={{ color: '#cbd5e1' }}>Corporate Email</span>}
              rules={[
                { required: true, message: 'Email is required' },
                { type: 'email', message: 'Enter a valid email address' },
              ]}
            >
              <Input placeholder="rahul.sharma@acme.com" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="department_id"
              label={<span style={{ color: '#cbd5e1' }}>Department</span>}
              rules={[{ required: true, message: 'Select a department' }]}
            >
              <Select
                placeholder="Select department"
                loading={loadingDepts}
                options={departments.map((d) => ({
                  value: d.id,
                  label: `${d.name} (${d.code})`,
                }))}
              />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={8}>
            <Form.Item
              name="base_salary"
              label={<span style={{ color: '#cbd5e1' }}>Monthly Base Salary (₹)</span>}
              rules={[{ required: true, message: 'Base salary is required' }]}
            >
              <InputNumber<number>
                style={{ width: '100%' }}
                placeholder="e.g. 85000"
                min={1}
                formatter={(val) => `₹ ${val}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                parser={(val) => (val ? Number(val.replace(/[₹\s,]/g, '')) : 0)}
              />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item
              name="employment_status"
              label={<span style={{ color: '#cbd5e1' }}>Status</span>}
              rules={[{ required: true }]}
            >
              <Select
                options={[
                  { value: 'ACTIVE', label: 'Active' },
                  { value: 'PROBATION', label: 'Probation' },
                  { value: 'ON_LEAVE', label: 'On Leave' },
                ]}
              />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item
              name="hire_date"
              label={<span style={{ color: '#cbd5e1' }}>Hire Date</span>}
              rules={[{ required: true, message: 'Hire date is required' }]}
            >
              <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" />
            </Form.Item>
          </Col>
        </Row>

        <Divider
          style={{
            borderColor: 'rgba(255, 255, 255, 0.08)',
            margin: '12px 0 16px',
            color: '#94a3b8',
            fontSize: 12,
          }}
        >
          <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <BankOutlined /> Banking & Statutory Details
          </span>
        </Divider>

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="bank_name"
              label={<span style={{ color: '#cbd5e1' }}>Bank Name</span>}
            >
              <Input placeholder="e.g. HDFC Bank" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="bank_account_number"
              label={<span style={{ color: '#cbd5e1' }}>Account Number</span>}
            >
              <Input placeholder="e.g. 5010023456789" />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="bank_ifsc_code"
              label={<span style={{ color: '#cbd5e1' }}>IFSC Code</span>}
            >
              <Input placeholder="e.g. HDFC0001234" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="tax_id"
              label={<span style={{ color: '#cbd5e1' }}>PAN / Tax ID</span>}
            >
              <Input
                prefix={<IdcardOutlined style={{ color: '#64748b' }} />}
                placeholder="e.g. ABCDE1234F"
              />
            </Form.Item>
          </Col>
        </Row>
      </Form>
    </Modal>
  );
};

export default AddEmployeeModal;
