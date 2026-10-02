<script setup lang="ts">
/**
 * 工作台：统计卡片 + 纯 CSS 图表（部门人数 / 薪资类型分布）+ 最近操作日志。
 * 图表不引入 echarts，用 Element Plus 进度条 / CSS 条实现，避免额外依赖。
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer.vue'
import {
  fetchPayTypeDistribution,
  fetchRecentAuditLogs,
  fetchStatsOverview,
  fetchUsersByDepartment
} from '@/api/stats'
import type { AuditLogView, NameValue, PayType, StatsOverview } from '@/api/types'
import {
  auditOutcomeLabel,
  auditOutcomeTagType,
  formatAmount,
  formatDateTime,
  formatInteger,
  payTypeLabel,
  toBarPercent,
  truncate
} from '@/utils/format'
import { usePermission } from '@/utils/permission'

defineOptions({ name: 'DashboardPage' })

const router = useRouter()
const { has } = usePermission()

const loading = ref(false)
const overview = ref<StatsOverview | null>(null)
const departmentStats = ref<NameValue[]>([])
const payTypeStats = ref<NameValue[]>([])
const recentLogs = ref<AuditLogView[]>([])

const PAY_TYPE_VALUES: PayType[] = ['MONTHLY', 'BONUS', 'ALLOWANCE', 'DEDUCTION']

const statCards = computed(() => [
  {
    key: 'userTotal',
    label: '人员总数',
    value: formatInteger(overview.value?.userTotal, '0'),
    suffix: '人',
    tone: 'primary',
    description: `其中启用 ${formatInteger(overview.value?.userEnabled, '0')} 人`
  },
  {
    key: 'userEnabled',
    label: '启用账号',
    value: formatInteger(overview.value?.userEnabled, '0'),
    suffix: '人',
    tone: 'success',
    description: `禁用 ${formatInteger(overview.value?.userDisabled, '0')} 人`
  },
  {
    key: 'positionTotal',
    label: '职位数量',
    value: formatInteger(overview.value?.positionTotal, '0'),
    suffix: '个',
    tone: 'warning',
    description: `部门 ${formatInteger(overview.value?.departmentCount, '0')} 个`
  },
  {
    key: 'salarySum',
    label: '薪资总额',
    value: formatAmount(overview.value?.salarySum, 2, '¥0.00'),
    suffix: '',
    tone: 'danger',
    description: `薪资记录 ${formatInteger(overview.value?.salaryTotal, '0')} 条`
  }
])

const departmentMax = computed(() =>
  departmentStats.value.reduce((max, item) => Math.max(max, item.value), 0)
)

const payTypeMax = computed(() => payTypeStats.value.reduce((max, item) => Math.max(max, item.value), 0))

const payTypeTotal = computed(() => payTypeStats.value.reduce((sum, item) => sum + item.value, 0))

/** 后端可能返回枚举名（MONTHLY）或中文标签，这里做兼容 */
function displayPayTypeName(name: string): string {
  return PAY_TYPE_VALUES.includes(name as PayType) ? payTypeLabel(name as PayType, name) : name
}

function departmentPercent(value: number): number {
  return toBarPercent(value, departmentMax.value)
}

function payTypePercent(value: number): number {
  return toBarPercent(value, payTypeMax.value)
}

function goAuditLogs(): void {
  void router.push('/audit-logs')
}

async function loadDashboard(): Promise<void> {
  loading.value = true
  const [overviewResult, departmentResult, payTypeResult, logResult] = await Promise.allSettled([
    fetchStatsOverview(),
    fetchUsersByDepartment(),
    fetchPayTypeDistribution(),
    fetchRecentAuditLogs(10)
  ])

  if (overviewResult.status === 'fulfilled') overview.value = overviewResult.value
  if (departmentResult.status === 'fulfilled') departmentStats.value = departmentResult.value ?? []
  if (payTypeResult.status === 'fulfilled') payTypeStats.value = payTypeResult.value ?? []
  if (logResult.status === 'fulfilled') recentLogs.value = logResult.value ?? []
  loading.value = false
}

onMounted(loadDashboard)
</script>

<template>
  <PageContainer title="工作台" description="人员、职位、薪资与安全审计的整体概览">
    <template #extra>
      <el-button :icon="Refresh" :loading="loading" @click="loadDashboard">刷新数据</el-button>
    </template>

    <div v-loading="loading" class="dashboard">
      <el-row :gutter="16">
        <el-col v-for="card in statCards" :key="card.key" :xs="24" :sm="12" :lg="6">
          <div class="stat-card" :class="`stat-card--${card.tone}`">
            <div class="stat-card__label">{{ card.label }}</div>
            <div class="stat-card__value">
              {{ card.value }}
              <small v-if="card.suffix">{{ card.suffix }}</small>
            </div>
            <div class="stat-card__desc">{{ card.description }}</div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="dashboard__row">
        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="pms-card">
            <template #header>
              <div class="pms-card__title">部门人数分布</div>
            </template>

            <div v-if="departmentStats.length" class="chart-list">
              <div v-for="item in departmentStats" :key="item.name" class="chart-bar">
                <div class="chart-bar__head">
                  <span class="chart-bar__name">{{ truncate(item.name, 18) }}</span>
                  <span class="chart-bar__value">{{ formatInteger(item.value, '0') }} 人</span>
                </div>
                <div class="chart-bar__track">
                  <div class="chart-bar__fill" :style="{ width: `${departmentPercent(item.value)}%` }" />
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无部门数据" :image-size="80" />
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="pms-card">
            <template #header>
              <div class="pms-card__title">薪资类型分布</div>
            </template>

            <div v-if="payTypeStats.length" class="chart-list">
              <div v-for="item in payTypeStats" :key="item.name" class="chart-bar">
                <div class="chart-bar__head">
                  <span class="chart-bar__name">{{ displayPayTypeName(item.name) }}</span>
                  <span class="chart-bar__value">
                    {{ formatInteger(item.value, '0') }} 条
                    <em v-if="payTypeTotal">（{{ ((item.value / payTypeTotal) * 100).toFixed(0) }}%）</em>
                  </span>
                </div>
                <div class="chart-bar__track">
                  <div
                    class="chart-bar__fill chart-bar__fill--alt"
                    :style="{ width: `${payTypePercent(item.value)}%` }"
                  />
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无薪资数据" :image-size="80" />
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="pms-card dashboard__row">
        <template #header>
          <div class="pms-card__title">
            <span>最近操作日志</span>
            <el-button
              v-if="has('audit:view')"
              link
              type="primary"
              @click="goAuditLogs"
            >
              查看全部
            </el-button>
          </div>
        </template>

        <el-table :data="recentLogs" size="default" stripe :border="false" empty-text="暂无操作日志">
          <el-table-column label="时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="username" label="操作人" min-width="110" />
          <el-table-column prop="action" label="操作" min-width="130" show-overflow-tooltip />
          <el-table-column label="请求" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="dashboard__method">{{ row.method }}</span>
              <span>{{ row.path }}</span>
            </template>
          </el-table-column>
          <el-table-column label="结果" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="auditOutcomeTagType(row.outcome)" size="small" effect="light">
                {{ auditOutcomeLabel(row.outcome) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="耗时" width="100" align="right">
            <template #default="{ row }">{{ formatInteger(row.elapsedMs, '0') }} ms</template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.dashboard__row {
  margin-top: 16px;
}

.stat-card {
  position: relative;
  padding: 18px 20px;
  margin-bottom: 16px;
  overflow: hidden;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 2px rgba(23, 43, 77, 0.06), 0 8px 20px rgba(23, 43, 77, 0.05);

  &::after {
    position: absolute;
    top: 0;
    left: 0;
    width: 4px;
    height: 100%;
    content: '';
    background: var(--pms-primary, #2f6fed);
  }

  &--success::after {
    background: #35b37e;
  }

  &--warning::after {
    background: #e6a23c;
  }

  &--danger::after {
    background: #f56c6c;
  }
}

.stat-card__label {
  font-size: 13px;
  color: #7a8699;
}

.stat-card__value {
  margin: 10px 0 6px;
  font-size: 26px;
  font-weight: 600;
  color: #1f2a3c;

  small {
    margin-left: 4px;
    font-size: 13px;
    font-weight: 400;
    color: #7a8699;
  }
}

.stat-card__desc {
  font-size: 12px;
  color: #a2abbd;
}

.pms-card__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 15px;
  font-weight: 600;
  color: #1f2a3c;
}

.chart-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.chart-bar__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 13px;
  color: #4b5a75;
}

.chart-bar__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chart-bar__value {
  flex: none;
  margin-left: 12px;
  color: #7a8699;

  em {
    font-style: normal;
    color: #a2abbd;
  }
}

.chart-bar__track {
  height: 10px;
  overflow: hidden;
  background: #eef1f7;
  border-radius: 6px;
}

.chart-bar__fill {
  height: 100%;
  background: linear-gradient(90deg, #2f6fed, #5b95f8);
  border-radius: 6px;
  transition: width 0.4s ease;

  &--alt {
    background: linear-gradient(90deg, #35b37e, #6ed3a6);
  }
}

.dashboard__method {
  display: inline-block;
  min-width: 54px;
  margin-right: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #2f6fed;
}
</style>
