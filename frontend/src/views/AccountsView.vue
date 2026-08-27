<template>
  <div class="accounts-page">
    <!-- 资产总览 -->
    <el-row :gutter="16" class="summary">
      <el-col :span="8">
        <el-card shadow="never">
          <div class="sum-label">总资产</div>
          <div class="sum-value assets">¥ {{ summaryData.totalAssets.toFixed(2) }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <div class="sum-label">总负债</div>
          <div class="sum-value liability">¥ {{ summaryData.totalLiability.toFixed(2) }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <div class="sum-label">净资产</div>
          <div class="sum-value net">¥ {{ summaryData.netAssets.toFixed(2) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="list-card">
      <template #header>
        <div class="card-head">
          <span>账户列表</span>
          <el-button type="primary" size="small" @click="showCreate = true">＋ 新增账户</el-button>
        </div>
      </template>

      <div v-for="group in groupedAccounts" :key="group.type" class="type-group">
        <div class="type-title">{{ typeLabel(group.type) }}</div>
        <div v-for="a in group.accounts" :key="a.id" class="account-row">
          <div class="acc-icon">{{ a.icon || '🏦' }}</div>
          <div class="acc-main">
            <div class="acc-name">{{ a.name }}</div>
            <div class="acc-sub">初始 {{ a.initialBalance.toFixed(2) }}</div>
          </div>
          <div class="acc-right">
            <div class="acc-balance">{{ a.balance.toFixed(2) }}</div>
            <el-button text size="small" @click="openAdjust(a)">调整余额</el-button>
          </div>
        </div>
        <el-empty v-if="!group.accounts.length" :image-size="60" description="暂无该类型账户" />
      </div>
    </el-card>

    <!-- 新增账户 -->
    <el-dialog v-model="showCreate" title="新增账户" width="440px">
      <el-form label-width="80px">
        <el-form-item label="账户名称">
          <el-input v-model="createForm.name" placeholder="如：工资卡 / 信用卡 / 余额宝" />
        </el-form-item>
        <el-form-item label="账户类型">
          <el-select v-model="createForm.type" style="width: 100%">
            <el-option label="常用账户" value="common" />
            <el-option label="负债账户（信用卡/房贷）" value="liability" />
            <el-option label="储值账户（余额/卡券）" value="stored_value" />
            <el-option label="投资账户" value="investment" />
          </el-select>
        </el-form-item>
        <el-form-item label="期初余额">
          <el-input-number v-model="createForm.initialBalance" :precision="2" :controls="false" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 调整余额 -->
    <el-dialog v-model="showAdjust" :title="`调整余额 · ${adjustTarget?.name || ''}`" width="420px">
      <el-form label-width="80px">
        <el-form-item label="当前余额">¥ {{ adjustTarget?.balance.toFixed(2) }}</el-form-item>
        <el-form-item label="调整后余额">
          <el-input-number v-model="adjustForm.newBalance" :precision="2" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input v-model="adjustForm.reason" placeholder="如：现金清点 / 银行利息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdjust = false">取消</el-button>
        <el-button type="primary" :loading="adjusting" @click="submitAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listAccounts, createAccount, adjustBalance, summary } from '@/api/account'
import type { AccountVO } from '@/api/account'
import { useLedgerStore } from '@/stores/ledger'

const ledgerStore = useLedgerStore()
const accounts = ref<AccountVO[]>([])
const summaryData = ref({ totalAssets: 0, totalLiability: 0, netAssets: 0, accounts: [] as AccountVO[] })

const showCreate = ref(false)
const creating = ref(false)
const createForm = reactive({ name: '', type: 'common', initialBalance: 0 })

const showAdjust = ref(false)
const adjusting = ref(false)
const adjustTarget = ref<AccountVO | null>(null)
const adjustForm = reactive({ newBalance: 0, reason: '' })

const TYPE_LABELS: Record<string, string> = {
  common: '常用账户',
  liability: '负债账户',
  stored_value: '储值账户',
  investment: '投资账户',
}
const TYPE_ORDER = ['common', 'liability', 'stored_value', 'investment']

function typeLabel(t: string) {
  return TYPE_LABELS[t] || t
}

const groupedAccounts = computed(() =>
  TYPE_ORDER.map((t) => ({
    type: t,
    accounts: accounts.value.filter((a) => a.type === t),
  }))
)

async function load() {
  if (!ledgerStore.currentLedgerId) return
  const [list, sum] = await Promise.all([
    listAccounts(ledgerStore.currentLedgerId),
    summary(ledgerStore.currentLedgerId),
  ])
  accounts.value = list
  summaryData.value = sum
}

async function submitCreate() {
  if (!createForm.name.trim()) {
    ElMessage.warning('请输入账户名称')
    return
  }
  creating.value = true
  try {
    await createAccount(ledgerStore.currentLedgerId, {
      name: createForm.name.trim(),
      type: createForm.type,
      initialBalance: createForm.initialBalance || undefined,
    })
    ElMessage.success('账户已创建')
    showCreate.value = false
    createForm.name = ''
    createForm.initialBalance = 0
    await load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

function openAdjust(a: AccountVO) {
  adjustTarget.value = a
  adjustForm.newBalance = a.balance
  adjustForm.reason = ''
  showAdjust.value = true
}

async function submitAdjust() {
  if (!adjustTarget.value) return
  adjusting.value = true
  try {
    await adjustBalance(adjustTarget.value.id, {
      newBalance: adjustForm.newBalance,
      reason: adjustForm.reason || undefined,
    })
    ElMessage.success('余额已调整')
    showAdjust.value = false
    await load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '调整失败')
  } finally {
    adjusting.value = false
  }
}

onMounted(async () => {
  await ledgerStore.fetch()
  await load()
})
</script>

<style scoped>
.accounts-page {
  padding: 20px;
  max-width: 960px;
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
  font-size: 26px;
  font-weight: 700;
  margin-top: 6px;
}
.sum-value.assets {
  color: var(--el-color-success);
}
.sum-value.liability {
  color: var(--el-color-danger);
}
.sum-value.net {
  color: var(--el-color-primary);
}
.list-card {
  border-radius: 8px;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.type-group {
  margin-bottom: 16px;
}
.type-title {
  font-size: 13px;
  color: #909399;
  margin: 8px 0;
  padding-left: 4px;
}
.account-row {
  display: flex;
  align-items: center;
  padding: 10px 8px;
  border-radius: 8px;
}
.account-row:hover {
  background: var(--el-fill-color-light);
}
.acc-icon {
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
.acc-main {
  flex: 1;
}
.acc-name {
  font-size: 14px;
}
.acc-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.acc-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.acc-balance {
  font-size: 16px;
  font-weight: 600;
}
</style>
