/**
 * 认证相关接口（/api/auth）
 */
import request from './http'
import type {
  ChangePasswordPayload,
  CurrentUserView,
  LoginPayload,
  LoginResult,
  PermissionPayload,
  UpdateProfilePayload,
  UserView
} from './types'

/** POST /api/auth/login —— 登录 */
export function login(payload: LoginPayload): Promise<LoginResult> {
  return request.post<LoginResult>('/auth/login', payload)
}

/** POST /api/auth/refresh —— 用 refreshToken 换取新令牌 */
export function refresh(payload: { refreshToken: string }): Promise<LoginResult> {
  return request.post<LoginResult>('/auth/refresh', payload)
}

/** POST /api/auth/logout —— 退出登录（需认证） */
export function logout(): Promise<null> {
  return request.post<null>('/auth/logout')
}

/** GET /api/auth/me —— 当前登录用户（概要：uid/username/displayName/roles/permissions） */
export function fetchCurrentUser(): Promise<CurrentUserView> {
  return request.get<CurrentUserView>('/auth/me')
}

/** PUT /api/auth/password —— 修改自己的密码 */
export function changePassword(payload: ChangePasswordPayload): Promise<null> {
  return request.put<null>('/auth/password', payload)
}

/** PUT /api/auth/profile —— 修改自己的资料 */
export function updateProfile(payload: UpdateProfilePayload): Promise<UserView> {
  return request.put<UserView>('/auth/profile', payload)
}

/** GET /api/auth/permissions —— 当前用户的权限码 / 角色 / 菜单 */
export function fetchPermissions(): Promise<PermissionPayload> {
  return request.get<PermissionPayload>('/auth/permissions')
}
