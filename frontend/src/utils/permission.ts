/**
 * 权限工具：`usePermission()` 组合式函数 + `v-permission` 指令。
 *
 * 用法：
 *  - 模板里控制按钮：`<el-button v-permission="'user:create'">新增</el-button>`
 *  - 多个权限任一即可：`<el-button v-permission="['user:update','user:delete']">`
 *  - 脚本里判断：`const { has, hasAny, hasAll } = usePermission(); has('user:delete')`
 */
import { computed, type App, type Directive, type DirectiveBinding } from 'vue'
import { useAuthStore } from '@/stores/auth'

export type PermissionValue = string | string[] | undefined

/**
 * 权限组合式函数。
 * `has(code)` 支持单个权限码或权限码数组（任一命中即为 true）。
 */
export function usePermission() {
  const auth = useAuthStore()

  function has(code?: PermissionValue): boolean {
    return auth.hasPermission(code)
  }

  /** 命中任意一个权限码 */
  function hasAny(codes: string[]): boolean {
    return codes.some((code) => auth.hasPermission(code))
  }

  /** 必须同时拥有全部权限码 */
  function hasAll(codes: string[]): boolean {
    return codes.every((code) => auth.hasPermission(code))
  }

  /** 是否拥有某个角色 */
  function hasRole(code?: PermissionValue): boolean {
    return auth.hasRole(code)
  }

  /** 按权限码过滤菜单/列表项 */
  function filterByPermission<T extends { permission?: string }>(items: T[]): T[] {
    return items.filter((item) => auth.hasPermission(item.permission))
  }

  const permissions = computed(() => auth.permissions)
  const roles = computed(() => auth.roles)
  const isAdmin = computed(() => auth.isAdmin)

  return { has, hasAny, hasAll, hasRole, filterByPermission, permissions, roles, isAdmin }
}

/** 无权限时把元素从 DOM 中移除 */
function applyPermission(el: HTMLElement, binding: DirectiveBinding<PermissionValue>): void {
  const allowed = useAuthStore().hasPermission(binding.value)
  if (allowed) {
    return
  }
  if (el.parentNode) {
    el.parentNode.removeChild(el)
  } else {
    el.style.display = 'none'
  }
}

/** `v-permission="'user:create'"` 指令 */
export const permission: Directive<HTMLElement, PermissionValue> = {
  mounted(el, binding) {
    applyPermission(el, binding)
  },
  updated(el, binding) {
    if (binding.value === binding.oldValue) return
    applyPermission(el, binding)
  }
}

/** 注册全局指令：main.ts 里 `setupPermissionDirective(app)` */
export function setupPermissionDirective(app: App): void {
  app.directive('permission', permission)
}

export default permission
