/**
 * 路由表 + 全局守卫。
 *
 * 元信息约定（meta）：
 *  - title       浏览器标题 & 菜单名 & 面包屑
 *  - icon        Element Plus 图标组件名（侧边栏用）
 *  - permission  访问/显示所需权限码（缺省表示不需权限）
 *  - roles       访问所需角色（可选）
 *  - public      免登录页面
 *  - hidden      不在侧边栏显示
 */
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    icon?: string
    permission?: string
    roles?: string[]
    /** 免登录访问 */
    public?: boolean
    /** 不在侧边栏展示 */
    hidden?: boolean
    /** 是否缓存组件 */
    keepAlive?: boolean
  }
}

/** 应用标题（用于 document.title） */
export const APP_TITLE = import.meta.env.VITE_APP_TITLE || '人员管理系统'

/** 布局组件（懒加载） */
const DefaultLayout = () => import('@/layouts/DefaultLayout.vue')

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true, hidden: true }
  },
  {
    path: '/',
    component: DefaultLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'Odometer' }
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '人员管理', icon: 'User', permission: 'user:view' }
      },
      {
        path: 'roles',
        name: 'roles',
        component: () => import('@/views/role/index.vue'),
        meta: { title: '角色权限', icon: 'Avatar', permission: 'role:view' }
      },
      {
        path: 'positions',
        name: 'positions',
        component: () => import('@/views/position/index.vue'),
        meta: { title: '职位管理', icon: 'Briefcase', permission: 'position:view' }
      },
      {
        path: 'salaries',
        name: 'salaries',
        component: () => import('@/views/salary/index.vue'),
        meta: { title: '薪资管理', icon: 'Money', permission: 'salary:view' }
      },
      {
        path: 'audit-logs',
        name: 'audit-logs',
        component: () => import('@/views/audit/index.vue'),
        meta: { title: '审计日志', icon: 'Document', permission: 'audit:view' }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', icon: 'Setting' }
      },
      {
        path: '403',
        name: 'forbidden',
        component: () => import('@/views/error/403.vue'),
        meta: { title: '无访问权限', hidden: true }
      }
    ]
  },
  {
    path: '/404',
    name: 'not-found',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', public: true, hidden: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found-catch-all',
    redirect: '/404',
    meta: { hidden: true }
  }
]

/** 布局下可展示在侧边栏的菜单路由（未做权限过滤） */
export function getMenuRoutes(): RouteRecordRaw[] {
  const layout = routes.find((route) => route.path === '/')
  const children = layout?.children ?? []
  return children.filter((route) => !route.meta?.hidden && Boolean(route.meta?.title))
}

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

/** 把 fullPath 规整成可安全放进 redirect 的值 */
function safeRedirect(fullPath: string): string | undefined {
  if (!fullPath || fullPath === '/' || fullPath.startsWith('/login')) return undefined
  return fullPath
}

router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title} - ${APP_TITLE}` : APP_TITLE

  const auth = useAuthStore()

  // 已登录访问登录页 → 回工作台（或 redirect 指定的页面）
  if (to.name === 'login') {
    if (!auth.isLoggedIn) return true
    const redirect = typeof to.query.redirect === 'string' ? safeRedirect(to.query.redirect) : undefined
    return redirect ? { path: redirect } : { path: '/dashboard' }
  }

  // 免登录页面
  if (to.meta.public) return true

  // 未登录 → 去登录页并记住来源
  if (!auth.isLoggedIn) {
    const redirect = safeRedirect(to.fullPath)
    return { path: '/login', query: redirect ? { redirect } : {} }
  }

  // 已登录但内存里没有用户资料（刷新页面 / 直接深链进入）→ 先拉一次
  if (!auth.profileLoaded) {
    try {
      await auth.fetchProfile()
    } catch {
      auth.clearSession()
      const redirect = safeRedirect(to.fullPath)
      return { path: '/login', query: redirect ? { redirect } : {} }
    }
  }

  // 权限 / 角色校验
  if (to.meta.permission && !auth.hasPermission(to.meta.permission)) {
    return { path: '/403' }
  }
  if (to.meta.roles && to.meta.roles.length > 0 && !auth.hasRole(to.meta.roles)) {
    return { path: '/403' }
  }

  return true
})

export default router
