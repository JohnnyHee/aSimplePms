/**
 * 薪资管理接口（/api/salaries）
 */
import request, { compactParams, pathParam } from './http'
import type { PageResult, SalaryPayload, SalaryQuery, SalaryView } from './types'

/** GET /api/salaries —— 分页查询薪资记录 */
export function fetchSalaries(params: SalaryQuery = {}): Promise<PageResult<SalaryView>> {
  return request.get<PageResult<SalaryView>>(
    '/salaries',
    compactParams(params as Record<string, unknown>)
  )
}

/** GET /api/salaries/history/{uid} —— 某人的调薪历史（按生效日期倒序，含 ACTIVE 与已归档记录） */
export function fetchSalaryHistory(uid: string): Promise<SalaryView[]> {
  return request.get<SalaryView[]>(`/salaries/history/${pathParam(uid)}`)
}

/** POST /api/salaries —— 新增薪资记录 */
export function createSalary(payload: SalaryPayload): Promise<SalaryView> {
  return request.post<SalaryView>('/salaries', payload)
}

/** PUT /api/salaries/{id} —— 修改薪资记录 */
export function updateSalary(id: string, payload: SalaryPayload): Promise<SalaryView> {
  return request.put<SalaryView>(`/salaries/${pathParam(id)}`, payload)
}

/** DELETE /api/salaries/{id} —— 删除薪资记录 */
export function deleteSalary(id: string): Promise<null> {
  return request.delete<null>(`/salaries/${pathParam(id)}`)
}
