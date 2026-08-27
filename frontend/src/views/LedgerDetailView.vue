<template>
  <div class="ledger-detail">
    <el-card v-if="ledger">
      <div class="head">
        <div>
          <h2>{{ ledger.name }}</h2>
          <el-tag :type="ledger.type === 'public' ? 'primary' : 'info'" size="small">
            {{ ledger.type === 'public' ? '公共账本' : '个人账本' }}
          </el-tag>
          <span class="count"> {{ ledger.memberCount }} 人</span>
        </div>
        <div class="head-actions">
          <el-button v-if="isCreator" @click="$router.push(`/ledgers/${ledger.id}/settings`)">账本管理</el-button>
          <el-button type="primary" @click="showForm = true">＋ 记一笔</el-button>
        </div>
      </div>
      <el-divider />

      <!-- 筛选栏 -->
      <div class="filters">
        <el-radio-group v-model="filters.type" size="small" @change="load">
          <el-radio-button :value="undefined">全部</el-radio-button>
          <el-radio-button value="expense">支出</el-radio-button>
          <el-radio-button value="income">收入</el-radio-button>
          <el-radio-button value="transfer">转账</el-radio-button>
        </el-radio-group>
        <div class="filter-right">
          <el-date-picker
            v-model="filters.range"
            type="daterange"
            size="small"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="load"
          />
          <el-input
            v-model="filters.keyword"
            size="small"
            placeholder="搜索备注/分类"
            clearable
            style="width: 160px"
            @keyup.enter="load"
            @clear="load"
          />
        </div>
      </div>

      <!-- 账单列表 -->
      <template v-if="bills.length">
        <div v-for="g in grouped" :key="g.date" class="day-group">
          <div class="day-head">
            <span class="day-date">{{ g.date }}</span>
            <span class="day-summary">
              支出 {{ g.expense }}　收入 {{ g.income }}
            </span>
          </div>
          <div v-for="b in g.list" :key="b.id" class="bill-row" @click="$router.push(`/bills/${b.id}`)">
            <div class="bill-icon">{{ typeIcon(b) }}</div>
            <div class="bill-main">
              <div class="bill-title">
                {{ b.type === 'transfer' ? '转账' : b.categoryName || '未分类' }}
                <el-tag v-if="b.remark" size="small" type="info">{{ b.remark }}</el-tag>
              </div>
              <div class="bill-sub">
                {{ accountSummary(b) }}
                <template v-if="b.type !== 'transfer'"> · {{ b.memberName }}</template>
                <el-tag v-for="t in b.tags" :key="t.id" size="small" :color="t.color" class="bill-tag">
                  {{ t.name }}
                </el-tag>
              </div>
            </div>
            <div class="bill-amount" :class="amountClass(b)">
              {{ amountText(b) }}
            </div>
          </div>
        </div>
        <div class="pager">
          <el-pagination
            layout="prev, pager, next"
            :total="total"
            :page-size="size"
            :current-page="page"
            @current-change="(p: number) => { page = p; load() }"
          />
        </div>
      </template>
      <el-empty v-else description="还没有账单，点击右上角“记一笔”开始吧" />
    </el-card>

    <BillFormDialog v-model="showForm" :ledger-id="ledgerId" @saved="load" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getLedger } from '@/api/ledger'
import { listBills } from '@/api/bill'
import type { BillVO } from '@/api/bill'
import { useLedgerStore } from '@/stores/ledger'
import { useUserStore } from '@/stores/user'
import type { LedgerVO } from '@/types'
import BillFormDialog from '@/components/BillFormDialog.vue'

const route = useRoute()
const ledgerStore = useLedgerStore()
const userStore = useUserStore()
const ledger = ref<LedgerVO | null>(null)
const bills = ref<BillVO[]>([])
const total = ref(0)
const page = ref(1)
const size = 20
const showForm = ref(false)
const ledgerId = Number(route.params.id)

const filters = reactive<{ type: string | undefined; range: [string, string] | null; keyword: string }>({
  type: undefined,
  range: null,
  keyword: '',
})

const isCreator = computed(() => ledger.value?.ownerId === userStore.user?.id)

const grouped = computed(() => {
  const map = new Map<string, BillVO[]>()
  for (const b of bills.value) {
    const date = (b.billDate || '').slice(0, 10)
    if (!map.has(date)) map.set(date, [])
    map.get(date)!.push(b)
  }
  return Array.from(map.entries()).map(([date, list]) => ({
    date,
    list,
    expense: list.filter((b) => b.type === 'expense').reduce((s, b) => s + b.amount, 0),
    income: list.filter((b) => b.type === 'income').reduce((s, b) => s + b.amount, 0),
  }))
})

function typeIcon(b: BillVO): string {
  if (b.type === 'expense') return '🛒'
  if (b.type === 'income') return '💵'
  return '🔁'
}
function accountSummary(b: BillVO): string {
  return b.accounts.map((a) => a.accountName).join(' / ')
}
function amountClass(b: BillVO) {
  if (b.type === 'expense') return 'expense'
  if (b.type === 'income') return 'income'
  return 'transfer'
}
function amountText(b: BillVO): string {
  if (b.type === 'expense') return `-${b.amount.toFixed(2)}`
  if (b.type === 'income') return `+${b.amount.toFixed(2)}`
  return `${b.amount.toFixed(2)}`
}

async function load() {
  const params: Record<string, string | number | undefined> = {
    page: page.value,
    size,
    type: filters.type,
    keyword: filters.keyword || undefined,
  }
  if (filters.range) {
    params.startDate = filters.range[0]
    params.endDate = filters.range[1]
  }
  const res = await listBills(ledgerId, params)
  bills.value = res.list
  total.value = res.total
}

onMounted(async () => {
  ledger.value = await getLedger(ledgerId)
  ledgerStore.switchTo(ledgerId)
  await load()
})
</script>

<style scoped>
.ledger-detail {
  padding: 20px;
  max-width: 960px;
  margin: 0 auto;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.head-actions {
  display: flex;
  gap: 8px;
}
.count {
  color: #909399;
  margin-left: 8px;
}
.filters {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 8px;
  flex-wrap: wrap;
}
.filter-right {
  display: flex;
  gap: 8px;
}
.day-group {
  margin-bottom: 12px;
}
.day-head {
  display: flex;
  justify-content: space-between;
  padding: 6px 4px;
  color: #909399;
  font-size: 13px;
}
.day-summary {
  font-size: 12px;
}
.bill-row {
  display: flex;
  align-items: center;
  padding: 10px 8px;
  border-radius: 8px;
  cursor: pointer;
}
.bill-row:hover {
  background: var(--el-fill-color-light);
}
.bill-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--el-fill-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  margin-right: 12px;
}
.bill-main {
  flex: 1;
  min-width: 0;
}
.bill-title {
  font-size: 14px;
}
.bill-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bill-tag {
  margin-left: 4px;
}
.bill-amount {
  font-size: 15px;
  font-weight: 600;
  margin-left: 12px;
}
.bill-amount.expense {
  color: var(--el-color-danger);
}
.bill-amount.income {
  color: var(--el-color-success);
}
.bill-amount.transfer {
  color: var(--el-color-info);
}
.pager {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}
</style>
