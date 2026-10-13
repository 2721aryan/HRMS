export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

export const ROLES = {
  ADMIN: 'ADMIN',
  HR: 'HR',
  MANAGER: 'MANAGER',
  EMPLOYEE: 'EMPLOYEE',
} as const;

export type Role = (typeof ROLES)[keyof typeof ROLES];

export const TOKEN_KEY = 'hrms_access_token';

export const DATE_FORMAT = 'DD MMM YYYY';
export const DATETIME_FORMAT = 'DD MMM YYYY, HH:mm';
export const MONTH_FORMAT = 'MMM YYYY';
