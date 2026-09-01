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
          <el-button type="primary" size="small" @click="openCreate">＋ 新增账户</el-button>
        </div>
      </template>

      <div v-for="group in groupedAccounts" :key="group.type" class="type-group">
        <div class="type-title">{{ typeLabel(group.type) }}</div>
        <div v-for="a in group.accounts" :key="a.id" class="account-row">
          <div class="acc-icon" :style="{ background: iconBg(a.icon) }">
            <span class="acc-emoji">{{ iconEmoji(a.icon) }}</span>
          </div>
          <div class="acc-main">
            <div class="acc-name">{{ a.name }}</div>
            <div class="acc-sub">{{ a.groupName || typeLabel(a.type) }} · 初始 {{ a.initialBalance.toFixed(2) }}</div>
          </div>
          <div class="acc-right">
            <div class="acc-balance">{{ a.balance.toFixed(2) }}</div>
            <el-button text size="small" @click="openAdjust(a)">调整余额</el-button>
          </div>
        </div>
        <el-empty v-if="!group.accounts.length" :image-size="60" description="暂无该类型账户" />
      </div>
    </el-card>

    <!-- 新增账户：两步对话框 -->
    <el-dialog
      v-model="showCreate"
      :width="createStep === 1 ? '520px' : '560px'"
      :show-close="true"
      :close-on-click-modal="false"
      class="create-account-dialog"
      @closed="resetCreate"
    >
      <!-- Step 1：选择账户类型 -->
      <div v-if="createStep === 1" class="step-type">
        <div class="dialog-header">
          <div class="header-icon">
            <el-icon :size="32"><Wallet /></el-icon>
          </div>
          <div class="header-title">新建账户</div>
          <div class="header-sub">选择一个账户类型开始创建</div>
        </div>
        <div class="type-cards">
          <div
            v-for="t in TYPE_ORDER"
            :key="t"
            class="type-card"
            @click="pickType(t)"
          >
            <div class="type-card-icon" :style="{ background: typeMeta(t).bg }">
              <span class="type-card-emoji">{{ typeMeta(t).emoji }}</span>
            </div>
            <div class="type-card-info">
              <div class="type-card-name">{{ typeLabel(t) }}</div>
              <div class="type-card-desc">{{ typeMeta(t).desc }}</div>
            </div>
            <el-icon class="type-card-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <!-- Step 2：填写账户信息 -->
      <div v-else class="step-form">
        <div class="form-topbar">
          <el-button text class="back-btn" @click="createStep = 1">
            <el-icon><ArrowLeft /></el-icon>
            <span>返回</span>
          </el-button>
        </div>
        <div class="dialog-header">
          <div class="header-icon" :style="{ background: iconBg(createForm.icon) }">
            <span class="header-emoji">{{ iconEmoji(createForm.icon) }}</span>
          </div>
          <div class="header-title">添加{{ typeLabel(createForm.type) }}</div>
          <div class="header-sub">当前图标：{{ iconLabel(createForm.icon) }}</div>
        </div>

        <!-- 选择图标 -->
        <div class="form-section">
          <div class="section-label">
            <el-icon><Picture /></el-icon>
            <span>选择图标</span>
          </div>
          <div class="icon-grid">
            <div
              v-for="ic in ICON_OPTIONS"
              :key="ic.key"
              class="icon-item"
              :class="{ active: createForm.icon === ic.key }"
              @click="createForm.icon = ic.key"
            >
              <div class="icon-circle" :style="{ background: ic.bg }">
                <span class="icon-emoji">{{ ic.emoji }}</span>
              </div>
              <div class="icon-name">{{ ic.label }}</div>
            </div>
          </div>
        </div>

        <!-- 账户名称 -->
        <div class="form-section">
          <div class="section-label">
            <el-icon><EditPen /></el-icon>
            <span>账户名称</span>
          </div>
          <el-input
            v-model="createForm.name"
            placeholder="请输入账户名称"
            size="large"
            class="form-input"
          />
        </div>

        <!-- 账户余额 -->
        <div class="form-section">
          <div class="section-label">
            <el-icon><Money /></el-icon>
            <span>账户余额</span>
          </div>
          <div class="balance-wrap" :class="{ 'is-gray': createForm.balanceDisabled }" @click="onBalanceClick">
            <el-input-number
              ref="balanceInput"
              v-model="createForm.initialBalance"
              :precision="2"
              :controls="false"
              size="large"
              class="form-input"
              :readonly="createForm.balanceDisabled"
              @blur="onBalanceBlur"
            >
              <template #prepend>¥</template>
            </el-input-number>
          </div>
          <div v-if="createForm.balanceDisabled" class="balance-tip">默认 0.00，点击可填写初始余额</div>
        </div>

        <!-- 分组 -->
        <div class="form-section">
          <div class="section-label">
            <el-icon><Folder /></el-icon>
            <span>分组</span>
          </div>
          <el-input
            v-model="createForm.groupName"
            :placeholder="typeLabel(createForm.type)"
            size="large"
            class="form-input"
          />
        </div>

        <!-- 备注 -->
        <div class="form-section">
          <div class="section-label">
            <el-icon><Document /></el-icon>
            <span>备注</span>
          </div>
          <el-input
            v-model="createForm.remark"
            type="textarea"
            :rows="2"
            placeholder="请输入备注（选填）"
            class="form-input"
          />
        </div>

        <!-- 计入总资产 -->
        <div class="form-section include-total">
          <div class="include-total-icon">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="include-total-info">
            <div class="include-total-name">计入总资产</div>
            <div class="include-total-desc">该账户余额将计入资产总额</div>
          </div>
          <el-switch v-model="createForm.includeInTotal" :active-value="1" :inactive-value="0" />
        </div>
      </div>

      <template #footer>
        <div v-if="createStep === 2" class="dialog-footer">
          <el-button size="large" @click="showCreate = false">取消</el-button>
          <el-button type="primary" size="large" :loading="creating" @click="submitCreate">创建</el-button>
        </div>
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
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Wallet, ArrowRight, ArrowLeft, Picture, EditPen, Money, Folder, Document, CircleCheck,
} from '@element-plus/icons-vue'
import {
  listAccounts,
  createAccount,
  adjustBalance,
  summary,
  ACCOUNT_TYPE_LABELS,
  ACCOUNT_TYPE_ORDER,
} from '@/api/account'
import type { AccountVO, AccountType } from '@/api/account'
import { useLedgerStore } from '@/stores/ledger'

const ledgerStore = useLedgerStore()
const accounts = ref<AccountVO[]>([])
const summaryData = ref({ totalAssets: 0, totalLiability: 0, netAssets: 0, accounts: [] as AccountVO[] })

/* ---------- 新增账户：两步 ---------- */
const showCreate = ref(false)
const createStep = ref(1) // 1=选类型, 2=填表单
const creating = ref(false)

interface IconOption { key: string; label: string; emoji: string; bg: string }
const ICON_OPTIONS: IconOption[] = [
  { key: 'cash', label: '现金', emoji: '¥', bg: '#FFF3E0' },
  { key: 'wechat', label: '微信', emoji: '💬', bg: '#E8F5E9' },
  { key: 'alipay', label: '支付宝', emoji: '支', bg: '#E3F2FD' },
  { key: 'bankcard', label: '银行卡', emoji: '💳', bg: '#FFF8E1' },
  { key: 'creditcard', label: '信用卡', emoji: '💳', bg: '#ECEFF1' },
  { key: 'member', label: '会员卡', emoji: '🎫', bg: '#FFF3E0' },
  { key: 'meal', label: '饭卡', emoji: '🍱', bg: '#FFF3E0' },
  { key: 'bus', label: '公交卡', emoji: '🚌', bg: '#E8F5E9' },
  { key: 'huabei', label: '花呗', emoji: '🌸', bg: '#E3F2FD' },
  { key: 'baitiao', label: '白条', emoji: '📋', bg: '#FFEBEE' },
  { key: 'jd', label: '京东金融', emoji: '🐕', bg: '#FFEBEE' },
  { key: 'qq', label: 'QQ钱包', emoji: '🐧', bg: '#E3F2FD' },
  { key: 'stock', label: '股票', emoji: '📈', bg: '#FFEBEE' },
  { key: 'fund', label: '基金', emoji: '📊', bg: '#E8F5E9' },
  { key: 'finance', label: '理财', emoji: '💰', bg: '#FFF8E1' },
  { key: 'deposit', label: '存款', emoji: '🏦', bg: '#E3F2FD' },
  { key: 'fixed', label: '固定资产', emoji: '🔒', bg: '#ECEFF1' },
  { key: 'house', label: '房子', emoji: '🏠', bg: '#FFEBEE' },
  { key: 'car', label: '车', emoji: '🚗', bg: '#E3F2FD' },
  { key: 'insurance', label: '保险', emoji: '🛡️', bg: '#E3F2FD' },
  { key: 'reimburse', label: '报销', emoji: '🧾', bg: '#E3F2FD' },
  { key: 'deposit_fee', label: '押金', emoji: '🔑', bg: '#E3F2FD' },
  { key: 'crypto', label: '虚拟货币', emoji: '🪙', bg: '#F3E5F5' },
  { key: 'ledger', label: '账本', emoji: '📒', bg: '#ECEFF1' },
  { key: 'other', label: '其他', emoji: '⭐', bg: '#E8F5E9' },
  { key: 'lend', label: '借出', emoji: '💸', bg: '#FFF8E1' },
  { key: 'borrow', label: '借入', emoji: '💵', bg: '#FFF8E1' },
]

const createForm = reactive<{
  name: string
  type: AccountType
  initialBalance: number | null
  icon: string
  groupName: string
  remark: string
  includeInTotal: number
  balanceDisabled: boolean
}>({
  name: '',
  type: 'asset',
  initialBalance: 0,
  icon: 'cash',
  groupName: '',
  remark: '',
  includeInTotal: 1,
  balanceDisabled: true,
})

const TYPE_LABELS: Record<string, string> = ACCOUNT_TYPE_LABELS
const TYPE_ORDER = ACCOUNT_TYPE_ORDER

const TYPE_META: Record<AccountType, { emoji: string; bg: string; desc: string }> = {
  asset: { emoji: '¥', bg: '#FFF3E0', desc: '储蓄卡、现金、电子钱包' },
  credit: { emoji: '💳', bg: '#ECEFF1', desc: '信用卡、花呗、白条' },
  stored_value: { emoji: '🎫', bg: '#E3F2FD', desc: '会员卡、储值卡、预付卡' },
}

function typeLabel(t: string) {
  return TYPE_LABELS[t] || t
}
function typeMeta(t: AccountType) {
  return TYPE_META[t]
}

function iconOption(key?: string): IconOption {
  return ICON_OPTIONS.find((i) => i.key === key) || ICON_OPTIONS[0]
}
function iconEmoji(key?: string) { return iconOption(key).emoji }
function iconLabel(key?: string) { return iconOption(key).label }
function iconBg(key?: string) { return iconOption(key).bg }

function openCreate() {
  createStep.value = 1
  showCreate.value = true
}

function pickType(t: AccountType) {
  createForm.type = t
  createForm.groupName = ''
  createStep.value = 2
}

function resetCreate() {
  createStep.value = 1
  createForm.name = ''
  createForm.type = 'asset'
  createForm.initialBalance = 0
  createForm.icon = 'cash'
  createForm.groupName = ''
  createForm.remark = ''
  createForm.includeInTotal = 1
  createForm.balanceDisabled = true
}

/* ---------- 初始余额：默认置灰 0.00，点击置空可输入 ---------- */
const balanceInput = ref()
function onBalanceClick() {
  if (createForm.balanceDisabled) {
    createForm.balanceDisabled = false
    createForm.initialBalance = null
    nextTick(() => balanceInput.value?.focus())
  }
}
function onBalanceBlur() {
  const v = createForm.initialBalance
  if (v == null || v === 0) {
    createForm.initialBalance = 0
    createForm.balanceDisabled = true
  }
}

/* ---------- 调整余额 ---------- */
const showAdjust = ref(false)
const adjusting = ref(false)
const adjustTarget = ref<AccountVO | null>(null)
const adjustForm = reactive({ newBalance: 0, reason: '' })

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
      icon: createForm.icon,
      groupName: createForm.groupName.trim() || undefined,
      remark: createForm.remark.trim() || undefined,
      includeInTotal: createForm.includeInTotal,
    })
    ElMessage.success('账户已创建')
    showCreate.value = false
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
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  flex-shrink: 0;
}
.acc-emoji {
  font-size: 18px;
}
.acc-main {
  flex: 1;
  min-width: 0;
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

/* ===== 新增账户对话框 ===== */
.create-account-dialog :deep(.el-dialog__body) {
  padding: 8px 24px 16px;
}
.create-account-dialog :deep(.el-dialog__footer) {
  padding: 8px 24px 20px;
}

.dialog-header {
  text-align: center;
  margin-bottom: 20px;
}
.header-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  margin: 0 auto 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.header-emoji {
  font-size: 28px;
}
.header-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
.header-sub {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

/* Step 1：类型卡片 */
.type-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.type-card {
  display: flex;
  align-items: center;
  padding: 16px;
  border-radius: 12px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  cursor: pointer;
  transition: all 0.2s;
}
.type-card:hover {
  background: #f5f7fa;
  border-color: var(--el-color-primary-light-5);
  transform: translateY(-1px);
}
.type-card-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 14px;
  flex-shrink: 0;
}
.type-card-emoji {
  font-size: 22px;
}
.type-card-info {
  flex: 1;
}
.type-card-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.type-card-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.type-card-arrow {
  color: #c0c4cc;
  font-size: 18px;
}

/* Step 2：表单 */
.form-topbar {
  margin-bottom: 4px;
}
.back-btn {
  color: var(--el-color-primary);
  font-size: 14px;
  padding: 4px 8px;
}
.back-btn:hover {
  background: var(--el-color-primary-light-9);
}

.form-section {
  margin-bottom: 18px;
}
.section-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
}
.section-label .el-icon {
  color: var(--el-color-primary);
}
.form-input {
  width: 100%;
}

/* 初始余额：默认置灰，点击置空 */
.balance-wrap {
  cursor: pointer;
}
.balance-wrap.is-gray .el-input__wrapper {
  background-color: #f5f7fa;
  box-shadow: none;
}
.balance-wrap.is-gray input {
  caret-color: transparent;
}
.balance-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

/* 图标网格 */
.icon-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  padding: 12px;
  background: #fafafa;
  border-radius: 12px;
  border: 1px solid #ebeef5;
}
.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 6px 2px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  border: 2px solid transparent;
}
.icon-item:hover {
  background: #fff;
}
.icon-item.active {
  border-color: var(--el-color-primary);
  background: #fff;
}
.icon-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.icon-emoji {
  font-size: 18px;
}
.icon-name {
  font-size: 11px;
  color: #606266;
  text-align: center;
  line-height: 1.2;
}

/* 计入总资产 */
.include-total {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fafafa;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  margin-bottom: 0;
}
.include-total-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.include-total-info {
  flex: 1;
}
.include-total-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}
.include-total-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.dialog-footer .el-button {
  min-width: 88px;
}

/* 移动端适配：图标网格减少列数、内边距收紧 */
@media (max-width: 768px) {
  .accounts-page {
    padding: 14px 12px;
  }
  .icon-grid {
    grid-template-columns: repeat(5, 1fr);
    gap: 6px;
    padding: 8px;
  }
  .summary {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
