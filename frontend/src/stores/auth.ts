/**
 * 登录态 store（Pinia setup 风格）。
 *
 * state：accessToken / refreshToken / user / permissions / roles / menus
 * token 持久化在 localStorage（pms_access_token / pms_refresh_token）
 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import * as userApi from '@/api/user'
import { tokenStorage } from '@/api/http'
import type {
  ChangePasswordPayload,
  CurrentUserView,
  LoginPayload,
  LoginResult,
  MenuItem,
  UpdateProfilePayload,
  UserStatus,
  UserView
} from '@/api/types'

/** 视为管理员的角色标识（后端内置角色 code = ADMIN） */
const ADMIN_ROLE_ALIASES = ['ADMIN', 'ROLE_ADMIN', '系统管理员']

/** 把 /auth/me 概要信息转成完整的 UserView 形状（缺失字段留空） */
function toSummary(me: CurrentUserView): UserView {
  return {
    uid: me.uid,
    username: me.username,
    name: me.displayName || me.username,
    status: 'ENABLED' as UserStatus,
    roleIds: [],
    roleNames: [...(me.roles ?? [])]
  }
}

export const useAuthStore = defineStore('auth', () => {
  /* ---------------- state ---------------- */
  const accessToken = ref<string | null>(tokenStorage.getAccessToken())
  const refreshToken = ref<string | null>(tokenStorage.getRefreshToken())
  const user = ref<UserView | null>(null)
  const permissions = ref<string[]>([])
  const roles = ref<string[]>([])
  const menus = ref<MenuItem[]>([])
  /** 是否已经拉取过 /auth/me 与 /auth/permissions */
  const profileLoaded = ref(false)

  /* ---------------- getters ---------------- */
  const isLoggedIn = computed(() => Boolean(accessToken.value))
  const displayName = computed(
    () => user.value?.name || user.value?.nickname || user.value?.username || '未登录'
  )
  const avatar = computed(() => user.value?.avatar || '')
  const isAdmin = computed(() =>
    roles.value.some((role) => ADMIN_ROLE_ALIASES.includes(role.trim().toUpperCase()))
  )

  /* ---------------- actions ---------------- */

  /** 写入登录态（登录 / 刷新令牌成功后调用） */
  function setSession(result: LoginResult): void {
    accessToken.value = result.accessToken
    refreshToken.value = result.refreshToken
    tokenStorage.set(result.accessToken, result.refreshToken)
    if (result.user) {
      user.value = result.user
    }
  }

  /** 清空登录态（退出登录 / 令牌失效时调用） */
  function clearSession(): void {
    accessToken.value = null
    refreshToken.value = null
    user.value = null
    permissions.value = []
    roles.value = []
    menus.value = []
    profileLoaded.value = false
    tokenStorage.clear()
  }

  /** 登录：拿令牌 + 拉取用户信息与权限 */
  async function login(payload: LoginPayload): Promise<LoginResult> {
    const result = await authApi.login(payload)
    setSession(result)
    await fetchProfile()
    return result
  }

  /**
   * 拉取当前用户资料、权限码、角色与菜单。
   *
   * 后端 `GET /api/auth/me` 返回的是概要视图（uid/username/displayName/roles/permissions），
   * 不含姓名、头像等明细字段；因此这里用 `GET /api/users/{uid}` 补齐。
   * 明细接口需要 `user:view` 权限，无权限的用户降级为概要信息，不阻塞登录。
   * 失败时抛出 ApiError（由调用方或路由守卫处理）。
   */
  async function fetchProfile(): Promise<UserView | null> {
    if (!accessToken.value) return null
    const [me, permissionPayload] = await Promise.all([
      authApi.fetchCurrentUser(),
      authApi.fetchPermissions()
    ])
    permissions.value = permissionPayload?.permissions ?? []
    roles.value = permissionPayload?.roles ?? []
    menus.value = permissionPayload?.menus ?? []
    user.value = toSummary(me)
    try {
      user.value = await userApi.fetchUser(me.uid)
    } catch {
      /* 无人员查看权限时保留概要信息 */
    }
    profileLoaded.value = true
    return user.value
  }

  /** 退出登录：通知后端注销令牌（失败也照常清理本地状态） */
  async function logout(): Promise<void> {
    try {
      await authApi.logout()
    } catch {
      /* 后端不可用 / 令牌已失效时忽略，本地状态一定要清干净 */
    } finally {
      clearSession()
    }
  }

  /**
   * 是否拥有权限码。
   * - 不传参数 → true
   * - 拥有 `*` / `*:*` → true（超级管理员通配）
   * - 数组时命中任意一个即 true
   */
  function hasPermission(code?: string | string[]): boolean {
    if (!code) return true
    const codes = Array.isArray(code) ? code : [code]
    if (codes.length === 0) return true
    const owned = permissions.value
    if (owned.includes('*') || owned.includes('*:*')) return true
    return codes.some((item) => owned.includes(item))
  }

  /** 是否拥有角色（匹配角色 code 或名称，忽略大小写） */
  function hasRole(code?: string | string[]): boolean {
    if (!code) return true
    const codes = Array.isArray(code) ? code : [code]
    if (codes.length === 0) return true
    const owned = roles.value.map((role) => role.trim().toUpperCase())
    return codes.some((item) => owned.includes(item.trim().toUpperCase()))
  }

  /** 修改自己的资料 */
  async function updateProfile(payload: UpdateProfilePayload): Promise<UserView> {
    const updated = await authApi.updateProfile(payload)
    user.value = updated
    return updated
  }

  /** 修改自己的密码 */
  async function changePassword(payload: ChangePasswordPayload): Promise<void> {
    await authApi.changePassword(payload)
  }

  return {
    // state
    accessToken,
    refreshToken,
    user,
    permissions,
    roles,
    menus,
    profileLoaded,
    // getters
    isLoggedIn,
    displayName,
    avatar,
    isAdmin,
    // actions
    setSession,
    clearSession,
    login,
    logout,
    fetchProfile,
    hasPermission,
    hasRole,
    updateProfile,
    changePassword
  }
})
