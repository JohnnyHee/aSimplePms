/**
 * 角色与权限接口（/api/roles）
 */
import request, { compactParams, pathParam } from './http'
import type {
  PageResult,
  PermissionCatalog,
  PermissionGroup,
  RoleCreatePayload,
  RoleQuery,
  RoleUpdatePayload,
  RoleView
} from './types'

/** GET /api/roles —— 分页查询角色 */
export function fetchRoles(params: RoleQuery = {}): Promise<PageResult<RoleView>> {
  return request.get<PageResult<RoleView>>('/roles', compactParams(params as Record<string, unknown>))
}

/** GET /api/roles/options —— 角色下拉选项（不分页） */
export function fetchRoleOptions(): Promise<RoleView[]> {
  return request.get<RoleView[]>('/roles/options')
}

/** GET /api/roles/{id} —— 角色详情 */
export function fetchRole(id: string): Promise<RoleView> {
  return request.get<RoleView>(`/roles/${pathParam(id)}`)
}

/** POST /api/roles —— 新增角色 */
export function createRole(payload: RoleCreatePayload): Promise<RoleView> {
  return request.post<RoleView>('/roles', payload)
}

/** PUT /api/roles/{id} —— 更新角色（内置角色改权限 → code 4002） */
export function updateRole(id: string, payload: RoleUpdatePayload): Promise<RoleView> {
  return request.put<RoleView>(`/roles/${pathParam(id)}`, payload)
}

/** DELETE /api/roles/{id} —— 删除角色（内置或已被人员引用 → code 4002） */
export function deleteRole(id: string): Promise<null> {
  return request.delete<null>(`/roles/${pathParam(id)}`)
}

/** GET /api/roles/permissions —— 全部权限码（按分组，供勾选） */
export function fetchPermissionCatalog(): Promise<PermissionGroup[]> {
  return request
    .get<PermissionCatalog>('/roles/permissions')
    .then((catalog) => catalog?.groups ?? [])
}
