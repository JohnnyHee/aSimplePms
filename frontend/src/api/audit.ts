/**
 * 审计日志接口（/api/audit-logs）
 */
import request, { compactParams } from './http'
import type { AuditLogView, AuditQuery, PageResult } from './types'

/** GET /api/audit-logs —— 分页查询审计日志 */
export function fetchAuditLogs(params: AuditQuery = {}): Promise<PageResult<AuditLogView>> {
  return request.get<PageResult<AuditLogView>>(
    '/audit-logs',
    compactParams(params as Record<string, unknown>)
  )
}

/** DELETE /api/audit-logs —— 清空审计日志（可按条件清理） */
export function clearAuditLogs(params: AuditQuery = {}): Promise<null> {
  return request.delete<null>(
    '/audit-logs',
    compactParams(params as Record<string, unknown>)
  )
}
