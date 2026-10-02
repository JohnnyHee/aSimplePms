<script setup lang="ts">
/** 403 页面：已登录但没有访问该资源的权限 */
import { useRouter } from 'vue-router'
import { Back, HomeFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

defineOptions({ name: 'ForbiddenPage' })

const router = useRouter()
const authStore = useAuthStore()

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
    <div class="error-page__code">403</div>
    <h1 class="error-page__title">没有访问权限</h1>
    <p class="error-page__desc">
      当前账号
      <b>{{ authStore.displayName }}</b>
      没有访问该页面的权限，请联系系统管理员分配相应角色。
    </p>
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
  background: linear-gradient(135deg, #e6a23c, #f56c6c);
  -webkit-background-clip: text;
  background-clip: text;
}

.error-page__title {
  margin: 18px 0 8px;
  font-size: 22px;
  color: #1f2a3c;
}

.error-page__desc {
  max-width: 460px;
  margin: 0 0 26px;
  font-size: 14px;
  line-height: 1.7;
  color: #7a8699;

  b {
    color: #1f2a3c;
  }
}

.error-page__actions {
  display: flex;
  gap: 12px;
}
</style>
