/**
 * 人员管理接口（/api/users）
 */
import request, { compactParams, pathParam } from './http'
import type {
  PageResult,
  UserCreatePayload,
  UserQuery,
  UserStatus,
  UserUpdatePayload,
  UserView
} from './types'

/** GET /api/users —— 分页查询人员 */
export function fetchUsers(params: UserQuery = {}): Promise<PageResult<UserView>> {
  return request.get<PageResult<UserView>>('/users', compactParams(params as Record<string, unknown>))
}

/** GET /api/users/{uid} —— 人员详情 */
export function fetchUser(uid: string): Promise<UserView> {
  return request.get<UserView>(`/users/${pathParam(uid)}`)
}

/** POST /api/users —— 新增人员（用户名已存在 → code 4001） */
export function createUser(payload: UserCreatePayload): Promise<UserView> {
  return request.post<UserView>('/users', payload)
}

/** PUT /api/users/{uid} —— 更新人员（不含 username / password） */
export function updateUser(uid: string, payload: UserUpdatePayload): Promise<UserView> {
  return request.put<UserView>(`/users/${pathParam(uid)}`, payload)
}

/** DELETE /api/users/{uid} —— 删除人员（不允许删除自己 → code 4002） */
export function deleteUser(uid: string): Promise<null> {
  return request.delete<null>(`/users/${pathParam(uid)}`)
}

/** PATCH /api/users/{uid}/status —— 启用/禁用人员 */
export function updateUserStatus(uid: string, status: UserStatus): Promise<UserView> {
  return request.patch<UserView>(`/users/${pathParam(uid)}/status`, { status })
}

/** PUT /api/users/{uid}/password —— 管理员重置某人密码 */
export function resetUserPassword(uid: string, newPassword: string): Promise<null> {
  return request.put<null>(`/users/${pathParam(uid)}/password`, { newPassword })
}

/** GET /api/users/departments —— 全部部门（供筛选下拉，已去重排序） */
export function fetchDepartments(): Promise<string[]> {
  return request.get<string[]>('/users/departments')
}

/** GET /api/users/check-username?username=xxx —— 登录名是否可用（true 表示未被占用） */
export function checkUsername(username: string): Promise<boolean> {
  return request.get<boolean>('/users/check-username', compactParams({ username }))
}
