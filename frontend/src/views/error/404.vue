<script setup lang="ts">
/** 404 页面 */
import { useRouter } from 'vue-router'
import { Back, HomeFilled } from '@element-plus/icons-vue'

defineOptions({ name: 'NotFoundPage' })

const router = useRouter()

function goHome(): void {
  void router.replace('/dashboard')
}

function goBack(): void {
  if (window.history.length > 1) {
    router.back()
  } else {
    goHome()
  }
}
</script>

<template>
  <div class="error-page">
    <div class="error-page__code">404</div>
    <h1 class="error-page__title">页面走丢了</h1>
    <p class="error-page__desc">抱歉，你访问的页面不存在或已被移除，请检查地址是否正确。</p>
    <div class="error-page__actions">
      <el-button type="primary" :icon="HomeFilled" @click="goHome">返回首页</el-button>
      <el-button :icon="Back" @click="goBack">返回上一页</el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  text-align: center;
  background: var(--pms-bg-page, #f2f4f8);
}

.error-page__code {
  font-size: 96px;
  font-weight: 700;
  line-height: 1;
  color: transparent;
  background: linear-gradient(135deg, #2f6fed, #7ac7ff);
  -webkit-background-clip: text;
  background-clip: text;
}

.error-page__title {
  margin: 18px 0 8px;
  font-size: 22px;
  color: #1f2a3c;
}

.error-page__desc {
  max-width: 420px;
  margin: 0 0 26px;
  font-size: 14px;
  line-height: 1.7;
  color: #7a8699;
}

.error-page__actions {
  display: flex;
  gap: 12px;
}
</style>
