<script setup lang="ts">
/**
 * 角色权限：角色分页列表 + 权限目录勾选。
 * 内置角色（builtIn）不允许修改权限与删除，后端会返回 code 4002。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import SearchBar from '@/components/SearchBar.vue'
import {
  createRole,
  deleteRole,
  fetchPermissionCatalog,
  fetchRoles,
  updateRole
} from '@/api/role'
import type { PermissionGroup, RoleCreatePayload, RoleUpdatePayload, RoleView } from '@/api/types'
import { formatDateTime } from '@/utils/format'
import { usePermission } from '@/utils/permission'

defineOptions({ name: 'RolePage' })

const { has } = usePermission()

const query = reactive({ keyword: '' })
const pagination = reactive({ page: 1, size: 20, total: 0 })

const loading = ref(false)
const records = ref<RoleView[]>([])
const permissionGroups = ref<PermissionGroup[]>([])

const totalPermissionCount = computed(() =>
  permissionGroups.value.reduce((sum, group) => sum + group.items.length, 0)
)

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const result = await fetchRoles({
      page: pagination.page,
      size: pagination.size,
      keyword: query.keyword || undefined
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
  handleSearch()
}

function handleSizeChange(size: number): void {
  pagination.size = size
  pagination.page = 1
  void loadData()
}

/* ---------------- 新增 / 编辑 ---------------- */
interface RoleFormState {
  id: string
  code: string
  name: string
  description: string
  permissions: string[]
  builtIn: boolean
}

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<RoleFormState>({
  id: '',
  code: '',
  name: '',
  description: '',
  permissions: [],
  builtIn: false
})

const rules: FormRules = {
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    {
      pattern: /^[A-Z][A-Z0-9_]{1,31}$/,
      message: '大写字母开头，2-32 位大写字母/数字/下划线',
      trigger: 'blur'
    }
  ],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  permissions: [
    {
      type: 'array',
      required: true,
      min: 1,
      message: '至少选择一个权限',
      trigger: 'change'
    }
  ]
}

function resetForm(): void {
  form.id = ''
  form.code = ''
  form.name = ''
  form.description = ''
  form.permissions = []
  form.builtIn = false
  formRef.value?.clearValidate()
}

function openCreate(): void {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: RoleView): void {
  dialogMode.value = 'edit'
  resetForm()
  form.id = row.id
  form.code = row.code
  form.name = row.name
  form.description = row.description ?? ''
  form.permissions = [...(row.permissions ?? [])]
  form.builtIn = row.builtIn
  dialogVisible.value = true
}

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      const payload: RoleCreatePayload = {
        code: form.code.trim().toUpperCase(),
        name: form.name.trim(),
        description: form.description || undefined,
        permissions: [...form.permissions]
      }
      await createRole(payload)
      ElMessage.success('新增成功')
    } else {
      const payload: RoleUpdatePayload = {
        name: form.name.trim(),
        description: form.description || undefined,
        permissions: [...form.permissions]
      }
      await updateRole(form.id, payload)
      ElMessage.success('保存成功')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: RoleView): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定要删除角色「${row.name}」吗？`, '危险操作', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && pagination.page > 1) {
    pagination.page -= 1
  }
  await loadData()
}

onMounted(async () => {
  permissionGroups.value = await fetchPermissionCatalog().catch(() => [])
  await loadData()
})
</script>

<template>
  <PageContainer title="角色权限" description="维护角色及其权限范围，权限决定菜单与操作可见性">
    <template #extra>
      <el-button v-if="has('role:create')" type="primary" :icon="Plus" @click="openCreate">
        新增角色
      </el-button>
    </template>

    <SearchBar :loading="loading" @search="handleSearch" @reset="handleReset">
      <el-form-item label="关键字">
        <el-input
          v-model="query.keyword"
          placeholder="角色名称或编码"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
      </el-form-item>
    </SearchBar>

    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column prop="code" label="角色编码" min-width="140" />
      <el-table-column prop="name" label="角色名称" min-width="140" />
      <el-table-column label="描述" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.description || '-' }}</template>
      </el-table-column>
      <el-table-column label="权限数" width="100" align="center">
        <template #default="{ row }">{{ (row.permissions ?? []).length }}</template>
      </el-table-column>
      <el-table-column label="成员数" width="100" align="center">
        <template #default="{ row }">{{ row.userCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.builtIn ? 'warning' : 'info'" size="small">
            {{ row.builtIn ? '内置' : '自定义' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right" align="center">
        <template #default="{ row }">
          <el-button v-if="has('role:update')" link type="primary" :icon="Edit" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button
            v-if="has('role:delete')"
            link
            type="danger"
            :icon="Delete"
            :disabled="row.builtIn"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无角色数据" />
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增角色' : '编辑角色'"
      width="720px"
      destroy-on-close
    >
      <el-alert
        v-if="form.builtIn"
        type="warning"
        :closable="false"
        show-icon
        title="内置角色不允许修改权限（后端会拒绝该操作），仅可修改名称与描述。"
        class="mb-12"
      />
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色编码" prop="code">
          <el-input
            v-model="form.code"
            :disabled="dialogMode === 'edit'"
            placeholder="如：HR_MANAGER"
          />
        </el-form-item>
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="如：人力资源主管" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="权限" prop="permissions">
          <div class="permission-panel">
            <div class="permission-panel__hint">
              共 {{ totalPermissionCount }} 项权限，已选 {{ form.permissions.length }} 项
            </div>
            <el-checkbox-group v-model="form.permissions" :disabled="form.builtIn">
              <div v-for="group in permissionGroups" :key="group.group" class="permission-group">
                <div class="permission-group__title">{{ group.group }}</div>
                <el-checkbox
                  v-for="item in group.items"
                  :key="item.code"
                  :value="item.code"
                  class="permission-group__item"
                >
                  {{ item.label }}
                  <span class="permission-group__code">{{ item.code }}</span>
                </el-checkbox>
              </div>
            </el-checkbox-group>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<style scoped lang="scss">
.mb-12 {
  margin-bottom: 12px;
}

.table-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.permission-panel {
  width: 100%;
  max-height: 320px;
  padding: 8px 12px;
  overflow-y: auto;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;

  &__hint {
    margin-bottom: 8px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
}

.permission-group {
  margin-bottom: 8px;

  &__title {
    margin-bottom: 4px;
    font-weight: 600;
    font-size: 13px;
  }

  &__item {
    min-width: 200px;
    margin-right: 12px;
  }

  &__code {
    margin-left: 4px;
    color: var(--el-text-color-placeholder);
    font-size: 12px;
  }
}
</style>
