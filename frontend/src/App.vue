<script setup lang="ts">
import { useAppStore } from '@/stores/app'

// 初始化主题（store 内 watch immediate 会把主题写到 <html data-theme>）
const appStore = useAppStore()
appStore.applyTheme()
</script>

<template>
  <router-view />
</template>
