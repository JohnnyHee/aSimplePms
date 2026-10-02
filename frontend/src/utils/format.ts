/**
 * 展示层格式化工具（时间、金额、枚举文案与标签色）。
 * 后端多数视图对象已经带 `*Label`，这里的映射只是兜底。
 */
import type { AuditOutcome, PayType, SalaryStatus, UserStatus } from '@/api/types'

/** 把后端日期（ISO-8601 / yyyy-MM-dd HH:mm:ss / 时间戳）解析为 Date；非法值返回 null */
export function toDate(value?: string | number | Date | null): Date | null {
  if (value === undefined || value === null || value === '') return null
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
  if (typeof value === 'number') {
    const fromNumber = new Date(value)
    return Number.isNaN(fromNumber.getTime()) ? null : fromNumber
  }
  // Safari / 部分环境不接受 'yyyy-MM-dd HH:mm:ss'，统一换成 ISO
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

function pad(value: number): string {
  return value < 10 ? `0${value}` : String(value)
}

/** 格式化为 yyyy-MM-dd HH:mm:ss */
export function formatDateTime(value?: string | number | Date | null, fallback = '-'): string {
  const date = toDate(value)
  if (!date) return fallback
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  )
}

/** 格式化为 yyyy-MM-dd */
export function formatDate(value?: string | number | Date | null, fallback = '-'): string {
  const date = toDate(value)
  if (!date) return fallback
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/** 提交给后端的开始时间（当天 00:00:00） */
export function toStartOfDay(value?: string | number | Date | null): string {
  const date = toDate(value)
  if (!date) return ''
  date.setHours(0, 0, 0, 0)
  return formatDateTime(date, '')
}

/** 提交给后端的结束时间（当天 23:59:59） */
export function toEndOfDay(value?: string | number | Date | null): string {
  const date = toDate(value)
  if (!date) return ''
  date.setHours(23, 59, 59, 0)
  return formatDateTime(date, '')
}

/** 千分位数字，如 12,345.67 */
export function formatNumber(value?: number | null, digits = 2, fallback = '-'): string {
  if (value === undefined || value === null || Number.isNaN(value)) return fallback
  return value.toLocaleString('zh-CN', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  })
}

/** 金额（带 ¥ 前缀） */
export function formatAmount(value?: number | null, digits = 2, fallback = '-'): string {
  if (value === undefined || value === null || Number.isNaN(value)) return fallback
  return `¥${formatNumber(value, digits, fallback)}`
}

/** 整数（不带小数） */
export function formatInteger(value?: number | null, fallback = '0'): string {
  if (value === undefined || value === null || Number.isNaN(value)) return fallback
  return value.toLocaleString('zh-CN')
}

/** 百分比数值（0~100，保留 digits 位小数） */
export function formatPercent(value: number, total: number, digits = 0): string {
  if (!total) return '0%'
  return `${((value / total) * 100).toFixed(digits)}%`
}

/** 进度条宽度百分比（0~100，限制在 100 以内） */
export function toBarPercent(value: number, max: number): number {
  if (!max || max <= 0) return 0
  return Math.min(100, Math.round((value / max) * 100))
}

/** 超长文本截断 */
export function truncate(value?: string | null, max = 40, suffix = '…'): string {
  if (!value) return '-'
  return value.length > max ? `${value.slice(0, max)}${suffix}` : value
}

/* ------------------------------------------------------------------ */
/* 枚举文案与标签色                                                    */
/* ------------------------------------------------------------------ */

const USER_STATUS_LABELS: Record<UserStatus, string> = {
  ENABLED: '正常',
  DISABLED: '已禁用'
}

export function userStatusLabel(status?: UserStatus, fallback = '-'): string {
  if (!status) return fallback
  return USER_STATUS_LABELS[status] ?? String(status)
}

export function userStatusTagType(status?: UserStatus): 'success' | 'danger' | 'info' {
  if (status === 'ENABLED') return 'success'
  if (status === 'DISABLED') return 'danger'
  return 'info'
}

const PAY_TYPE_LABELS: Record<PayType, string> = {
  MONTHLY: '基本月薪',
  BONUS: '绩效奖金',
  ALLOWANCE: '津贴补助',
  DEDUCTION: '扣款'
}

export function payTypeLabel(payType?: PayType, fallback = '-'): string {
  if (!payType) return fallback
  return PAY_TYPE_LABELS[payType] ?? String(payType)
}

/** 供 el-select 使用的薪资类型选项 */
export const PAY_TYPE_OPTIONS: Array<{ label: string; value: PayType }> = (
  Object.keys(PAY_TYPE_LABELS) as PayType[]
).map((value) => ({ label: PAY_TYPE_LABELS[value], value }))

export function payTypeTagType(payType?: PayType): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  switch (payType) {
    case 'MONTHLY':
      return 'primary'
    case 'BONUS':
      return 'success'
    case 'ALLOWANCE':
      return 'warning'
    case 'DEDUCTION':
      return 'danger'
    default:
      return 'info'
  }
}

const SALARY_STATUS_LABELS: Record<SalaryStatus, string> = {
  ACTIVE: '生效中',
  CLOSED: '已归档'
}

export function salaryStatusLabel(status?: SalaryStatus, fallback = '-'): string {
  if (!status) return fallback
  return SALARY_STATUS_LABELS[status] ?? String(status)
}

export function salaryStatusTagType(status?: SalaryStatus): 'success' | 'info' {
  return status === 'ACTIVE' ? 'success' : 'info'
}

/** 供 el-select 使用的状态选项 */
export const SALARY_STATUS_OPTIONS: Array<{ label: string; value: SalaryStatus }> = (
  Object.keys(SALARY_STATUS_LABELS) as SalaryStatus[]
).map((value) => ({ label: SALARY_STATUS_LABELS[value], value }))

export const USER_STATUS_OPTIONS: Array<{ label: string; value: UserStatus }> = (
  Object.keys(USER_STATUS_LABELS) as UserStatus[]
).map((value) => ({ label: USER_STATUS_LABELS[value], value }))

const AUDIT_OUTCOME_LABELS: Record<AuditOutcome, string> = {
  SUCCESS: '成功',
  FAILURE: '失败'
}

export function auditOutcomeLabel(outcome?: string, fallback = '-'): string {
  if (!outcome) return fallback
  return AUDIT_OUTCOME_LABELS[outcome as AuditOutcome] ?? outcome
}

export function auditOutcomeTagType(outcome?: string): 'success' | 'danger' {
  return outcome === 'SUCCESS' ? 'success' : 'danger'
}

/** 按角色 code 取中文名（内置角色兜底） */
const ROLE_CODE_LABELS: Record<string, string> = {
  ADMIN: '系统管理员',
  MANAGER: '部门主管',
  USER: '普通员工'
}

export function roleCodeLabel(code?: string, fallback = '-'): string {
  if (!code) return fallback
  return ROLE_CODE_LABELS[code.toUpperCase()] ?? code
}

/** 姓名首字母（用于无头像时的 el-avatar） */
export function nameInitial(name?: string): string {
  if (!name) return '?'
  return name.trim().charAt(0).toUpperCase()
}
