<script setup lang="ts">
/**
 * 个人中心：查看本人资料、修改资料与密码。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Key, User } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/format'

defineOptions({ name: 'ProfilePage' })

const authStore = useAuthStore()

const activeTab = ref('profile')

/* ---------------- 基本资料 ---------------- */
const profileRef = ref<FormInstance>()
const savingProfile = ref(false)

const profileForm = reactive({
  name: '',
  nickname: '',
  department: '',
  email: '',
  phone: '',
  age: undefined as number | undefined,
  avatar: '',
  remark: ''
})

const profileRules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  age: [{ type: 'number', min: 16, max: 100, message: '年龄需在 16 ~ 100 之间', trigger: 'blur' }]
}

const accountInfo = computed(() => authStore.user)

function fillForm(): void {
  const user = authStore.user
  if (!user) return
  profileForm.name = user.name ?? ''
  profileForm.nickname = user.nickname ?? ''
  profileForm.department = user.department ?? ''
  profileForm.email = user.email ?? ''
  profileForm.phone = user.phone ?? ''
  profileForm.age = user.age ?? undefined
  profileForm.avatar = user.avatar ?? ''
  profileForm.remark = user.remark ?? ''
}

async function saveProfile(): Promise<void> {
  const valid = await profileRef.value?.validate().catch(() => false)
  if (!valid) return
  savingProfile.value = true
  try {
    await authStore.updateProfile({
      name: profileForm.name,
      nickname: profileForm.nickname || undefined,
      department: profileForm.department || undefined,
      email: profileForm.email || undefined,
      phone: profileForm.phone || undefined,
      age: profileForm.age ?? undefined,
      avatar: profileForm.avatar || undefined,
      remark: profileForm.remark || undefined
    })
    ElMessage.success('资料已保存')
    fillForm()
  } finally {
    savingProfile.value = false
  }
}

/* ---------------- 修改密码 ---------------- */
const passwordRef = ref<FormInstance>()
const savingPassword = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度需为 8 ~ 64 位', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/,
      message: '密码需同时包含字母和数字',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback: (error?: Error) => void) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

async function savePassword(): Promise<void> {
  const valid = await passwordRef.value?.validate().catch(() => false)
  if (!valid) return
  savingPassword.value = true
  try {
    await authStore.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码已修改，请使用新密码重新登录')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordRef.value?.clearValidate()
  } finally {
    savingPassword.value = false
  }
}

onMounted(async () => {
  if (!authStore.user) {
    await authStore.fetchProfile().catch(() => null)
  }
  fillForm()
})
</script>

<template>
  <PageContainer title="个人中心" description="查看并维护本人资料，修改登录密码">
    <div class="profile">
      <el-card class="profile__card" shadow="never">
        <div class="identity">
          <el-avatar :size="72" :src="authStore.avatar || undefined">
            {{ (authStore.displayName || 'U').slice(0, 1) }}
          </el-avatar>
          <div class="identity__meta">
            <div class="identity__name">{{ authStore.displayName }}</div>
            <div class="identity__account">登录名：{{ accountInfo?.username || '-' }}</div>
            <div class="identity__roles">
              <el-tag
                v-for="role in authStore.roles"
                :key="role"
                size="small"
                type="info"
                class="identity__role"
              >
                {{ role }}
              </el-tag>
              <span v-if="authStore.roles.length === 0" class="identity__empty">暂无角色</span>
            </div>
          </div>
          <div class="identity__extra">
            <div>部门：{{ accountInfo?.department || '-' }}</div>
            <div>职位：{{ accountInfo?.positionName || '-' }}</div>
            <div>最后登录：{{ formatDateTime(accountInfo?.lastLoginAt) }}</div>
          </div>
        </div>
      </el-card>

      <el-card class="profile__card" shadow="never">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基本资料" name="profile">
            <el-form
              ref="profileRef"
              :model="profileForm"
              :rules="profileRules"
              label-width="90px"
              class="profile__form"
            >
              <el-form-item label="姓名" prop="name">
                <el-input v-model="profileForm.name" maxlength="32" placeholder="请输入姓名" />
              </el-form-item>
              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="profileForm.nickname" maxlength="32" placeholder="请输入昵称" />
              </el-form-item>
              <el-form-item label="部门" prop="department">
                <el-input v-model="profileForm.department" maxlength="32" placeholder="请输入部门" />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="profileForm.email" maxlength="64" placeholder="请输入邮箱" />
              </el-form-item>
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="profileForm.phone" maxlength="11" placeholder="请输入手机号" />
              </el-form-item>
              <el-form-item label="年龄" prop="age">
                <el-input-number
                  v-model="profileForm.age"
                  :min="16"
                  :max="100"
                  controls-position="right"
                  placeholder="请输入年龄"
                />
              </el-form-item>
              <el-form-item label="头像地址" prop="avatar">
                <el-input v-model="profileForm.avatar" maxlength="255" placeholder="头像图片链接（可选）" />
              </el-form-item>
              <el-form-item label="备注" prop="remark">
                <el-input
                  v-model="profileForm.remark"
                  type="textarea"
                  :rows="3"
                  maxlength="255"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="User" :loading="savingProfile" @click="saveProfile">
                  保存资料
                </el-button>
                <el-button @click="fillForm">重置</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="修改密码" name="password">
            <el-form
              ref="passwordRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-width="90px"
              class="profile__form"
            >
              <el-form-item label="原密码" prop="oldPassword">
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
                  placeholder="8 ~ 64 位，需含字母和数字"
                />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  show-password
                  placeholder="请再次输入新密码"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Key" :loading="savingPassword" @click="savePassword">
                  修改密码
                </el-button>
                <span class="profile__hint">修改成功后当前登录令牌会被吊销，需要重新登录。</span>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.profile {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.profile__card {
  border-radius: 8px;
}

.identity {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.identity__meta {
  flex: 1;
  min-width: 200px;
}

.identity__name {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.identity__account {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.identity__roles {
  margin-top: 8px;
}

.identity__role {
  margin-right: 6px;
}

.identity__empty {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.identity__extra {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 22px;
}

.profile__form {
  max-width: 520px;
  margin-top: 8px;
}

.profile__hint {
  margin-left: 12px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
