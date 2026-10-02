<script setup lang="ts">
/**
 * 面包屑：根据 route.matched 中的 meta.title 生成。
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import type { RouteLocationMatched } from 'vue-router'

const route = useRoute()

interface Crumb {
  title: string
  path: string
}

const crumbs = computed<Crumb[]>(() => {
  const matched = route.matched.filter(
    (item: RouteLocationMatched) => Boolean(item.meta?.title) && !item.meta?.hidden
  )
  const list: Crumb[] = matched.map((item) => ({
    title: (item.meta?.title as string) ?? '',
    path: item.path
  }))
  // 首页兜底：深链直接进入子页面时给出「工作台」入口
  if (list.length === 0) {
    return [{ title: '工作台', path: '/dashboard' }]
  }
  return list
})
</script>

<template>
  <el-breadcrumb class="app-breadcrumb" separator="/">
    <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
    <el-breadcrumb-item v-for="(crumb, index) in crumbs" :key="crumb.path + index">
      {{ crumb.title }}
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<style scoped lang="scss">
.app-breadcrumb {
  font-size: 13px;
  line-height: 1;
  white-space: nowrap;

  :deep(.el-breadcrumb__inner) {
    color: var(--pms-text-secondary);
  }

  :deep(.el-breadcrumb__inner.is-link) {
    color: var(--pms-primary);
  }
}
</style>
