import dayjs from 'dayjs';
import { DATE_FORMAT, DATETIME_FORMAT, MONTH_FORMAT } from '../constants';

/** Convert paise (integer) to rupee string: 4500000 -> "₹45,000.00" */
export function formatCurrency(paise: number): string {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
  }).format(paise / 100);
}

export const formatDate = (date: string | null | undefined): string =>
  date ? dayjs(date).format(DATE_FORMAT) : '—';

export const formatDateTime = (date: string | null | undefined): string =>
  date ? dayjs(date).format(DATETIME_FORMAT) : '—';

export const formatMonth = (date: string | null | undefined): string =>
  date ? dayjs(date).format(MONTH_FORMAT) : '—';
