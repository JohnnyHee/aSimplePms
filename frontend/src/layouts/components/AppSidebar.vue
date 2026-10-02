<script setup lang="ts">
/**
 * 侧边栏：Logo + 按权限过滤的菜单（支持折叠）。
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import { getMenuRoutes } from '@/router'

const route = useRoute()
const appStore = useAppStore()
const authStore = useAuthStore()

/** 菜单项：路径 + 标题 + 图标 + 权限 */
interface MenuItem {
  path: string
  title: string
  icon: string
  permission?: string
}

const menus = computed<MenuItem[]>(() =>
  getMenuRoutes().map((item) => ({
    path: item.path.startsWith('/') ? item.path : `/${item.path}`,
    title: item.meta?.title ?? '',
    icon: item.meta?.icon ?? 'Menu',
    permission: item.meta?.permission
  }))
)

/** 只显示当前用户有权限的菜单 */
const visibleMenus = computed(() =>
  menus.value.filter((item) => authStore.hasPermission(item.permission))
)

const activeMenu = computed(() => route.path)

const collapsed = computed(() => appStore.sidebarCollapsed)
</script>

<template>
  <div class="app-sidebar">
    <div class="app-sidebar__logo">
      <div class="app-sidebar__logo-mark">PM</div>
      <transition name="fade-slide">
        <span v-show="!collapsed" class="app-sidebar__logo-text">人员管理系统</span>
      </transition>
    </div>

    <el-scrollbar class="app-sidebar__scroll">
      <el-menu
        class="app-sidebar__menu"
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        router
        unique-opened
        background-color="transparent"
        text-color="var(--pms-text-inverse)"
        active-text-color="#ffffff"
      >
        <el-menu-item v-for="item in visibleMenus" :key="item.path" :index="item.path">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-scrollbar>

    <div v-show="!collapsed" class="app-sidebar__footer">
      <span>PMS · Vue 3 + Element Plus</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.app-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  color: var(--pms-text-inverse);

  &__logo {
    display: flex;
    align-items: center;
    gap: 10px;
    height: var(--pms-header-height);
    padding: 0 16px;
    overflow: hidden;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  }

  &__logo-mark {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    font-size: 13px;
    font-weight: 700;
    letter-spacing: 0.5px;
    color: #ffffff;
    background-image: linear-gradient(135deg, #2f6fed, #6b98f2);
    border-radius: 8px;
  }

  &__logo-text {
    font-size: 15px;
    font-weight: 600;
    white-space: nowrap;
  }

  &__scroll {
    flex: 1;
    min-height: 0;
  }

  &__menu {
    border-right: none;

    :deep(.el-menu-item) {
      height: 46px;
      margin: 4px 8px;
      line-height: 46px;
      color: var(--pms-text-inverse);
      border-radius: 6px;

      &:hover {
        background-color: var(--pms-bg-sidebar-hover) !important;
      }

      &.is-active {
        font-weight: 600;
        background-color: var(--pms-bg-sidebar-active) !important;
      }
    }
  }

  &__footer {
    padding: 12px 16px;
    font-size: 12px;
    color: rgba(232, 238, 248, 0.45);
    white-space: nowrap;
    border-top: 1px solid rgba(255, 255, 255, 0.08);
  }
}

:deep(.el-menu--collapse) {
  width: var(--pms-sidebar-width-collapsed);
}
</style>
