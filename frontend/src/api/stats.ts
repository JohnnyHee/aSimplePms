/**
 * 统计接口（/api/stats）—— 仪表盘用
 */
import request from './http'
import type { AuditLogView, NameValue, StatsOverview } from './types'

/** GET /api/stats/overview —— 总览数字 */
export function fetchStatsOverview(): Promise<StatsOverview> {
  return request.get<StatsOverview>('/stats/overview')
}

/** GET /api/stats/users-by-department —— 部门人数分布 */
export function fetchUsersByDepartment(): Promise<NameValue[]> {
  return request.get<NameValue[]>('/stats/users-by-department')
}

/** GET /api/stats/pay-type-distribution —— 薪资类型分布 */
export function fetchPayTypeDistribution(): Promise<NameValue[]> {
  return request.get<NameValue[]>('/stats/pay-type-distribution')
}

/** GET /api/stats/recent-audit-logs —— 最近操作日志 */
export function fetchRecentAuditLogs(limit = 10): Promise<AuditLogView[]> {
  return request.get<AuditLogView[]>('/stats/recent-audit-logs', { limit })
}
