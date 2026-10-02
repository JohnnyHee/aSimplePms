<script setup lang="ts">
/**
 * 顶部头部：折叠按钮 + 面包屑 + 主题切换 + 当前用户下拉（个人中心 / 修改密码 / 退出登录）。
 */
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  ArrowDown,
  Expand,
  Fold,
  Lock,
  Moon,
  Sunny,
  SwitchButton,
  User
} from '@element-plus/icons-vue'
import AppBreadcrumb from './AppBreadcrumb.vue'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import { nameInitial } from '@/utils/format'

const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()

const collapsed = computed(() => appStore.sidebarCollapsed)
const displayName = computed(() => authStore.displayName)
const avatar = computed(() => authStore.avatar)
const roleText = computed(() => authStore.roles.join(' / ') || '未分配角色')
const isDark = computed(() => appStore.theme === 'dark')

/* ---------------- 修改密码 ---------------- */
const passwordVisible = ref(false)
const submitting = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (
  _rule: unknown,
  value: string,
  callback: (error?: Error) => void
): void => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validateConfirm, trigger: 'blur' }]
}

function openPasswordDialog(): void {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordVisible.value = true
}

async function submitPassword(): Promise<void> {
  const form = passwordFormRef.value
  if (!form) return
  try {
    await form.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await authStore.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请使用新密码重新登录')
    passwordVisible.value = false
    await authStore.logout()
    router.replace({ path: '/login' })
  } catch {
    // 错误提示已由 axios 拦截器统一弹出
  } finally {
    submitting.value = false
  }
}

/* ---------------- 退出登录 ---------------- */
async function handleLogout(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await authStore.logout()
  ElMessage.success('已退出登录')
  router.replace({ path: '/login' })
}

/* ---------------- 下拉菜单 ---------------- */
async function handleCommand(command: string): Promise<void> {
  switch (command) {
    case 'profile':
      router.push({ path: '/profile' })
      break
    case 'password':
      openPasswordDialog()
      break
    case 'logout':
      await handleLogout()
      break
    default:
      break
  }
}
</script>

<template>
  <div class="app-header">
    <div class="app-header__left">
      <el-button
        class="app-header__collapse"
        text
        :icon="collapsed ? Expand : Fold"
        :title="collapsed ? '展开菜单' : '收起菜单'"
        @click="appStore.toggleSidebar()"
      />
      <AppBreadcrumb />
    </div>

    <div class="app-header__right">
      <el-button
        class="app-header__theme"
        text
        :icon="isDark ? Sunny : Moon"
        :title="isDark ? '切换到浅色主题' : '切换到深色主题'"
        @click="appStore.toggleTheme()"
      />

      <el-dropdown trigger="click" @command="handleCommand">
        <div class="app-header__user">
          <el-avatar :size="30" :src="avatar || undefined">
            {{ nameInitial(displayName) }}
          </el-avatar>
          <div class="app-header__user-meta">
            <span class="app-header__user-name">{{ displayName }}</span>
            <span class="app-header__user-role">{{ roleText }}</span>
          </div>
          <el-icon class="app-header__user-arrow"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile" :icon="User">个人中心</el-dropdown-item>
            <el-dropdown-item command="password" :icon="Lock">修改密码</el-dropdown-item>
            <el-dropdown-item command="logout" divided :icon="SwitchButton">
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <el-dialog v-model="passwordVisible" title="修改密码" width="420px" append-to-body>
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="88px"
        @submit.prevent
      >
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            show-password
            placeholder="请输入当前密码"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            placeholder="至少 6 位"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
            @keyup.enter="submitPassword"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 100%;
  padding: 0 16px;

  &__left {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
  }

  &__collapse {
    font-size: 18px;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__theme {
    font-size: 17px;
  }

  &__user {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 4px 8px;
    cursor: pointer;
    border-radius: 6px;
    outline: none;
    transition: background-color 0.2s ease;

    &:hover {
      background-color: var(--pms-border-light);
    }
  }

  &__user-meta {
    display: flex;
    flex-direction: column;
    line-height: 1.2;
  }

  &__user-name {
    font-size: 13px;
    font-weight: 600;
    color: var(--pms-text-primary);
  }

  &__user-role {
    font-size: 11px;
    color: var(--pms-text-secondary);
  }

  &__user-arrow {
    font-size: 12px;
    color: var(--pms-text-secondary);
  }
}
</style>
