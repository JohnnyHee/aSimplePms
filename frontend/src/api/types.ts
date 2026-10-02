/**
 * 后端接口数据模型（与 Spring Boot 3 后端 1:1 对齐）。
 *
 * 统一响应信封：{ code: 0, message: '成功', data: ..., timestamp: '...' }
 * 分页信封（data）：{ records, total, page, size }
 */

/** 统一响应信封 */
export interface ApiEnvelope<T = unknown> {
  /** 业务码，0 表示成功 */
  code: number
  /** 提示信息 */
  message: string
  /**
   * 业务数据。后端 Jackson 配置为 `default-property-inclusion: non_null`，
   * 因此失败响应里该字段会被整个省略（不是 `null`），读取时按 `undefined` 处理。
   */
  data?: T
  /** 服务器时间（ISO-8601） */
  timestamp?: string
}

/** 分页结果 */
export interface PageResult<T> {
  records: T[]
  total: number
  /** 当前页码，从 1 开始 */
  page: number
  /** 每页条数 */
  size: number
}

/** 分页/排序通用请求参数 */
export interface PageParams {
  /** 页码，从 1 开始 */
  page?: number
  /** 每页条数 */
  size?: number
  /** 排序，如 `createdAt,desc` */
  sort?: string
}

/* ------------------------------------------------------------------ */
/* 枚举                                                                */
/* ------------------------------------------------------------------ */

export type UserStatus = 'ENABLED' | 'DISABLED'
export type PayType = 'MONTHLY' | 'BONUS' | 'ALLOWANCE' | 'DEDUCTION'
export type SalaryStatus = 'ACTIVE' | 'CLOSED'
/** 审计结果 */
export type AuditOutcome = 'SUCCESS' | 'FAILURE'

/* ------------------------------------------------------------------ */
/* 视图对象                                                            */
/* ------------------------------------------------------------------ */

/** 人员视图（后端 UserView） */
export interface UserView {
  uid: string
  username: string
  name: string
  nickname?: string
  department?: string
  email?: string
  phone?: string
  avatar?: string
  remark?: string
  age?: number
  status: UserStatus
  statusLabel?: string
  roleIds: string[]
  roleNames: string[]
  positionId?: string
  positionName?: string
  lastLoginAt?: string
  createdAt?: string
  updatedAt?: string
}

/** 角色视图（后端 RoleView） */
export interface RoleView {
  id: string
  code: string
  name: string
  description?: string
  permissions: string[]
  builtIn: boolean
  userCount: number
  createdAt?: string
}

/** 权限项 / 权限分组 */
export interface PermissionItem {
  code: string
  label: string
}

export interface PermissionGroup {
  group: string
  items: PermissionItem[]
}

export interface PermissionCatalog {
  groups: PermissionGroup[]
}

/** 职位视图（后端 PositionView） */
export interface PositionView {
  id: string
  code: string
  name: string
  /** 职级，例如 P5 / M2 */
  level?: string
  description?: string
  sortOrder: number
  userCount: number
  createdAt?: string
}

/** 薪资视图（后端 SalaryView） */
export interface SalaryView {
  id: string
  uid: string
  userName?: string
  positionId?: string
  positionName?: string
  amount: number
  payType: PayType
  payTypeLabel?: string
  status: SalaryStatus
  statusLabel?: string
  effectiveFrom?: string
  effectiveTo?: string
  remark?: string
  createdAt?: string
}

/** 审计日志视图（后端 AuditLogView） */
export interface AuditLogView {
  id: string
  username: string
  uid?: string
  action: string
  method: string
  path: string
  ip?: string
  outcome: AuditOutcome | string
  detail?: string
  elapsedMs: number
  createdAt: string
}

/** 统计总览 */
export interface StatsOverview {
  userTotal: number
  userEnabled: number
  userDisabled: number
  roleTotal: number
  positionTotal: number
  salaryTotal: number
  salarySum: number
  departmentCount: number
}

/** 图表用「名称-数值」结构 */
export interface NameValue {
  name: string
  value: number
}

/* ------------------------------------------------------------------ */
/* 认证                                                                */
/* ------------------------------------------------------------------ */

export interface LoginPayload {
  username: string
  password: string
}

export interface RefreshPayload {
  refreshToken: string
}

/** 登录/刷新令牌返回体 */
export interface LoginResult {
  accessToken: string
  refreshToken: string
  tokenType: string
  /** 过期秒数 */
  expiresIn: number
  user: UserView
}

export interface ChangePasswordPayload {
  oldPassword: string
  newPassword: string
}

export interface UpdateProfilePayload {
  name?: string
  nickname?: string
  department?: string
  email?: string
  phone?: string
  age?: number
  avatar?: string
  remark?: string
}

/** GET /api/auth/me —— 当前登录用户的概要信息 */
export interface CurrentUserView {
  uid: string
  username: string
  displayName?: string
  roles: string[]
  permissions: string[]
}

/** 菜单项（后端返回，按权限过滤后的可见菜单） */
export interface MenuItem {
  code: string
  name: string
}

/** GET /api/auth/permissions */
export interface PermissionPayload {
  permissions: string[]
  roles: string[]
  menus: MenuItem[]
}

/* ------------------------------------------------------------------ */
/* 人员                                                                */
/* ------------------------------------------------------------------ */

export interface UserQuery extends PageParams {
  keyword?: string
  roleId?: string
  positionId?: string
  status?: UserStatus | ''
}

export interface UserCreatePayload {
  username: string
  password: string
  name: string
  nickname?: string
  department?: string
  email?: string
  phone?: string
  age?: number
  remark?: string
  roleIds?: string[]
  positionId?: string
}

/** 更新人员：不含 username / password */
export interface UserUpdatePayload {
  name: string
  nickname?: string
  department?: string
  email?: string
  phone?: string
  age?: number
  remark?: string
  avatar?: string
  roleIds?: string[]
  positionId?: string
  status?: UserStatus
}

/* ------------------------------------------------------------------ */
/* 角色                                                                */
/* ------------------------------------------------------------------ */

export interface RoleQuery extends PageParams {
  keyword?: string
}

export interface RoleCreatePayload {
  code: string
  name: string
  description?: string
  permissions: string[]
}

/** 更新角色：code 不可改 */
export interface RoleUpdatePayload {
  name: string
  description?: string
  permissions: string[]
}

/* ------------------------------------------------------------------ */
/* 职位                                                                */
/* ------------------------------------------------------------------ */

export interface PositionQuery extends PageParams {
  keyword?: string
}

export interface PositionPayload {
  code: string
  name: string
  /** 职级，例如 P5 / M2 */
  level?: string
  description?: string
  sortOrder?: number
}

/* ------------------------------------------------------------------ */
/* 薪资                                                                */
/* ------------------------------------------------------------------ */

export interface SalaryQuery extends PageParams {
  uid?: string
  payType?: PayType | ''
  status?: SalaryStatus | ''
}

export interface SalaryPayload {
  uid: string
  positionId?: string
  amount: number
  payType: PayType
  status?: SalaryStatus
  effectiveFrom?: string
  effectiveTo?: string
  remark?: string
}

/* ------------------------------------------------------------------ */
/* 审计                                                                */
/* ------------------------------------------------------------------ */

export interface AuditQuery extends PageParams {
  username?: string
  action?: string
  outcome?: AuditOutcome | string
  /** 起始时间（ISO-8601 或 yyyy-MM-dd HH:mm:ss） */
  from?: string
  /** 结束时间 */
  to?: string
}

/* ------------------------------------------------------------------ */
/* 权限码常量                                                          */
/* ------------------------------------------------------------------ */

export const PERMISSIONS = {
  USER_VIEW: 'user:view',
  USER_CREATE: 'user:create',
  USER_UPDATE: 'user:update',
  USER_DELETE: 'user:delete',
  USER_RESET_PASSWORD: 'user:reset-password',
  ROLE_VIEW: 'role:view',
  ROLE_CREATE: 'role:create',
  ROLE_UPDATE: 'role:update',
  ROLE_DELETE: 'role:delete',
  POSITION_VIEW: 'position:view',
  POSITION_CREATE: 'position:create',
  POSITION_UPDATE: 'position:update',
  POSITION_DELETE: 'position:delete',
  SALARY_VIEW: 'salary:view',
  SALARY_CREATE: 'salary:create',
  SALARY_UPDATE: 'salary:update',
  SALARY_DELETE: 'salary:delete',
  AUDIT_VIEW: 'audit:view',
  AUDIT_CLEAR: 'audit:clear'
} as const

export type PermissionCode = (typeof PERMISSIONS)[keyof typeof PERMISSIONS]
