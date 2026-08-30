<template>
  <div class="refresh-page">
    <!-- 顶部操作栏 -->
    <div class="top-bar">
      <div class="title-area">
        <h2>数据刷新</h2>
        <span class="refresh-time" v-if="lastRefresh">最后刷新：{{ lastRefresh }}</span>
      </div>
      <el-button type="primary" :loading="refreshing" @click="refreshAll">
        <el-icon><Refresh /></el-icon>
        <span>{{ refreshing ? '刷新中...' : '刷新数据' }}</span>
      </el-button>
    </div>

    <!-- 统计概览卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-icon ledger"><el-icon :size="24"><Notebook /></el-icon></div>
          <div class="stat-info">
            <div class="stat-num">{{ ledgers.length }}</div>
            <div class="stat-label">账本</div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-icon account"><el-icon :size="24"><Wallet /></el-icon></div>
          <div class="stat-info">
            <div class="stat-num">{{ accounts.length }}</div>
            <div class="stat-label">账户</div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-icon bill"><el-icon :size="24"><Document /></el-icon></div>
          <div class="stat-info">
            <div class="stat-num">{{ billCount }}</div>
            <div class="stat-label">账单</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 数据展示区 -->
    <div class="data-area">
      <!-- 账本列表 -->
      <div class="data-card">
        <div class="card-header">
          <span class="card-title">账本列表</span>
          <el-button text type="primary" size="small" @click="$router.push('/config')">管理</el-button>
        </div>
        <div class="card-body" v-if="ledgers.length">
          <div v-for="l in ledgers" :key="l.id" class="data-row" @click="$router.push(`/ledgers/${l.id}`)">
            <div class="row-icon">📒</div>
            <div class="row-main">
              <div class="row-title">{{ l.name }}</div>
              <div class="row-sub">{{ l.type === 'public' ? '公共账本' : '个人账本' }} · {{ l.memberCount || 1 }} 人</div>
            </div>
            <el-tag v-if="l.id === ledgerStore.currentLedgerId" size="small" type="primary">当前</el-tag>
          </div>
        </div>
        <el-empty v-else :image-size="60" description="暂无账本" />
      </div>

      <!-- 账户列表 -->
      <div class="data-card">
        <div class="card-header">
          <span class="card-title">账户列表</span>
          <el-button text type="primary" size="small" @click="$router.push('/accounts')">管理</el-button>
        </div>
        <div class="card-body" v-if="accounts.length">
          <div v-for="a in accounts" :key="a.id" class="data-row">
            <div class="row-icon">💳</div>
            <div class="row-main">
              <div class="row-title">{{ a.name }}</div>
              <div class="row-sub">{{ accountTypeLabel(a.type) }}{{ a.groupName ? ' · ' + a.groupName : '' }}</div>
            </div>
            <div class="row-amount">{{ a.balance.toFixed(2) }}</div>
          </div>
        </div>
        <el-empty v-else :image-size="60" description="暂无账户" />
      </div>

      <!-- 近期账单 -->
      <div class="data-card">
        <div class="card-header">
          <span class="card-title">近期账单</span>
          <el-button text type="primary" size="small" @click="goLedgerDetail">查看全部</el-button>
        </div>
        <div class="card-body" v-if="recentBills.length">
          <div v-for="b in recentBills" :key="b.id" class="data-row" @click="$router.push(`/bills/${b.id}`)">
            <div class="row-icon">{{ billTypeIcon(b) }}</div>
            <div class="row-main">
              <div class="row-title">{{ b.type === 'transfer' ? '转账' : b.categoryName || '未分类' }}</div>
              <div class="row-sub">{{ (b.billDate || '').slice(0, 16).replace('T', ' ') }}</div>
            </div>
            <div class="row-amount" :class="b.type">{{ billAmountText(b) }}</div>
          </div>
        </div>
        <el-empty v-else :image-size="60" description="暂无账单" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh, Notebook, Wallet, Document } from '@element-plus/icons-vue'
import { useLedgerStore } from '@/stores/ledger'
import { listLedgers } from '@/api/ledger'
import type { LedgerVO } from '@/types'
import { listAccounts } from '@/api/account'
import type { AccountVO, AccountType } from '@/api/account'
import { listBills } from '@/api/bill'
import type { BillVO } from '@/api/bill'
import { ACCOUNT_TYPE_LABELS } from '@/api/account'
import { useRouter } from 'vue-router'

const router = useRouter()
const ledgerStore = useLedgerStore()

const refreshing = ref(false)
const lastRefresh = ref('')
const ledgers = ref<LedgerVO[]>([])
const accounts = ref<AccountVO[]>([])
const recentBills = ref<BillVO[]>([])
const billCount = ref(0)

function accountTypeLabel(t: AccountType): string {
  return ACCOUNT_TYPE_LABELS[t] || t
}

function billTypeIcon(b: BillVO): string {
  if (b.type === 'expense') return '🛒'
  if (b.type === 'income') return '💵'
  return '🔁'
}

function billAmountText(b: BillVO): string {
  if (b.type === 'expense') return `-${b.amount.toFixed(2)}`
  if (b.type === 'income') return `+${b.amount.toFixed(2)}`
  return b.amount.toFixed(2)
}

function goLedgerDetail() {
  if (ledgerStore.currentLedgerId) {
    router.push(`/ledgers/${ledgerStore.currentLedgerId}`)
  }
}

async function refreshAll() {
  refreshing.value = true
  try {
    // 刷新账本 store
    await ledgerStore.fetch()
    // 并行查询所有数据
    const [ledgerList, accountList, billRes] = await Promise.all([
      listLedgers(),
      ledgerStore.currentLedgerId ? listAccounts(ledgerStore.currentLedgerId) : Promise.resolve([]),
      ledgerStore.currentLedgerId ? listBills(ledgerStore.currentLedgerId, { page: 1, size: 10 }) : Promise.resolve({ list: [], total: 0 }),
    ])
    ledgers.value = ledgerList
    accounts.value = accountList
    recentBills.value = billRes.list
    billCount.value = billRes.total || 0
    const now = new Date()
    lastRefresh.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
  } finally {
    refreshing.value = false
  }
}

onMounted(() => {
  refreshAll()
})
</script>

<style scoped>
.refresh-page {
  padding: 20px 24px;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.title-area h2 {
  margin: 0 0 4px 0;
  font-size: 20px;
}
.refresh-time {
  font-size: 12px;
  color: #909399;
}

.stat-row {
  margin-bottom: 20px;
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.stat-icon.ledger { background: linear-gradient(135deg, #409eff, #66b1ff); }
.stat-icon.account { background: linear-gradient(135deg, #67c23a, #85ce61); }
.stat-icon.bill { background: linear-gradient(135deg, #e6a23c, #ebb563); }
.stat-num {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

.data-area {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.data-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  max-height: 480px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-shrink: 0;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.card-body {
  flex: 1;
  overflow-y: auto;
}
.data-row {
  display: flex;
  align-items: center;
  padding: 10px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.data-row:hover {
  background: #f5f7fa;
}
.row-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  margin-right: 10px;
  flex-shrink: 0;
}
.row-main {
  flex: 1;
  min-width: 0;
}
.row-title {
  font-size: 13px;
  color: #303133;
}
.row-sub {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}
.row-amount {
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}
.row-amount.expense { color: #f56c6c; }
.row-amount.income { color: #67c23a; }
.row-amount.transfer { color: #909399; }
</style>
