<script setup lang="ts">
/**
 * 审计日志：查询系统操作记录，支持按操作人、动作、结果与时间范围筛选，可清空日志。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Refresh } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import SearchBar from '@/components/SearchBar.vue'
import { clearAuditLogs, fetchAuditLogs } from '@/api/audit'
import type { AuditLogView, AuditOutcome } from '@/api/types'
import { auditOutcomeLabel, auditOutcomeTagType, formatDateTime, toEndOfDay, toStartOfDay } from '@/utils/format'
import { usePermission } from '@/utils/permission'

defineOptions({ name: 'AuditLogPage' })

const { has } = usePermission()

/** 后端 @Audited 中使用的动作名称，供筛选下拉选择 */
const ACTION_OPTIONS = [
  '用户登录',
  '修改密码',
  '新增人员',
  '修改人员',
  '删除人员',
  '重置密码',
  '新增角色',
  '修改角色',
  '删除角色',
  '新增职位',
  '修改职位',
  '删除职位',
  '新增薪资',
  '修改薪资',
  '删除薪资',
  '清空审计日志'
]

const query = reactive({
  username: '',
  action: '',
  outcome: '' as AuditOutcome | '',
  /** 日期范围（yyyy-MM-dd） */
  dateRange: null as [string, string] | null
})

const pagination = reactive({ page: 1, size: 20, total: 0 })
const loading = ref(false)
const records = ref<AuditLogView[]>([])

/** 是否处于「无筛选条件的全量查询」——用于提示清空按钮的影响范围 */
const hasFilter = computed(
  () =>
    Boolean(query.username || query.action || query.outcome) ||
    Boolean(query.dateRange && query.dateRange.length === 2)
)

function methodTagType(method?: string): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  switch ((method || '').toUpperCase()) {
    case 'POST':
      return 'success'
    case 'PUT':
    case 'PATCH':
      return 'warning'
    case 'DELETE':
      return 'danger'
    case 'GET':
      return 'info'
    default:
      return 'primary'
  }
}

function queryParams() {
  return {
    page: pagination.page,
    size: pagination.size,
    username: query.username || undefined,
    action: query.action || undefined,
    outcome: query.outcome || undefined,
    from: query.dateRange?.[0] ? toStartOfDay(query.dateRange[0]) : undefined,
    to: query.dateRange?.[1] ? toEndOfDay(query.dateRange[1]) : undefined
  }
}

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const result = await fetchAuditLogs(queryParams())
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
  query.username = ''
  query.action = ''
  query.outcome = ''
  query.dateRange = null
  handleSearch()
}

function handleSizeChange(size: number): void {
  pagination.size = size
  pagination.page = 1
  void loadData()
}

async function handleClear(): Promise<void> {
  const tip = hasFilter.value
    ? '当前筛选条件不会影响清理范围：后端将清空全部审计日志，确定继续吗？'
    : '确定要清空全部审计日志吗？该操作不可恢复。'
  try {
    await ElMessageBox.confirm(tip, '危险操作', {
      type: 'warning',
      confirmButtonText: '确定清空',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  const removed = await clearAuditLogs()
  ElMessage.success(removed ? `已清空 ${removed} 条审计日志` : '审计日志已清空')
  pagination.page = 1
  await loadData()
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <PageContainer title="审计日志" description="记录人员、角色、职位与薪资的关键操作，便于追溯">
    <template #extra>
      <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
      <el-button v-if="has('audit:clear')" type="danger" :icon="Delete" @click="handleClear">
        清空日志
      </el-button>
    </template>

    <SearchBar :loading="loading" @search="handleSearch" @reset="handleReset">
      <el-form-item label="操作人">
        <el-input
          v-model="query.username"
          placeholder="登录名关键字"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item label="操作动作">
        <el-select v-model="query.action" placeholder="全部动作" clearable filterable style="width: 180px">
          <el-option v-for="action in ACTION_OPTIONS" :key="action" :label="action" :value="action" />
        </el-select>
      </el-form-item>
      <el-form-item label="结果">
        <el-select v-model="query.outcome" placeholder="全部结果" clearable style="width: 130px">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILURE" />
        </el-select>
      </el-form-item>
      <el-form-item label="操作时间">
        <el-date-picker
          v-model="query.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
        />
      </el-form-item>
    </SearchBar>

    <el-table v-loading="loading" :data="records" border stripe>
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="detail">
            <div class="detail__item"><span>日志 ID：</span>{{ row.id }}</div>
            <div class="detail__item"><span>操作人 UID：</span>{{ row.uid || '-' }}</div>
            <div class="detail__item"><span>耗时：</span>{{ row.elapsedMs }} ms</div>
            <div class="detail__item detail__item--full">
              <span>详情：</span>{{ row.detail || '无' }}
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作人" width="130">
        <template #default="{ row }">{{ row.username || '匿名' }}</template>
      </el-table-column>
      <el-table-column label="动作" min-width="130">
        <template #default="{ row }">{{ row.action || '-' }}</template>
      </el-table-column>
      <el-table-column label="请求方法" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="methodTagType(row.method)" size="small">{{ row.method || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="请求路径" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">{{ row.path || '-' }}</template>
      </el-table-column>
      <el-table-column label="来源 IP" width="140">
        <template #default="{ row }">{{ row.ip || '-' }}</template>
      </el-table-column>
      <el-table-column label="结果" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="auditOutcomeTagType(row.outcome)" size="small">
            {{ auditOutcomeLabel(row.outcome) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="耗时" width="100" align="right">
        <template #default="{ row }">{{ row.elapsedMs }} ms</template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无审计日志" />
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
  </PageContainer>
</template>

<style scoped lang="scss">
.table-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.detail {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 24px;
  padding: 4px 16px 8px;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.detail__item span {
  color: var(--el-text-color-secondary);
}

.detail__item--full {
  flex: 1 0 100%;
  word-break: break-all;
}
</style>
