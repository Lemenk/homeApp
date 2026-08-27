<template>
  <div class="stats-page">
    <el-row :gutter="16" class="summary">
      <el-col :span="8">
        <el-card shadow="never">
          <div class="sum-label">总收入</div>
          <div class="sum-value income">¥ {{ totals.income.toFixed(2) }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <div class="sum-label">总支出</div>
          <div class="sum-value expense">¥ {{ totals.expense.toFixed(2) }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <div class="sum-label">结余</div>
          <div class="sum-value balance">¥ {{ (totals.income - totals.expense).toFixed(2) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-head">
          <span>收支趋势</span>
          <el-radio-group v-model="groupBy" size="small" @change="load">
            <el-radio-button value="month">按月</el-radio-button>
            <el-radio-button value="day">按日</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="trendEl" class="chart" />
      <el-empty v-if="!trendData.length" description="暂无数据，先去记几笔账吧" />
    </el-card>

    <el-row :gutter="16" class="cat-row">
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-head"><span>支出分类占比</span></div>
          </template>
          <div ref="expensePieEl" class="chart" />
          <el-empty v-if="!expenseData.length" description="暂无支出数据" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-head"><span>收入分类占比</span></div>
          </template>
          <div ref="incomePieEl" class="chart" />
          <el-empty v-if="!incomeData.length" description="暂无收入数据" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { trend, categoryStat } from '@/api/statistics'
import type { TrendPoint, CategoryStat } from '@/api/statistics'
import { useLedgerStore } from '@/stores/ledger'

const ledgerStore = useLedgerStore()
const groupBy = ref<'day' | 'month'>('month')
const trendData = ref<TrendPoint[]>([])
const expenseData = ref<CategoryStat[]>([])
const incomeData = ref<CategoryStat[]>([])
const trendEl = ref<HTMLDivElement>()
const expensePieEl = ref<HTMLDivElement>()
const incomePieEl = ref<HTMLDivElement>()

const totals = computed(() => {
  const expense = trendData.value.reduce((s, p) => s + p.expense, 0)
  const income = trendData.value.reduce((s, p) => s + p.income, 0)
  return { expense, income }
})

let charts: echarts.ECharts[] = []

function render() {
  charts.forEach((c) => c.dispose())
  charts = []
  if (!trendEl.value || !expensePieEl.value || !incomePieEl.value) return

  // 趋势图
  const t = echarts.init(trendEl.value)
  charts.push(t)
  t.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['收入', '支出'] },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: trendData.value.map((p) => p.period) },
    yAxis: { type: 'value' },
    series: [
      { name: '收入', type: 'line', smooth: true, data: trendData.value.map((p) => p.income), itemStyle: { color: '#67c23a' } },
      { name: '支出', type: 'line', smooth: true, data: trendData.value.map((p) => p.expense), itemStyle: { color: '#f56c6c' } },
    ],
  })

  renderPie(expensePieEl.value, expenseData.value, '支出分类占比')
  renderPie(incomePieEl.value, incomeData.value, '收入分类占比')
}

function renderPie(el: HTMLDivElement, data: CategoryStat[], _title: string) {
  const c = echarts.init(el)
  charts.push(c)
  c.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    series: [
      {
        type: 'pie',
        radius: ['35%', '65%'],
        data: data.map((d) => ({ name: `${d.categoryName}`, value: d.amount })),
        label: { formatter: '{b} {d}%' },
      },
    ],
  })
}

async function load() {
  if (!ledgerStore.currentLedgerId) return
  const [t, exp, inc] = await Promise.all([
    trend(ledgerStore.currentLedgerId, { groupBy: groupBy.value }),
    categoryStat(ledgerStore.currentLedgerId, { type: 'expense' }),
    categoryStat(ledgerStore.currentLedgerId, { type: 'income' }),
  ])
  trendData.value = t
  expenseData.value = exp
  incomeData.value = inc
  render()
}

onMounted(async () => {
  await ledgerStore.fetch()
  await load()
  window.addEventListener('resize', () => charts.forEach((c) => c.resize()))
})
</script>

<style scoped>
.stats-page {
  padding: 20px;
  max-width: 1100px;
  margin: 0 auto;
}
.summary {
  margin-bottom: 16px;
}
.sum-label {
  color: #909399;
  font-size: 13px;
}
.sum-value {
  font-size: 24px;
  font-weight: 700;
  margin-top: 6px;
}
.sum-value.income {
  color: var(--el-color-success);
}
.sum-value.expense {
  color: var(--el-color-danger);
}
.sum-value.balance {
  color: var(--el-color-primary);
}
.chart-card {
  margin-bottom: 16px;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chart {
  height: 320px;
}
.cat-row .chart {
  height: 300px;
}
</style>
