<script setup lang="ts">
/**
 * 登录页：用户名 / 密码 + 表单校验 + 回车提交 + loading + 错误提示。
 * 登录成功后跳转 redirect 查询参数指定的地址，默认 /dashboard。
 */
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, Right, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

defineOptions({ name: 'LoginPage' })

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const errorMessage = ref('')
const capsLock = ref(false)

const form = reactive({
  username: 'admin',
  password: 'Admin@123'
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' }
  ]
}

/** 登录成功后的落地地址，只接受站内路径，避免开放重定向 */
const redirect = computed<string>(() => {
  const value = route.query.redirect
  const target = Array.isArray(value) ? value[0] : value
  if (typeof target === 'string' && target.startsWith('/') && !target.startsWith('//')) {
    return target
  }
  return '/dashboard'
})

function fillDemoAccount(): void {
  form.username = 'admin'
  form.password = 'Admin@123'
}

function handleKeyup(event: KeyboardEvent): void {
  capsLock.value = typeof event.getModifierState === 'function' ? event.getModifierState('CapsLock') : false
}

async function handleSubmit(): Promise<void> {
  if (submitting.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (valid !== true) return

  submitting.value = true
  errorMessage.value = ''
  try {
    await authStore.login({ username: form.username.trim(), password: form.password })
    ElMessage.success('登录成功')
    await router.replace(redirect.value)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-page__glow login-page__glow--one" />
    <div class="login-page__glow login-page__glow--two" />

    <div class="login-card">
      <aside class="login-card__brand">
        <div class="login-card__logo">PM</div>
        <h1 class="login-card__title">人员管理系统</h1>
        <p class="login-card__subtitle">Personnel Management System</p>
        <ul class="login-card__features">
          <li>人员 / 角色 / 职位一体化管理</li>
          <li>薪资档案与统计分析</li>
          <li>操作审计日志可追溯</li>
        </ul>
      </aside>

      <section class="login-card__form">
        <header class="login-card__form-header">
          <h2>欢迎回来</h2>
          <p>请使用系统账号登录</p>
        </header>

        <el-alert
          v-if="errorMessage"
          class="login-card__alert"
          :title="errorMessage"
          type="error"
          show-icon
          :closable="false"
        />

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="handleSubmit"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              clearable
              autocomplete="username"
              :prefix-icon="User"
              @keyup.enter="handleSubmit"
              @keyup="handleKeyup"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
              autocomplete="current-password"
              :prefix-icon="Lock"
              @keyup.enter="handleSubmit"
              @keyup="handleKeyup"
            />
          </el-form-item>

          <p v-if="capsLock" class="login-card__caps">提示：大写锁定已开启</p>

          <el-button
            class="login-card__submit"
            type="primary"
            size="large"
            :loading="submitting"
            native-type="submit"
            @click="handleSubmit"
          >
            {{ submitting ? '登录中…' : '登 录' }}
            <el-icon v-if="!submitting"><Right /></el-icon>
          </el-button>
        </el-form>

        <div class="login-card__demo">
          <span>演示账号：<b>admin</b> / <b>Admin@123</b></span>
          <el-link type="primary" :underline="false" @click="fillDemoAccount">一键填入</el-link>
        </div>

        <p class="login-card__footer">© {{ new Date().getFullYear() }} 人员管理系统</p>
      </section>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  overflow: hidden;
  background: linear-gradient(135deg, #1b2a4a 0%, #2f6fed 55%, #4f8ef7 100%);
}

.login-page__glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(10px);
  opacity: 0.35;
  pointer-events: none;

  &--one {
    top: -120px;
    left: -80px;
    width: 420px;
    height: 420px;
    background: radial-gradient(circle, rgba(255, 255, 255, 0.55), transparent 70%);
  }

  &--two {
    right: -140px;
    bottom: -160px;
    width: 520px;
    height: 520px;
    background: radial-gradient(circle, rgba(122, 199, 255, 0.5), transparent 70%);
  }
}

.login-card {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  max-width: 880px;
  overflow: hidden;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 24px 60px rgba(15, 32, 66, 0.28);
}

.login-card__brand {
  display: flex;
  flex-direction: column;
  justify-content: center;
  width: 46%;
  padding: 44px 36px;
  color: #fff;
  background: linear-gradient(160deg, #24365c 0%, #2f6fed 100%);
}

.login-card__logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  margin-bottom: 20px;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 1px;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 14px;
}

.login-card__title {
  margin: 0;
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 2px;
}

.login-card__subtitle {
  margin: 8px 0 26px;
  font-size: 12px;
  letter-spacing: 1px;
  text-transform: uppercase;
  opacity: 0.7;
}

.login-card__features {
  padding: 0;
  margin: 0;
  font-size: 14px;
  line-height: 2.2;
  list-style: none;

  li::before {
    margin-right: 8px;
    content: '·';
    font-weight: 700;
  }
}

.login-card__form {
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: center;
  padding: 44px 40px;
}

.login-card__form-header {
  margin-bottom: 22px;

  h2 {
    margin: 0;
    font-size: 22px;
    color: #1f2a3c;
  }

  p {
    margin: 6px 0 0;
    font-size: 13px;
    color: #7a8699;
  }
}

.login-card__alert {
  margin-bottom: 16px;
}

.login-card__caps {
  margin: -6px 0 12px;
  font-size: 12px;
  color: #e6a23c;
}

.login-card__submit {
  width: 100%;
  letter-spacing: 2px;
}

.login-card__demo {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  margin-top: 20px;
  font-size: 13px;
  color: #61708a;
  background: var(--pms-bg-page, #f2f4f8);
  border-radius: 8px;
}

.login-card__footer {
  margin: 22px 0 0;
  font-size: 12px;
  color: #a2abbd;
  text-align: center;
}

@media (max-width: 768px) {
  .login-card__brand {
    display: none;
  }

  .login-card__form {
    padding: 32px 24px;
  }
}
</style>
