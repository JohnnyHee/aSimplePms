<script setup lang="ts">
/**
 * 薪资管理：按人员/类型/状态查询薪资记录，支持新增、调整与删除。
 * 说明：同一人员只允许一条「生效中」记录，保存后后端会自动归档旧记录。
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import SearchBar from '@/components/SearchBar.vue'
import { createSalary, deleteSalary, fetchSalaries, updateSalary } from '@/api/salary'
import { fetchPositionOptions } from '@/api/position'
import { fetchUsers } from '@/api/user'
import type { PayType, PositionView, SalaryPayload, SalaryStatus, SalaryView, UserView } from '@/api/types'
import {
  PAY_TYPE_OPTIONS,
  SALARY_STATUS_OPTIONS,
  formatAmount,
  formatDate,
  formatDateTime,
  payTypeLabel,
  payTypeTagType,
  salaryStatusLabel,
  salaryStatusTagType
} from '@/utils/format'
import { usePermission } from '@/utils/permission'

defineOptions({ name: 'SalaryPage' })

const { has } = usePermission()

const query = reactive({
  uid: '',
  payType: '' as PayType | '',
  status: '' as SalaryStatus | ''
})

const pagination = reactive({ page: 1, size: 20, total: 0 })

const loading = ref(false)
const records = ref<SalaryView[]>([])
const positionOptions = ref<PositionView[]>([])
/** 人员下拉候选（按关键字远程搜索） */
const userOptions = ref<UserView[]>([])
const userLoading = ref(false)

async function searchUsers(keyword: string): Promise<void> {
  userLoading.value = true
  try {
    const result = await fetchUsers({ page: 1, size: 20, keyword: keyword || undefined })
    userOptions.value = result?.records ?? []
  } catch {
    userOptions.value = []
  } finally {
    userLoading.value = false
  }
}

function userLabel(user: UserView): string {
  return user.department ? `${user.name}（${user.department}）` : user.name
}

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const result = await fetchSalaries({
      page: pagination.page,
      size: pagination.size,
      uid: query.uid || undefined,
      payType: query.payType || undefined,
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
  query.uid = ''
  query.payType = ''
  query.status = ''
  handleSearch()
}

function handleSizeChange(size: number): void {
  pagination.size = size
  pagination.page = 1
  void loadData()
}

/* ---------------- 新增 / 编辑 ---------------- */
interface SalaryFormState {
  id: string
  uid: string
  positionId: string
  amount: number | undefined
  payType: PayType
  status: SalaryStatus
  effectiveRange: [string, string] | null
  remark: string
}

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<SalaryFormState>({
  id: '',
  uid: '',
  positionId: '',
  amount: undefined,
  payType: 'MONTHLY',
  status: 'ACTIVE',
  effectiveRange: null,
  remark: ''
})

const rules: FormRules = {
  uid: [{ required: true, message: '请选择人员', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  payType: [{ required: true, message: '请选择薪资类型', trigger: 'change' }]
}

function resetForm(): void {
  form.id = ''
  form.uid = ''
  form.positionId = ''
  form.amount = undefined
  form.payType = 'MONTHLY'
  form.status = 'ACTIVE'
  form.effectiveRange = null
  form.remark = ''
  formRef.value?.clearValidate()
}

function openCreate(): void {
  dialogMode.value = 'create'
  resetForm()
  if (userOptions.value.length === 0) {
    void searchUsers('')
  }
  dialogVisible.value = true
}

function openEdit(row: SalaryView): void {
  dialogMode.value = 'edit'
  resetForm()
  form.id = row.id
  form.uid = row.uid
  form.positionId = row.positionId ?? ''
  form.amount = row.amount
  form.payType = row.payType
  form.status = row.status
  form.effectiveRange =
    row.effectiveFrom && row.effectiveTo ? [row.effectiveFrom, row.effectiveTo] : null
  form.remark = row.remark ?? ''
  // 编辑时确保当前人员在下拉里有选项，避免显示为空
  if (!userOptions.value.some((user) => user.uid === row.uid)) {
    userOptions.value = [
      {
        uid: row.uid,
        username: row.uid,
        name: row.userName || row.uid,
        status: 'ENABLED',
        roleIds: [],
        roleNames: []
      },
      ...userOptions.value
    ]
  }
  dialogVisible.value = true
}

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload: SalaryPayload = {
      uid: form.uid,
      positionId: form.positionId || undefined,
      amount: Number(form.amount ?? 0),
      payType: form.payType,
      status: form.status,
      effectiveFrom: form.effectiveRange?.[0] || undefined,
      effectiveTo: form.effectiveRange?.[1] || undefined,
      remark: form.remark || undefined
    }
    if (dialogMode.value === 'create') {
      await createSalary(payload)
      ElMessage.success('新增成功')
    } else {
      await updateSalary(form.id, payload)
      ElMessage.success('保存成功')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: SalaryView): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.userName || row.uid}」的这条薪资记录吗？`,
      '危险操作',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await deleteSalary(row.id)
  ElMessage.success('删除成功')
  if (records.value.length === 1 && pagination.page > 1) {
    pagination.page -= 1
  }
  await loadData()
}

onMounted(async () => {
  positionOptions.value = await fetchPositionOptions().catch(() => [])
  await Promise.all([loadData(), searchUsers('')])
})
</script>

<template>
  <PageContainer title="薪资管理" description="维护人员薪资档案，同一人员仅保留一条生效中的记录">
    <template #extra>
      <el-button v-if="has('salary:create')" type="primary" :icon="Plus" @click="openCreate">
        新增薪资
      </el-button>
    </template>

    <SearchBar :loading="loading" @search="handleSearch" @reset="handleReset">
      <el-form-item label="人员">
        <el-select
          v-model="query.uid"
          filterable
          remote
          clearable
          reserve-keyword
          placeholder="按姓名搜索"
          :remote-method="searchUsers"
          :loading="userLoading"
          style="width: 220px"
        >
          <el-option v-for="user in userOptions" :key="user.uid" :label="userLabel(user)" :value="user.uid" />
        </el-select>
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.payType" placeholder="全部类型" clearable style="width: 150px">
          <el-option
            v-for="option in PAY_TYPE_OPTIONS"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option
            v-for="option in SALARY_STATUS_OPTIONS"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
    </SearchBar>

    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column label="人员" min-width="140">
        <template #default="{ row }">{{ row.userName || row.uid }}</template>
      </el-table-column>
      <el-table-column label="职位" min-width="130">
        <template #default="{ row }">{{ row.positionName || '-' }}</template>
      </el-table-column>
      <el-table-column label="金额" width="150" align="right">
        <template #default="{ row }">
          <span class="amount">{{ formatAmount(row.amount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="payTypeTagType(row.payType)" size="small">
            {{ row.payTypeLabel || payTypeLabel(row.payType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="salaryStatusTagType(row.status)" size="small">
            {{ row.statusLabel || salaryStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="生效期间" min-width="200">
        <template #default="{ row }">
          {{ formatDate(row.effectiveFrom, '—') }} ~ {{ formatDate(row.effectiveTo, '至今') }}
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.remark || '-' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right" align="center">
        <template #default="{ row }">
          <el-button v-if="has('salary:update')" link type="primary" :icon="Edit" @click="openEdit(row)">
            编辑
          </el-button>
          <el-button v-if="has('salary:delete')" link type="danger" :icon="Delete" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无薪资数据" />
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
      :title="dialogMode === 'create' ? '新增薪资' : '编辑薪资'"
      width="600px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="人员" prop="uid">
          <el-select
            v-model="form.uid"
            filterable
            remote
            reserve-keyword
            placeholder="按姓名搜索"
            :remote-method="searchUsers"
            :loading="userLoading"
            :disabled="dialogMode === 'edit'"
            style="width: 100%"
          >
            <el-option v-for="user in userOptions" :key="user.uid" :label="userLabel(user)" :value="user.uid" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联职位">
          <el-select v-model="form.positionId" placeholder="可选" clearable style="width: 100%">
            <el-option
              v-for="position in positionOptions"
              :key="position.id"
              :label="position.name"
              :value="position.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number
            v-model="form.amount"
            :min="0"
            :max="9999999999"
            :precision="2"
            :step="100"
            controls-position="right"
            style="width: 220px"
          />
          <span class="form-hint">单位：元</span>
        </el-form-item>
        <el-form-item label="类型" prop="payType">
          <el-select v-model="form.payType" style="width: 220px">
            <el-option
              v-for="option in PAY_TYPE_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="option in SALARY_STATUS_OPTIONS"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="生效期间">
          <el-date-picker
            v-model="form.effectiveRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="生效日期"
            end-placeholder="结束日期（可空）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
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

.amount {
  font-weight: 600;
  color: var(--el-color-danger);
}

.form-hint {
  margin-left: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
