<script setup lang="ts">
/**
 * 职位管理：职位分页列表 + 新增/编辑/删除。
 * 职位被人员引用时后端返回 code 4002，这里直接展示后端提示。
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import SearchBar from '@/components/SearchBar.vue'
import { createPosition, deletePosition, fetchPositions, updatePosition } from '@/api/position'
import type { PositionPayload, PositionView } from '@/api/types'
import { formatDateTime } from '@/utils/format'
import { usePermission } from '@/utils/permission'

defineOptions({ name: 'PositionPage' })

const { has } = usePermission()

const query = reactive({ keyword: '' })
const pagination = reactive({ page: 1, size: 20, total: 0 })

const loading = ref(false)
const records = ref<PositionView[]>([])

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const result = await fetchPositions({
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
interface PositionFormState {
  id: string
  code: string
  name: string
  level: string
  description: string
  sortOrder: number
}

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<PositionFormState>({
  id: '',
  code: '',
  name: '',
  level: '',
  description: '',
  sortOrder: 0
})

const rules: FormRules = {
  code: [
    { required: true, message: '请输入职位编码', trigger: 'blur' },
    {
      pattern: /^[A-Z][A-Z0-9_]{1,31}$/,
      message: '大写字母开头，2-32 位大写字母/数字/下划线',
      trigger: 'blur'
    }
  ],
  name: [{ required: true, message: '请输入职位名称', trigger: 'blur' }]
}

function resetForm(): void {
  form.id = ''
  form.code = ''
  form.name = ''
  form.level = ''
  form.description = ''
  form.sortOrder = 0
  formRef.value?.clearValidate()
}

function openCreate(): void {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: PositionView): void {
  dialogMode.value = 'edit'
  resetForm()
  form.id = row.id
  form.code = row.code
  form.name = row.name
  form.level = row.level ?? ''
  form.description = row.description ?? ''
  form.sortOrder = row.sortOrder ?? 0
  dialogVisible.value = true
}

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload: PositionPayload = {
      code: form.code.trim().toUpperCase(),
      name: form.name.trim(),
      level: form.level || undefined,
      description: form.description || undefined,
      sortOrder: form.sortOrder
    }
    if (dialogMode.value === 'create') {
      await createPosition(payload)
      ElMessage.success('新增成功')
    } else {
      await updatePosition(form.id, payload)
      ElMessage.success('保存成功')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: PositionView): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定要删除职位「${row.name}」吗？`, '危险操作', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deletePosition(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && pagination.page > 1) {
    pagination.page -= 1
  }
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <PageContainer title="职位管理" description="维护组织内的职位与职级，供人员与薪资关联">
    <template #extra>
      <el-button v-if="has('position:create')" type="primary" :icon="Plus" @click="openCreate">
        新增职位
      </el-button>
    </template>

    <SearchBar :loading="loading" @search="handleSearch" @reset="handleReset">
      <el-form-item label="关键字">
        <el-input
          v-model="query.keyword"
          placeholder="职位名称或编码"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
        />
      </el-form-item>
    </SearchBar>

    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column prop="code" label="职位编码" min-width="140" />
      <el-table-column prop="name" label="职位名称" min-width="160" />
      <el-table-column label="职级" width="110" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.level" size="small" type="info">{{ row.level }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="描述" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.description || '-' }}</template>
      </el-table-column>
      <el-table-column label="排序" width="90" align="center">
        <template #default="{ row }">{{ row.sortOrder ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="在岗人数" width="110" align="center">
        <template #default="{ row }">{{ row.userCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right" align="center">
        <template #default="{ row }">
          <el-button v-if="has('position:update')" link type="primary" :icon="Edit" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button v-if="has('position:delete')" link type="danger" :icon="Delete" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无职位数据" />
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
      :title="dialogMode === 'create' ? '新增职位' : '编辑职位'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="职位编码" prop="code">
          <el-input
            v-model="form.code"
            :disabled="dialogMode === 'edit'"
            placeholder="如：BACKEND_DEV"
          />
        </el-form-item>
        <el-form-item label="职位名称" prop="name">
          <el-input v-model="form.name" placeholder="如：后端开发工程师" />
        </el-form-item>
        <el-form-item label="职级">
          <el-input v-model="form.level" placeholder="如 P5 / M2（可选）" maxlength="16" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" />
          <span class="form-hint">数值越小越靠前</span>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="255" show-word-limit />
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
.table-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.form-hint {
  margin-left: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
