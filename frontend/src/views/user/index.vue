<script setup lang="ts">
/**
 * 人员管理：查询 + 分页表格 + 新增/编辑/启用停用/重置密码/删除。
 * 对应后端 /api/users（分页、关键字、角色、职位、状态过滤）。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Key, Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import SearchBar from '@/components/SearchBar.vue'
import {
  createUser,
  deleteUser,
  fetchUsers,
  resetUserPassword,
  updateUser,
  updateUserStatus
} from '@/api/user'
import { fetchRoleOptions } from '@/api/role'
import { fetchPositionOptions } from '@/api/position'
import type {
  PositionView,
  RoleView,
  UserCreatePayload,
  UserStatus,
  UserUpdatePayload,
  UserView
} from '@/api/types'
import { formatDateTime } from '@/utils/format'
import { usePermission } from '@/utils/permission'

defineOptions({ name: 'UserPage' })

const { has } = usePermission()

/* ---------------- 查询条件 ---------------- */
const query = reactive({
  keyword: '',
  roleId: '',
  positionId: '',
  status: '' as UserStatus | ''
})

const pagination = reactive({ page: 1, size: 20, total: 0 })

const loading = ref(false)
const records = ref<UserView[]>([])
const roleOptions = ref<RoleView[]>([])
const positionOptions = ref<PositionView[]>([])

async function loadOptions(): Promise<void> {
  const [roles, positions] = await Promise.all([
    fetchRoleOptions().catch(() => [] as RoleView[]),
    fetchPositionOptions().catch(() => [] as PositionView[])
  ])
  roleOptions.value = roles
  positionOptions.value = positions
}

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const result = await fetchUsers({
      page: pagination.page,
      size: pagination.size,
      keyword: query.keyword || undefined,
      roleId: query.roleId || undefined,
      positionId: query.positionId || undefined,
      status: query.status || undefined
    })
    records.value = result?.records ?? []
    pagination.total = result?.total ?? 0
  } catch {
    records.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  pagination.page = 1
  void loadData()
}

function handleReset(): void {
  query.keyword = ''
  query.roleId = ''
  query.positionId = ''
  query.status = ''
  handleSearch()
}

function handleSizeChange(size: number): void {
  pagination.size = size
  pagination.page = 1
  void loadData()
}

/* ---------------- 新增 / 编辑 ---------------- */
interface UserFormState {
  uid: string
  username: string
  password: string
  name: string
  nickname: string
  department: string
  email: string
  phone: string
  age: number | undefined
  remark: string
  roleIds: string[]
  positionId: string
  status: UserStatus
}

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<UserFormState>({
  uid: '',
  username: '',
  password: '',
  name: '',
  nickname: '',
  department: '',
  email: '',
  phone: '',
  age: undefined,
  remark: '',
  roleIds: [],
  positionId: '',
  status: 'ENABLED'
})

const rules = computed<FormRules>(() => ({
  username: [
    { required: true, message: '请输入登录名', trigger: 'blur' },
    {
      pattern: /^[A-Za-z][A-Za-z0-9_.]{2,31}$/,
      message: '以字母开头，3-32 位字母/数字/下划线/点',
      trigger: 'blur'
    }
  ],
  password:
    dialogMode.value === 'create'
      ? [
          { required: true, message: '请输入初始密码', trigger: 'blur' },
          { min: 8, max: 64, message: '密码长度 8-64 位', trigger: 'blur' },
          {
            pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/,
            message: '密码需同时包含字母和数字',
            trigger: 'blur'
          }
        ]
      : [],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [
    { pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}))

function resetForm(): void {
  form.uid = ''
  form.username = ''
  form.password = ''
  form.name = ''
  form.nickname = ''
  form.department = ''
  form.email = ''
  form.phone = ''
  form.age = undefined
  form.remark = ''
  form.roleIds = []
  form.positionId = ''
  form.status = 'ENABLED'
  formRef.value?.clearValidate()
}

function openCreate(): void {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: UserView): void {
  dialogMode.value = 'edit'
  resetForm()
  form.uid = row.uid
  form.username = row.username
  form.name = row.name ?? ''
  form.nickname = row.nickname ?? ''
  form.department = row.department ?? ''
  form.email = row.email ?? ''
  form.phone = row.phone ?? ''
  form.age = row.age
  form.remark = row.remark ?? ''
  form.roleIds = [...(row.roleIds ?? [])]
  form.positionId = row.positionId ?? ''
  form.status = row.status
  dialogVisible.value = true
}

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      const payload: UserCreatePayload = {
        username: form.username.trim(),
        password: form.password,
        name: form.name.trim(),
        nickname: form.nickname || undefined,
        department: form.department || undefined,
        email: form.email || undefined,
        phone: form.phone || undefined,
        age: form.age,
        remark: form.remark || undefined,
        roleIds: form.roleIds.length ? [...form.roleIds] : undefined,
        positionId: form.positionId || undefined
      }
      await createUser(payload)
      ElMessage.success('新增成功')
    } else {
      const payload: UserUpdatePayload = {
        name: form.name.trim(),
        nickname: form.nickname || undefined,
        department: form.department || undefined,
        email: form.email || undefined,
        phone: form.phone || undefined,
        age: form.age,
        remark: form.remark || undefined,
        roleIds: [...form.roleIds],
        positionId: form.positionId || undefined,
        status: form.status
      }
      await updateUser(form.uid, payload)
      ElMessage.success('保存成功')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

/* ---------------- 行内操作 ---------------- */
async function handleToggleStatus(row: UserView): Promise<void> {
  const next: UserStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const actionText = next === 'ENABLED' ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${actionText}「${row.name || row.username}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: `确定${actionText}`,
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await updateUserStatus(row.uid, next)
  ElMessage.success(`${actionText}成功`)
  await loadData()
}

async function handleDelete(row: UserView): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.name || row.username}」吗？该操作会同时删除其薪资记录。`,
      '危险操作',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await deleteUser(row.uid)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && pagination.page > 1) {
    pagination.page -= 1
  }
  await loadData()
}

/* ---------------- 重置密码 ---------------- */
const passwordVisible = ref(false)
const passwordSubmitting = ref(false)
const passwordTarget = ref<UserView | null>(null)
const passwordForm = reactive({ newPassword: '', confirmPassword: '' })
const passwordFormRef = ref<FormInstance>()

const passwordRules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度 8-64 位', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: '密码需同时包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback: (error?: Error) => void) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

function openResetPassword(row: UserView): void {
  passwordTarget.value = row
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordVisible.value = true
  void passwordFormRef.value?.clearValidate()
}

async function handleResetPassword(): Promise<void> {
  if (!passwordTarget.value) return
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return
  passwordSubmitting.value = true
  try {
    await resetUserPassword(passwordTarget.value.uid, passwordForm.newPassword)
    ElMessage.success('密码已重置，该用户需重新登录')
    passwordVisible.value = false
  } finally {
    passwordSubmitting.value = false
  }
}

onMounted(async () => {
  await loadOptions()
  await loadData()
})
</script>

<template>
  <PageContainer title="人员管理" description="维护员工账号、角色与职位信息">
    <template #extra>
      <el-button v-if="has('user:create')" type="primary" :icon="Plus" @click="openCreate">
        新增人员
      </el-button>
    </template>

    <SearchBar :loading="loading" @search="handleSearch" @reset="handleReset">
      <el-form-item label="关键字">
        <el-input
          v-model="query.keyword"
          placeholder="姓名 / 用户名 / 部门 / 手机号"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="query.roleId" placeholder="全部角色" clearable style="width: 160px">
          <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="职位">
        <el-select v-model="query.positionId" placeholder="全部职位" clearable style="width: 160px">
          <el-option
            v-for="position in positionOptions"
            :key="position.id"
            :label="position.name"
            :value="position.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="正常" value="ENABLED" />
          <el-option label="已禁用" value="DISABLED" />
        </el-select>
      </el-form-item>
    </SearchBar>

    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column prop="username" label="登录名" min-width="120" />
      <el-table-column label="姓名" min-width="110">
        <template #default="{ row }">{{ row.name || '-' }}</template>
      </el-table-column>
      <el-table-column label="部门" min-width="120">
        <template #default="{ row }">{{ row.department || '-' }}</template>
      </el-table-column>
      <el-table-column label="角色" min-width="160">
        <template #default="{ row }">
          <el-tag
            v-for="(name, index) in row.roleNames ?? []"
            :key="index"
            size="small"
            class="mr-4"
            type="info"
          >
            {{ name }}
          </el-tag>
          <span v-if="!(row.roleNames ?? []).length">-</span>
        </template>
      </el-table-column>
      <el-table-column label="职位" min-width="120">
        <template #default="{ row }">{{ row.positionName || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'" size="small">
            {{ row.statusLabel || (row.status === 'ENABLED' ? '正常' : '已禁用') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最后登录" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.lastLoginAt, '从未登录') }}</template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="290" fixed="right" align="center">
        <template #default="{ row }">
          <el-button v-if="has('user:update')" link type="primary" :icon="Edit" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button
            v-if="has('user:update')"
            link
            :type="row.status === 'ENABLED' ? 'warning' : 'success'"
            @click="handleToggleStatus(row)"
          >
            {{ row.status === 'ENABLED' ? '禁用' : '启用' }}
          </el-button>
          <el-button
            v-if="has('user:reset-password')"
            link
            type="warning"
            :icon="Key"
            @click="openResetPassword(row)"
          >
            重置密码
          </el-button>
          <el-button v-if="has('user:delete')" link type="danger" :icon="Delete" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无人员数据" />
      </template>
    </el-table>

    <div class="table-footer">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="loadData"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- 新增 / 编辑 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增人员' : '编辑人员'"
      width="680px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="登录名" prop="username">
              <el-input
                v-model="form.username"
                :disabled="dialogMode === 'edit'"
                placeholder="字母开头，3-32 位"
              />
            </el-form-item>
          </el-col>
          <el-col v-if="dialogMode === 'create'" :span="12">
            <el-form-item label="初始密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="8-64 位，含字母和数字" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称">
              <el-input v-model="form.nickname" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门">
              <el-input v-model="form.department" placeholder="如：技术部" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位">
              <el-select v-model="form.positionId" placeholder="请选择职位" clearable style="width: 100%">
                <el-option
                  v-for="position in positionOptions"
                  :key="position.id"
                  :label="position.name"
                  :value="position.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色">
              <el-select
                v-model="form.roleIds"
                multiple
                collapse-tags
                collapse-tags-tooltip
                placeholder="可多选"
                style="width: 100%"
              >
                <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio value="ENABLED">正常</el-radio>
                <el-radio value="DISABLED">已禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年龄">
              <el-input-number v-model="form.age" :min="16" :max="100" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="passwordVisible" title="重置密码" width="420px" destroy-on-close>
      <p class="dialog-tip">
        正在为
        <b>{{ passwordTarget?.name || passwordTarget?.username }}</b>
        设置新密码，保存后该用户的登录状态会被强制失效。
      </p>
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="90px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSubmitting" @click="handleResetPassword">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<style scoped lang="scss">
.mr-4 {
  margin-right: 4px;
}

.table-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.dialog-tip {
  margin: 0 0 12px;
  color: var(--el-text-color-regular);
  font-size: 13px;
}
</style>
