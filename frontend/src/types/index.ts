export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
  path: string;
  timestamp: string;
  validationErrors?: Record<string, string>;
}

export type Role = 'ADMIN' | 'HR' | 'MANAGER' | 'EMPLOYEE';

export interface AuthUser {
  userId: number;
  employeeId: number;
  username: string;
  roles: Role[];
  employeeCode: string;
  fullName: string;
}
