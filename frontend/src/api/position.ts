/**
 * 职位管理接口（/api/positions）
 */
import request, { compactParams, pathParam } from './http'
import type { PageResult, PositionPayload, PositionQuery, PositionView } from './types'

/** GET /api/positions —— 分页查询职位 */
export function fetchPositions(params: PositionQuery = {}): Promise<PageResult<PositionView>> {
  return request.get<PageResult<PositionView>>(
    '/positions',
    compactParams(params as Record<string, unknown>)
  )
}

/** GET /api/positions/options —— 职位下拉选项（不分页） */
export function fetchPositionOptions(): Promise<PositionView[]> {
  return request.get<PositionView[]>('/positions/options')
}

/** GET /api/positions/{id} —— 职位详情 */
export function fetchPosition(id: string): Promise<PositionView> {
  return request.get<PositionView>(`/positions/${pathParam(id)}`)
}

/** POST /api/positions —— 新增职位 */
export function createPosition(payload: PositionPayload): Promise<PositionView> {
  return request.post<PositionView>('/positions', payload)
}

/** PUT /api/positions/{id} —— 更新职位 */
export function updatePosition(id: string, payload: PositionPayload): Promise<PositionView> {
  return request.put<PositionView>(`/positions/${pathParam(id)}`, payload)
}

/** DELETE /api/positions/{id} —— 删除职位（被人员引用时后端返回 4002） */
export function deletePosition(id: string): Promise<null> {
  return request.delete<null>(`/positions/${pathParam(id)}`)
}
