<template>
  <div class="home-page">
    <!-- 顶部：账本选择器 -->
    <div class="top-bar">
      <div class="ledger-selector">
        <el-select
          v-if="ledgerStore.ledgers.length"
          v-model="currentId"
          class="ledger-select"
          @change="onSwitch"
        >
          <template #prefix>
            <el-icon class="ledger-prefix-icon"><Notebook /></el-icon>
          </template>
          <el-option v-for="l in ledgerStore.ledgers" :key="l.id" :label="l.name" :value="l.id">
            <div class="ledger-option">
              <span class="ledger-option-name">{{ l.name }}</span>
              <div class="ledger-option-tags">
                <el-tag v-if="l.isDefault === 1" size="small" type="warning" effect="light">默认</el-tag>
                <el-tag size="small" :type="l.type === 'public' ? 'primary' : 'info'" effect="plain">
                  {{ l.type === 'public' ? '公共' : '个人' }}
                </el-tag>
              </div>
            </div>
          </el-option>
        </el-select>
        <span v-else class="no-ledger" @click="$router.push('/config')">还没有账本，去创建</span>
      </div>
    </div>

    <!-- 主体：左侧近期账单 + 右侧记账模块 -->
    <div class="main-body">
      <!-- 左侧：近期账单（年月选择 + 月度统计 + 按日分组列表） -->
      <div class="left-panel">
        <div class="panel-header">
          <span class="panel-title">近期账单</span>
          <div class="month-nav">
            <el-button circle size="small" class="month-nav-btn" @click="shiftMonth(-1)">
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
            <el-date-picker
              v-model="month"
              type="month"
              format="YYYY年M月"
              value-format="YYYY-MM"
              placeholder="选择月份"
              :clearable="false"
              class="month-select"
              @change="load"
            />
            <el-button circle size="small" class="month-nav-btn" @click="shiftMonth(1)">
              <el-icon><ArrowRight /></el-icon>
            </el-button>
            <el-button size="small" class="month-today" @click="backToThisMonth">本月</el-button>
          </div>
        </div>

        <!-- 月度统计 -->
        <div class="month-stats">
          <div class="stat-item">
            <div class="stat-label">支出</div>
            <div class="stat-value expense">-{{ stats.expense.toFixed(2) }}</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-label">收入</div>
            <div class="stat-value income">+{{ stats.income.toFixed(2) }}</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-label">结余</div>
            <div class="stat-value" :class="stats.balance >= 0 ? 'income' : 'expense'">{{ sign(stats.balance) }}</div>
          </div>
        </div>

        <!-- 按日分组账单列表 -->
        <div class="bill-list" v-if="groups.length">
          <div v-for="g in groups" :key="g.date" class="day-group">
            <div class="day-head">
              <span class="day-date">{{ dayLabel(g.date) }}</span>
              <span class="day-summary">
                收入 {{ fmt(g.income) }}　支出 {{ fmt(g.expense) }}　结余 {{ sign(g.balance) }}
              </span>
            </div>
            <div v-for="b in g.list" :key="b.id" class="bill-row" @click="$router.push(`/bills/${b.id}`)">
              <div class="bill-icon" :class="b.type">{{ categoryIcon(b) }}</div>
              <div class="bill-main">
                <div class="bill-title">
                  {{ b.type === 'transfer' ? '转账' : b.categoryName || '未分类' }}
                </div>
                <div class="bill-sub">
                  <span class="bill-time">{{ timeOf(b.billDate) }}</span>
                  <el-tag v-for="t in b.tags" :key="t.id" size="small" :color="t.color" class="bill-tag">
                    {{ t.name }}
                  </el-tag>
                </div>
                <div v-if="b.remark" class="bill-remark">{{ b.remark }}</div>
              </div>
              <div class="bill-right">
                <div v-for="a in b.accounts" :key="a.accountId" class="bill-account">
                  <AppIcon :icon="accountIconOf(a.accountId)" :size="16" />
                  <span class="acc-name">{{ a.accountName }}</span>
                </div>
                <div class="bill-amount" :class="b.type">{{ amountText(b) }}</div>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else :image-size="80" description="该月份还没有账单">
          <el-button type="primary" @click="openRecord">去记一笔</el-button>
        </el-empty>
      </div>

      <!-- 右侧：记账模块 -->
      <div class="right-panel">
        <div class="record-card">
          <div class="record-header">
            <div class="record-icon">
              <el-icon :size="28"><EditPen /></el-icon>
            </div>
            <div class="record-title">快速记账</div>
          </div>

          <!-- 选中月份收支概览 -->
          <div class="month-summary">
            <div class="sum-item">
              <div class="sum-label">{{ monthLabel }}收入</div>
              <div class="sum-value income">+{{ stats.income.toFixed(2) }}</div>
            </div>
            <div class="sum-divider"></div>
            <div class="sum-item">
              <div class="sum-label">{{ monthLabel }}支出</div>
              <div class="sum-value expense">-{{ stats.expense.toFixed(2) }}</div>
            </div>
          </div>

          <!-- 记一笔按钮 -->
          <el-button type="primary" size="large" class="record-btn" @click="openRecord">
            <el-icon><Plus /></el-icon>
            <span>记一笔</span>
          </el-button>

          <div class="record-hint">选择类型、金额、分类即可快速记录</div>
        </div>
      </div>
    </div>

    <BillFormDialog v-model="showForm" :ledger-id="ledgerStore.currentLedgerId" @saved="load" />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, EditPen, Notebook, Plus } from '@element-plus/icons-vue'
import { listBills } from '@/api/bill'
import type { BillVO } from '@/api/bill'
import { listAccounts, summary } from '@/api/account'
import type { AccountVO } from '@/api/account'
import { useLedgerStore } from '@/stores/ledger'
import BillFormDialog from '@/components/BillFormDialog.vue'
import AppIcon from '@/components/AppIcon.vue'
import { groupBillsByDay, monthStats, parseBillDateTime } from '@/utils/billStats'
import { categoryEmoji, isKnownCategoryIcon } from '@/utils/categoryIcon'

const ledgerStore = useLedgerStore()
const registerRefresh = inject<(fn: () => Promise<void>) => void>('registerRefresh')
const bills = ref<BillVO[]>([])
const accounts = ref<AccountVO[]>([])
const showForm = ref(false)

/** 当前选中月份 YYYY-MM */
const month = ref('')
const thisMonth = (() => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
})()

const currentId = computed({
  get: () => ledgerStore.currentLedgerId,
  set: (v: number) => ledgerStore.switchTo(v),
})

const monthLabel = computed(() => {
  const [y, m] = month.value.split('-').map(Number)
  return `${y}年${m}月`
})

/** 选中月份收支统计 */
const stats = computed(() => monthStats(bills.value))

/** 按日分组（日期倒序，组内时间倒序） */
const groups = computed(() => groupBillsByDay(bills.value))

function onSwitch() {
  load()
  loadAccounts()
}

function typeIcon(b: BillVO): string {
  if (b.type === 'expense') return '🛒'
  if (b.type === 'income') return '💵'
  return '🔁'
}

/** 分类图标：优先使用账单分类自身 emoji，否则回退到类型图标 */
function categoryIcon(b: BillVO): string {
  if (isKnownCategoryIcon(b.categoryIcon)) return categoryEmoji(b.categoryIcon)
  return typeIcon(b)
}

/** 账户 id → icon 映射（用于账单行的账户图标展示） */
const accountIconMap = computed(() => {
  const m = new Map<number, string>()
  for (const a of accounts.value) m.set(a.id, a.icon ?? 'other')
  return m
})
function accountIconOf(accountId: number): string | undefined {
  return accountIconMap.value.get(accountId)
}

function amountText(b: BillVO): string {
  if (b.type === 'expense') return `-${b.amount.toFixed(2)}`
  if (b.type === 'income') return `+${b.amount.toFixed(2)}`
  return b.amount.toFixed(2)
}

function fmt(n: number): string {
  return n.toFixed(2)
}
function sign(n: number): string {
  return `${n >= 0 ? '+' : ''}${n.toFixed(2)}`
}

function shiftMonth(delta: number) {
  const [y, m] = month.value.split('-').map(Number)
  const d = new Date(y, m - 1 + delta, 1)
  month.value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
  load()
}

function backToThisMonth() {
  month.value = thisMonth
  load()
}

function dayLabel(date: string): string {
  const [y, m, d] = date.split('-').map(Number)
  const now = new Date()
  const label = `${m}月${d}日`
  if (y === now.getFullYear() && m === now.getMonth() + 1 && d === now.getDate()) {
    return `今天 ${label}`
  }
  return label
}

function timeOf(billDate?: string): string {
  return parseBillDateTime(billDate).time
}

async function loadAll() {
  await Promise.all([load(), loadAccounts()])
}

/** 加载选中月份账单 */
async function load() {
  if (!ledgerStore.currentLedgerId) return
  const [y, m] = month.value.split('-').map(Number)
  const lastDay = new Date(y, m, 0).getDate()
  const res = await listBills(ledgerStore.currentLedgerId, {
    page: 1,
    size: 1000,
    startDate: `${month.value}-01`,
    endDate: `${month.value}-${String(lastDay).padStart(2, '0')}`,
  })
  bills.value = res.list
}

async function loadAccounts() {
  if (!ledgerStore.currentLedgerId) return
  const [list] = await Promise.all([
    listAccounts(ledgerStore.currentLedgerId),
    summary(ledgerStore.currentLedgerId).catch(() => null),
  ])
  accounts.value = list
}

function openRecord() {
  if (!ledgerStore.currentLedgerId) {
    ElMessage.warning('请先创建账本')
    return
  }
  showForm.value = true
}

onMounted(async () => {
  await ledgerStore.fetch()
  month.value = thisMonth
  await loadAll()
  registerRefresh?.(loadAll)
})
</script>

<style scoped>
.home-page {
  padding: 20px 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

/* 顶部：账本选择器 */
.top-bar {
  margin-bottom: 16px;
  flex-shrink: 0;
}
.ledger-selector {
  display: flex;
  align-items: center;
}
.ledger-select {
  width: 220px;
}
.ledger-select :deep(.el-select__wrapper) {
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  min-height: 40px;
  padding: 0 12px;
}
.ledger-select :deep(.el-select__wrapper:hover) {
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}
.ledger-prefix-icon {
  color: #409eff;
  margin-right: 4px;
}
.ledger-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.ledger-option-tags {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
.ledger-option-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.no-ledger {
  color: #409eff;
  cursor: pointer;
  font-size: 13px;
}

/* 主体左右分栏 */
.main-body {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
}

/* 左侧：近期账单 */
.left-panel {
  flex: 1.6;
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  flex-shrink: 0;
  gap: 8px;
  flex-wrap: wrap;
}
.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

/* 年月选择 */
.month-nav {
  display: flex;
  align-items: center;
  gap: 6px;
}
.month-select {
  width: 142px;
}
.month-select :deep(.el-input__inner) {
  text-align: center;
  font-weight: 600;
  font-size: 14px;
}
.month-nav-btn {
  color: var(--el-color-primary);
}

/* 月度统计 */
.month-stats {
  display: flex;
  align-items: center;
  padding: 10px 0;
  margin-bottom: 10px;
  background: #f8f9fb;
  border-radius: 10px;
  flex-shrink: 0;
}
.stat-item {
  flex: 1;
  text-align: center;
}
.stat-label {
  font-size: 12px;
  color: #909399;
}
.stat-value {
  font-size: 18px;
  font-weight: 700;
  margin-top: 2px;
}
.stat-value.expense {
  color: #f56c6c;
}
.stat-value.income {
  color: #67c23a;
}
.stat-divider {
  width: 1px;
  height: 30px;
  background: #e4e7ed;
}

.bill-list {
  flex: 1;
  overflow-y: auto;
  margin: 0 -4px;
}
.day-group {
  margin-bottom: 6px;
}
.day-head {
  display: flex;
  justify-content: space-between;
  padding: 6px 4px;
  color: #909399;
  font-size: 12px;
  border-radius: 6px;
}
.day-date {
  font-weight: 600;
  color: #606266;
}
.day-summary {
  font-size: 11px;
}
.bill-row {
  display: flex;
  align-items: center;
  padding: 8px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.bill-row:hover {
  background: #f5f7fa;
}
.bill-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-right: 6px;
}
.bill-icon {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  margin-right: 10px;
  flex-shrink: 0;
  background: #f0f2f5;
}
.bill-icon.expense { background: #fef0f0; }
.bill-icon.income { background: #f0f9eb; }
.bill-icon.transfer { background: #ecf5ff; }
.bill-main {
  flex: 1;
  min-width: 0;
}
.bill-title {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}
.bill-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bill-remark {
  font-size: 12px;
  color: #606266;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bill-tag {
  margin-left: 4px;
}
/* 右侧：账户（图标+名称）与金额 */
.bill-right {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: 8px;
  flex-shrink: 0;
}
.bill-account {
  display: flex;
  align-items: center;
  gap: 4px;
}
.acc-name {
  font-size: 12px;
  color: #606266;
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bill-amount {
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
}
.bill-amount.expense { color: #f56c6c; }
.bill-amount.income { color: #67c23a; }
.bill-amount.transfer { color: #909399; }

/* 右侧：记账模块 */
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 280px;
  max-width: 360px;
}
.record-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}
.record-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.record-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.record-title {
  font-size: 16px;
  font-weight: 600;
}
.month-summary {
  display: flex;
  align-items: center;
  background: #f8f9fb;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
}
.sum-item {
  flex: 1;
  text-align: center;
}
.sum-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}
.sum-value {
  font-size: 18px;
  font-weight: 700;
}
.sum-value.income { color: #67c23a; }
.sum-value.expense { color: #f56c6c; }
.sum-divider {
  width: 1px;
  height: 32px;
  background: #e4e7ed;
}
.record-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.record-hint {
  text-align: center;
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 10px;
}

/* 移动端适配：左右分栏改为纵向堆叠 */
@media (max-width: 768px) {
  .home-page {
    padding: 14px 12px;
  }
  .main-body {
    flex-direction: column;
  }
  .left-panel {
    flex: none;
    min-height: 260px;
    max-height: 48vh;
  }
  .panel-header {
    flex-direction: column;
    align-items: flex-start;
  }
  .month-nav {
    width: 100%;
    justify-content: center;
  }
  .right-panel {
    flex: none;
    width: 100%;
    min-width: 0;
    max-width: none;
  }
  .ledger-select {
    width: 100%;
  }
}
</style>
