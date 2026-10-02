<script setup lang="ts">
/**
 * 查询条件栏：表单插槽 + 查询/重置按钮 + 额外操作插槽。
 *
 * 用法：
 * <SearchBar @search="handleSearch" @reset="handleReset">
 *   <el-form-item label="关键字">
 *     <el-input v-model="query.keyword" placeholder="姓名/用户名" clearable />
 *   </el-form-item>
 *   <template #actions><el-button>导出</el-button></template>
 * </SearchBar>
 */
import { Refresh, Search } from '@element-plus/icons-vue'

withDefaults(
  defineProps<{
    /** 查询按钮 loading */
    loading?: boolean
    /** 是否显示重置按钮 */
    showReset?: boolean
    /** 查询按钮文案 */
    searchText?: string
  }>(),
  {
    loading: false,
    showReset: true,
    searchText: '查询'
  }
)

const emit = defineEmits<{
  (e: 'search'): void
  (e: 'reset'): void
}>()

function handleReset(): void {
  emit('reset')
}
</script>

<template>
  <div class="search-bar">
    <el-form class="search-bar__form" :inline="true" @submit.prevent="emit('search')">
      <slot />
    </el-form>

    <div class="search-bar__actions">
      <el-button type="primary" :icon="Search" :loading="loading" @click="emit('search')">
        {{ searchText }}
      </el-button>
      <el-button v-if="showReset" :icon="Refresh" @click="handleReset">重置</el-button>
      <slot name="actions" />
    </div>
  </div>
</template>
