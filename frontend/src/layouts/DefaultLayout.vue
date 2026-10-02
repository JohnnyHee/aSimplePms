<script setup lang="ts">
/**
 * 默认布局：左侧菜单 + 顶部头部 + 主内容区。
 */
import { computed } from 'vue'
import { useAppStore } from '@/stores/app'
import AppSidebar from './components/AppSidebar.vue'
import AppHeader from './components/AppHeader.vue'

const appStore = useAppStore()

const asideWidth = computed(() =>
  appStore.sidebarCollapsed ? 'var(--pms-sidebar-width-collapsed)' : 'var(--pms-sidebar-width)'
)
</script>

<template>
  <el-container class="default-layout">
    <el-aside class="default-layout__aside" :width="asideWidth">
      <AppSidebar />
    </el-aside>

    <el-container class="default-layout__body">
      <el-header class="default-layout__header" height="var(--pms-header-height)">
        <AppHeader />
      </el-header>

      <el-main class="default-layout__main">
        <router-view v-slot="{ Component, route }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped lang="scss">
.default-layout {
  height: 100vh;
  overflow: hidden;
  background-color: var(--pms-bg-page);

  &__aside {
    overflow: hidden;
    background-color: var(--pms-bg-sidebar);
    transition: width 0.25s ease;
  }

  &__body {
    min-width: 0;
    overflow: hidden;
  }

  &__header {
    display: flex;
    align-items: center;
    padding: 0;
    background-color: var(--pms-bg-header);
    border-bottom: 1px solid var(--pms-border-light);
    box-shadow: var(--pms-shadow-header);
  }

  &__main {
    padding: 16px;
    overflow-y: auto;
    background-color: var(--pms-bg-page);
  }
}
</style>
