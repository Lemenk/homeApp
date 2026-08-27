<template>
  <div class="home">
    <div class="welcome">
      <h2>你好，{{ userStore.user?.nickname || userStore.user?.phone }}</h2>
      <p class="sub">
        当前账本：<el-tag v-if="ledgerStore.currentLedger" :type="ledgerStore.currentLedger.type === 'public' ? 'primary' : 'info'">
          {{ ledgerStore.currentLedger.name }}
        </el-tag>
        <span v-else>还没有账本</span>
      </p>
    </div>

    <el-row :gutter="16" class="quick">
      <el-col :span="6">
        <el-card shadow="hover" class="quick-card" @click="goRecord">
          <div class="quick-icon">✍️</div>
          <div class="quick-name">记一笔</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="quick-card" @click="$router.push('/accounts')">
          <div class="quick-icon">🏦</div>
          <div class="quick-name">账户</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="quick-card" @click="$router.push('/statistics')">
          <div class="quick-icon">📊</div>
          <div class="quick-name">统计</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="quick-card" @click="$router.push('/family')">
          <div class="quick-icon">👨‍👩‍👧‍👦</div>
          <div class="quick-name">家庭</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="recent">
      <template #header>近期账单</template>
      <template v-if="recentBills.length">
        <div v-for="b in recentBills" :key="b.id" class="bill-row" @click="$router.push(`/bills/${b.id}`)">
          <div class="bill-icon">{{ typeIcon(b) }}</div>
          <div class="bill-main">
            <div class="bill-title">{{ b.type === 'transfer' ? '转账' : b.categoryName || '未分类' }}</div>
            <div class="bill-sub">{{ (b.billDate || '').slice(0, 16).replace('T', ' ') }}</div>
          </div>
          <div class="bill-amount" :class="b.type">{{ amountText(b) }}</div>
        </div>
      </template>
      <el-empty v-else :image-size="80" description="还没有账单">
        <el-button type="primary" @click="goRecord">去记一笔</el-button>
      </el-empty>
    </el-card>

    <BillFormDialog v-model="showForm" :ledger-id="ledgerStore.currentLedgerId" @saved="loadRecent" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listBills } from '@/api/bill'
import type { BillVO } from '@/api/bill'
import { useLedgerStore } from '@/stores/ledger'
import { useUserStore } from '@/stores/user'
import BillFormDialog from '@/components/BillFormDialog.vue'

const router = useRouter()
const ledgerStore = useLedgerStore()
const userStore = useUserStore()
const recentBills = ref<BillVO[]>([])
const showForm = ref(false)

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

async function loadRecent() {
  if (!ledgerStore.currentLedgerId) return
  const res = await listBills(ledgerStore.currentLedgerId, { page: 1, size: 8 })
  recentBills.value = res.list
}

function goRecord() {
  if (!ledgerStore.currentLedgerId) {
    ElMessage.warning('请先创建账本')
    router.push('/ledgers')
    return
  }
  showForm.value = true
}

onMounted(async () => {
  if (!userStore.user) await userStore.fetchMe()
  await ledgerStore.fetch()
  await loadRecent()
})
</script>

<style scoped>
.home {
  padding: 20px;
  max-width: 960px;
  margin: 0 auto;
}
.welcome {
  margin-bottom: 20px;
}
.welcome h2 {
  margin: 0;
}
.sub {
  color: #909399;
  margin-top: 8px;
}
.quick {
  margin-bottom: 16px;
}
.quick-card {
  text-align: center;
  cursor: pointer;
  border-radius: 10px;
}
.quick-icon {
  font-size: 30px;
}
.quick-name {
  margin-top: 8px;
  font-size: 14px;
}
.recent {
  border-radius: 8px;
}
.bill-row {
  display: flex;
  align-items: center;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;
}
.bill-row:hover {
  background: var(--el-fill-color-light);
}
.bill-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: var(--el-fill-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  margin-right: 10px;
}
.bill-main {
  flex: 1;
}
.bill-title {
  font-size: 14px;
}
.bill-sub {
  font-size: 12px;
  color: #909399;
}
.bill-amount {
  font-weight: 600;
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
</style>
