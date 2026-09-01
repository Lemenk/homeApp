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
              <el-tag size="small" :type="l.type === 'public' ? 'primary' : 'info'" effect="plain">
                {{ l.type === 'public' ? '公共' : '个人' }}
              </el-tag>
            </div>
          </el-option>
        </el-select>
        <span v-else class="no-ledger" @click="$router.push('/config')">还没有账本，去创建</span>
      </div>
    </div>

    <!-- 主体：左侧近期账单 + 右侧记账模块 -->
    <div class="main-body">
      <!-- 左侧：近期账单 -->
      <div class="left-panel">
        <div class="panel-header">
          <span class="panel-title">近期账单</span>
          <el-button text type="primary" size="small" @click="$router.push(`/ledgers/${ledgerStore.currentLedgerId}`)">查看全部</el-button>
        </div>
        <div class="bill-list" v-if="recentBills.length">
          <div
            v-for="b in recentBills"
            :key="b.id"
            class="bill-row"
            @click="$router.push(`/bills/${b.id}`)"
          >
            <div class="bill-icon" :class="b.type">{{ typeIcon(b) }}</div>
            <div class="bill-main">
              <div class="bill-title">{{ b.type === 'transfer' ? '转账' : b.categoryName || '未分类' }}</div>
              <div class="bill-sub">{{ formatDate(b.billDate) }} · {{ b.accountName || '未指定账户' }}</div>
            </div>
            <div class="bill-amount" :class="b.type">{{ amountText(b) }}</div>
          </div>
        </div>
        <el-empty v-else :image-size="80" description="还没有账单">
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

          <!-- 本月收支概览 -->
          <div class="month-summary">
            <div class="sum-item">
              <div class="sum-label">本月收入</div>
              <div class="sum-value income">+{{ monthIncome.toFixed(2) }}</div>
            </div>
            <div class="sum-divider"></div>
            <div class="sum-item">
              <div class="sum-label">本月支出</div>
              <div class="sum-value expense">-{{ monthExpense.toFixed(2) }}</div>
            </div>
          </div>

          <!-- 记一笔按钮 -->
          <el-button type="primary" size="large" class="record-btn" @click="openRecord">
            <el-icon><Plus /></el-icon>
            <span>记一笔</span>
          </el-button>

          <div class="record-hint">选择类型、金额、分类即可快速记录</div>
        </div>

        <!-- 账户概览小卡片 -->
        <div class="accounts-card" v-if="accounts.length">
          <div class="panel-header">
            <span class="panel-title">账户概览</span>
            <el-button text type="primary" size="small" @click="$router.push('/accounts')">管理</el-button>
          </div>
          <div class="acc-mini-list">
            <div v-for="a in accounts.slice(0, 4)" :key="a.id" class="acc-mini-row">
              <div class="acc-mini-icon">{{ a.icon ? '💳' : '🏦' }}</div>
              <div class="acc-mini-name">{{ a.name }}</div>
              <div class="acc-mini-balance">{{ a.balance.toFixed(2) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <BillFormDialog v-model="showForm" :ledger-id="ledgerStore.currentLedgerId" @saved="loadRecent" />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen, Notebook, Plus } from '@element-plus/icons-vue'
import { listBills } from '@/api/bill'
import type { BillVO } from '@/api/bill'
import { listAccounts, summary } from '@/api/account'
import type { AccountVO } from '@/api/account'
import { useLedgerStore } from '@/stores/ledger'
import BillFormDialog from '@/components/BillFormDialog.vue'

const ledgerStore = useLedgerStore()
const registerRefresh = inject<(fn: () => Promise<void>) => void>('registerRefresh')
const recentBills = ref<BillVO[]>([])
const accounts = ref<AccountVO[]>([])
const showForm = ref(false)
const monthIncome = ref(0)
const monthExpense = ref(0)

const currentId = computed({
  get: () => ledgerStore.currentLedgerId,
  set: (v: number) => ledgerStore.switchTo(v),
})

function onSwitch() {
  loadRecent()
  loadAccounts()
}

function typeIcon(b: BillVO): string {
  if (b.type === 'expense') return '🛒'
  if (b.type === 'income') return '💵'
  return '🔁'
}

function amountText(b: BillVO): string {
  if (b.type === 'expense') return `-${b.amount.toFixed(2)}`
  if (b.type === 'income') return `+${b.amount.toFixed(2)}`
  return b.amount.toFixed(2)
}

function formatDate(d?: string): string {
  if (!d) return ''
  return d.slice(0, 16).replace('T', ' ')
}

async function loadAll() {
  await Promise.all([loadRecent(), loadAccounts()])
}

async function loadRecent() {
  if (!ledgerStore.currentLedgerId) return
  const res = await listBills(ledgerStore.currentLedgerId, { page: 1, size: 15 })
  recentBills.value = res.list
  // 计算本月收支
  const now = new Date()
  const ym = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  monthIncome.value = res.list
    .filter((b) => b.type === 'income' && (b.billDate || '').startsWith(ym))
    .reduce((s, b) => s + b.amount, 0)
  monthExpense.value = res.list
    .filter((b) => b.type === 'expense' && (b.billDate || '').startsWith(ym))
    .reduce((s, b) => s + b.amount, 0)
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
  flex: 1.4;
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
  margin-bottom: 12px;
  flex-shrink: 0;
}
.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.bill-list {
  flex: 1;
  overflow-y: auto;
}
.bill-row {
  display: flex;
  align-items: center;
  padding: 10px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.bill-row:hover {
  background: #f5f7fa;
}
.bill-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  margin-right: 12px;
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
}
.bill-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.bill-amount {
  font-weight: 600;
  font-size: 15px;
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
  max-width: 380px;
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

/* 账户概览小卡片 */
.accounts-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
}
.acc-mini-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.acc-mini-row {
  display: flex;
  align-items: center;
  padding: 6px 4px;
  border-radius: 6px;
}
.acc-mini-row:hover {
  background: #f5f7fa;
}
.acc-mini-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  margin-right: 10px;
}
.acc-mini-name {
  flex: 1;
  font-size: 13px;
  color: #303133;
}
.acc-mini-balance {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}
</style>
