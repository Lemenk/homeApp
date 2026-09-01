<template>
  <div class="bill-detail">
    <el-card v-if="bill">
      <div class="head">
        <el-button text @click="$router.back()">← 返回</el-button>
        <div>
          <el-button type="primary" plain @click="editing = bill; showForm = true">编辑</el-button>
          <el-popconfirm title="确定删除这笔账单？删除后账户余额会回滚" confirm-button-text="删除" cancel-button-text="取消" @confirm="onDelete">
            <template #reference>
              <el-button type="danger" plain>删除</el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>

      <div class="amount-box">
        <div class="amount" :class="bill.type">{{ amountText }}</div>
        <div class="type-label">
          {{ typeLabel }}
          <template v-if="bill.type !== 'transfer'">
            · {{ bill.categoryName || '未分类' }}
          </template>
        </div>
      </div>

      <el-descriptions :column="1" border>
        <el-descriptions-item label="类型">
          <el-tag :type="bill.type === 'expense' ? 'danger' : bill.type === 'income' ? 'success' : 'info'">
            {{ typeLabel }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="记账人">{{ bill.memberName }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ formatTime(bill.billDate) }}</el-descriptions-item>
        <el-descriptions-item label="账户">
          <div v-for="a in bill.accounts" :key="a.accountId" class="acc-line">
            <span class="acc-emoji" :style="{ background: accBg(a.accountId) }">
              <AppIcon :icon="accountMap.get(a.accountId)" :size="16" />
            </span>
            {{ directionLabel(a.direction) }} {{ a.accountName }}（{{ a.amount.toFixed(2) }}）
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ bill.remark || '—' }}</el-descriptions-item>
        <el-descriptions-item label="标签">
          <el-tag v-for="t in bill.tags" :key="t.id" :color="t.color" style="margin-right: 4px">{{ t.name }}</el-tag>
          <span v-if="!bill.tags.length">—</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="logs.length" class="logs-card">
      <template #header>
        <div class="logs-head">
          <span>操作记录（留痕）</span>
          <span class="logs-count">共 {{ logs.length }} 条</span>
        </div>
      </template>
      <el-timeline>
        <el-timeline-item v-for="log in logs" :key="log.id" :timestamp="formatTime(log.createdAt)" placement="top">
          <div class="log-item">
            <el-tag :type="actionTagType(log.action)" size="small" effect="dark" class="log-action">
              {{ actionLabel(log.action) }}
            </el-tag>
            <span class="log-summary">{{ log.summary || '—' }}</span>
            <div class="log-meta">
              <span class="log-operator">操作人：{{ log.operatorName || '未知' }}</span>
              <span class="log-time">{{ formatTime(log.createdAt) }}</span>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <BillFormDialog v-model="showForm" :ledger-id="ledgerId" :editing="editing" @saved="load" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { deleteBill, getBill, billLogs } from '@/api/bill'
import type { BillVO, BillLogVO } from '@/api/bill'
import { listAccounts } from '@/api/account'
import type { AccountVO } from '@/api/account'
import { iconBg } from '@/utils/accountIcon'
import BillFormDialog from '@/components/BillFormDialog.vue'
import AppIcon from '@/components/AppIcon.vue'

const route = useRoute()
const router = useRouter()
const bill = ref<BillVO | null>(null)
const logs = ref<BillLogVO[]>([])
const showForm = ref(false)
const editing = ref<BillVO | null>(null)
const billId = Number(route.params.id)
const ledgerId = ref(0)
const accountMap = ref(new Map<number, string>())

const typeLabel = computed(() => {
  if (!bill.value) return ''
  if (bill.value.type === 'expense') return '支出'
  if (bill.value.type === 'income') return '收入'
  return '转账'
})
const amountText = computed(() => {
  if (!bill.value) return ''
  const n = bill.value.amount.toFixed(2)
  if (bill.value.type === 'expense') return `-${n}`
  if (bill.value.type === 'income') return `+${n}`
  return n
})

function directionLabel(d: string) {
  return d === 'out' ? '转出' : '转入'
}
function formatTime(t?: string) {
  // 时间精确到秒
  return t ? t.replace('T', ' ').slice(0, 19) : ''
}
function accBg(accountId: number) {
  return iconBg(accountMap.value.get(accountId))
}
function actionLabel(a: string) {
  return { create: '创建', update: '修改', delete: '删除' }[a] || a
}
function actionTagType(a: string) {
  return { create: 'success', update: 'warning', delete: 'danger' }[a] || 'info'
}

async function load() {
  bill.value = await getBill(billId)
  ledgerId.value = bill.value.ledgerId
  logs.value = await billLogs(billId)
  // 加载账户列表以建立 账户id → icon 映射
  const list = await listAccounts(ledgerId.value).catch(() => [] as AccountVO[])
  accountMap.value = new Map(list.map((a) => [a.id, a.icon ?? 'other'] as [number, string]))
}

async function onDelete() {
  await deleteBill(billId)
  ElMessage.success('已删除')
  router.push('/')
}

onMounted(load)
</script>

<style scoped>
.bill-detail {
  padding: 20px;
  max-width: 720px;
  margin: 0 auto;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.amount-box {
  text-align: center;
  margin: 12px 0 24px;
}
.amount {
  font-size: 40px;
  font-weight: 700;
}
.amount.expense {
  color: var(--el-color-danger);
}
.amount.income {
  color: var(--el-color-success);
}
.type-label {
  margin-top: 6px;
  color: #909399;
}
.acc-line {
  line-height: 1.8;
  display: flex;
  align-items: center;
  gap: 6px;
}
.acc-emoji {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.logs-card {
  margin-top: 16px;
}
.logs-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.logs-count {
  font-size: 12px;
  color: #909399;
}
.log-item {
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  padding: 10px 12px;
  line-height: 1.6;
}
.log-action {
  margin-right: 8px;
  vertical-align: middle;
}
.log-summary {
  font-size: 14px;
  color: var(--el-text-color-primary);
  word-break: break-all;
}
.log-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
.log-operator {
  margin-right: 12px;
}
.log-time {
  font-size: 12px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .bill-detail {
    padding: 14px 12px;
  }
}
</style>
