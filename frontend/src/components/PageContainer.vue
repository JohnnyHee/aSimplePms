<script setup lang="ts">
/**
 * 页面外壳：标题 + 描述 + 右侧操作插槽 + 内容插槽。
 *
 * 用法：
 * <PageContainer title="人员管理" description="维护人员账号与角色">
 *   <template #extra><el-button type="primary">新增</el-button></template>
 *   ...页面内容...
 * </PageContainer>
 */
withDefaults(
  defineProps<{
    /** 页面标题 */
    title?: string
    /** 页面描述 */
    description?: string
    /** 是否显示顶部操作区下方的间距（内容区自带间距，默认 true） */
    padded?: boolean
  }>(),
  {
    title: '',
    description: '',
    padded: true
  }
)
</script>

<template>
  <div class="page-container">
    <div v-if="title || description || $slots.extra" class="page-container__header">
      <div>
        <h2 v-if="title" class="page-container__title">{{ title }}</h2>
        <p v-if="description" class="page-container__desc">{{ description }}</p>
      </div>
      <div v-if="$slots.extra" class="page-container__extra">
        <slot name="extra" />
      </div>
    </div>

    <div :class="['page-container__body', { 'is-padded': padded }]">
      <slot />
    </div>
  </div>
</template>

<style scoped lang="scss">
.page-container__body.is-padded {
  gap: 16px;
}
</style>
