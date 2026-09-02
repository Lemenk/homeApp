<template>
  <div class="config-page">
    <div class="page-header">
      <h2>配置中心</h2>
      <span class="header-sub">管理账本、账户和账单分类</span>
    </div>

    <div class="config-columns">
      <!-- 左侧：账本区 -->
      <div class="config-col">
        <div class="col-header">
          <div class="col-title">
            <el-icon class="col-icon ledger"><Notebook /></el-icon>
            <span>账本</span>
          </div>
          <el-button type="primary" size="small" round @click="showLedgerDialog = true">
            <el-icon><Plus /></el-icon>新增
          </el-button>
        </div>
        <div class="col-body">
          <div v-for="l in ledgers" :key="l.id" class="item-card" :class="{ active: l.id === ledgerStore.currentLedgerId, default: l.isDefault === 1 }">
            <div class="item-icon">📒</div>
            <div class="item-main">
              <div class="item-name">
                {{ l.name }}
                <el-tag v-if="l.isDefault === 1" size="small" type="primary" effect="light" class="default-tag">默认</el-tag>
              </div>
              <div class="item-desc">{{ l.type === 'public' ? '公共账本' : '个人账本' }} · {{ l.memberCount || 1 }}人</div>
            </div>
            <el-button text size="small" type="info" @click="$router.push(`/ledgers/${l.id}/settings`)">管理</el-button>
            <el-button
              v-if="l.isDefault !== 1 && ledgers.length > 1"
              text
              size="small"
              type="primary"
              @click="setAsDefault(l)"
            >设为默认</el-button>
            <el-button
              v-else-if="l.isDefault === 1"
              text
              size="small"
              disabled
            >默认账本</el-button>
            <el-button v-else text size="small" type="primary" @click="switchLedger(l.id)">切换</el-button>
          </div>
          <el-empty v-if="!ledgers.length" :image-size="50" description="暂无账本" />
        </div>
      </div>

      <!-- 中间：账户区 -->
      <div class="config-col">
        <div class="col-header">
          <div class="col-title">
            <el-icon class="col-icon account"><Wallet /></el-icon>
            <span>账户</span>
          </div>
          <el-button type="primary" size="small" round @click="openAccountDialog">
            <el-icon><Plus /></el-icon>新增
          </el-button>
        </div>
        <div class="col-body">
          <div v-for="group in groupedAccounts" :key="group.type" class="account-group">
            <div class="group-label">{{ typeLabel(group.type) }}</div>
            <div v-for="a in group.accounts" :key="a.id" class="item-card" @click="openAccountDetail(a)">
              <div class="item-icon" :style="{ background: iconBg(a.icon) }">
                <AppIcon :icon="a.icon" :size="20" />
              </div>
              <div class="item-main">
                <div class="item-name">{{ a.name }}</div>
                <div class="item-desc">{{ a.groupName || '默认分组' }}</div>
              </div>
              <div class="item-balance">{{ a.balance.toFixed(2) }}</div>
            </div>
            <el-empty v-if="!group.accounts.length" :image-size="40" description="暂无账户" />
          </div>
        </div>
      </div>

      <!-- 右侧：账单类型（分类）区 -->
      <div class="config-col">
        <div class="col-header">
          <div class="col-title">
            <el-icon class="col-icon category"><Menu /></el-icon>
            <span>账单类型</span>
          </div>
          <el-button type="primary" size="small" round @click="openCategoryDialog">
            <el-icon><Plus /></el-icon>新增
          </el-button>
        </div>
        <div class="col-body">
          <el-radio-group v-model="categoryTab" size="small" class="cat-tab">
            <el-radio-button value="expense">支出</el-radio-button>
            <el-radio-button value="income">收入</el-radio-button>
          </el-radio-group>
          <div class="cat-list">
            <div v-for="c in filteredCategories" :key="c.id" class="item-card cat-item">
              <div class="item-icon cat-icon">{{ c.icon || '📂' }}</div>
              <div class="item-main">
                <div class="item-name">{{ c.name }}</div>
                <div class="item-desc">{{ c.enabled ? '已启用' : '已禁用' }}</div>
              </div>
              <el-switch
                :model-value="c.enabled === 1"
                size="small"
                @change="(v: string | number | boolean) => toggleCategory(c, !!v)"
              />
            </div>
            <el-empty v-if="!filteredCategories.length" :image-size="50" description="暂无分类" />
          </div>
        </div>
      </div>
    </div>

    <!-- 新增账本对话框 -->
    <el-dialog v-model="showLedgerDialog" title="新增账本" width="400px">
      <el-form label-width="70px">
        <el-form-item label="账本名称">
          <el-input v-model="ledgerForm.name" placeholder="如：家庭账本" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="ledgerForm.type">
            <el-radio value="personal">个人账本</el-radio>
            <el-radio value="public">公共账本</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showLedgerDialog = false">取消</el-button>
        <el-button type="primary" :loading="creatingLedger" @click="submitLedger">创建</el-button>
      </template>
    </el-dialog>

    <!-- 新增账户对话框 -->
    <el-dialog v-model="showAccountDialog" title="新增账户" width="400px">
      <el-form label-width="70px">
        <el-form-item label="账户名称">
          <el-input v-model="accountForm.name" placeholder="如：微信钱包" />
        </el-form-item>
        <el-form-item label="账户类型">
          <el-select v-model="accountForm.type" style="width: 100%">
            <el-option value="asset" label="资金账户" />
            <el-option value="credit" label="信贷账户" />
            <el-option value="stored_value" label="储值账户" />
          </el-select>
        </el-form-item>
        <el-form-item label="余额">
          <div class="balance-wrap" :class="{ 'is-gray': accountForm.balanceDisabled }" @click="onAccountBalanceClick">
            <el-input-number ref="accountBalanceInput" v-model="accountForm.balance" :precision="2" :controls="false" style="width: 100%" :readonly="accountForm.balanceDisabled" @blur="onAccountBalanceBlur" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAccountDialog = false">取消</el-button>
        <el-button type="primary" :loading="creatingAccount" @click="submitAccount">创建</el-button>
      </template>
    </el-dialog>

    <!-- 新增分类对话框 -->
    <el-dialog v-model="showCategoryDialog" title="新增账单类型" width="400px">
      <el-form label-width="70px">
        <el-form-item label="分类名称">
          <el-input v-model="categoryForm.name" placeholder="如：餐饮" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="categoryForm.type">
            <el-radio value="expense">支出</el-radio>
            <el-radio value="income">收入</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="categoryForm.icon" placeholder="emoji，如：🍜" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCategoryDialog = false">取消</el-button>
        <el-button type="primary" :loading="creatingCategory" @click="submitCategory">创建</el-button>
      </template>
    </el-dialog>

    <!-- 账户详情 / 编辑（公共组件） -->
    <AccountDetailDialog v-model="showAccountDetail" :account="detailAccount" @saved="loadAccounts" />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Notebook, Wallet, Menu, Plus } from '@element-plus/icons-vue'
import { useLedgerStore } from '@/stores/ledger'
import { listLedgers, createLedger, listCategories, createCategory, toggleCategory as toggleCategoryApi } from '@/api/ledger'
import type { CategoryVO } from '@/api/ledger'
import type { LedgerVO } from '@/types'
import { listAccounts, createAccount, ACCOUNT_TYPE_LABELS, ACCOUNT_TYPE_ORDER } from '@/api/account'
import type { AccountVO, AccountType } from '@/api/account'
import { iconBg } from '@/utils/accountIcon'
import AppIcon from '@/components/AppIcon.vue'
import AccountDetailDialog from '@/components/AccountDetailDialog.vue'

const ledgerStore = useLedgerStore()
const registerRefresh = inject<(fn: () => Promise<void>) => void>('registerRefresh')

async function loadAll() {
  await Promise.all([loadLedgers(), loadAccounts(), loadCategories()])
}

const ledgers = ref<LedgerVO[]>([])
const accounts = ref<AccountVO[]>([])
const categories = ref<CategoryVO[]>([])
const categoryTab = ref<'expense' | 'income'>('expense')

// 新增账本
const showLedgerDialog = ref(false)
const creatingLedger = ref(false)
const ledgerForm = reactive({ name: '', type: 'personal' as 'personal' | 'public' })

// 新增账户
const showAccountDialog = ref(false)
const creatingAccount = ref(false)
const accountForm = reactive<{ name: string; type: AccountType; balance: number | null; balanceDisabled: boolean }>({
  name: '',
  type: 'asset',
  balance: 0,
  balanceDisabled: true,
})

/* 余额：默认置灰 0.00，点击置空可输入 */
const accountBalanceInput = ref()
function onAccountBalanceClick() {
  if (accountForm.balanceDisabled) {
    accountForm.balanceDisabled = false
    accountForm.balance = null
    nextTick(() => accountBalanceInput.value?.focus())
  }
}
function onAccountBalanceBlur() {
  const v = accountForm.balance
  if (v == null || v === 0) {
    accountForm.balance = 0
    accountForm.balanceDisabled = true
  }
}

// 新增分类
const showCategoryDialog = ref(false)
const creatingCategory = ref(false)
const categoryForm = reactive({ name: '', type: 'expense' as 'expense' | 'income', icon: '' })

const groupedAccounts = computed(() =>
  ACCOUNT_TYPE_ORDER.map((t) => ({
    type: t,
    accounts: accounts.value.filter((a) => a.type === t),
  }))
)

const filteredCategories = computed(() =>
  categories.value.filter((c) => c.type === categoryTab.value)
)

function typeLabel(t: AccountType): string {
  return ACCOUNT_TYPE_LABELS[t] || t
}

/* 账户详情 / 编辑 */
const showAccountDetail = ref(false)
const detailAccount = ref<AccountVO | null>(null)
function openAccountDetail(a: AccountVO) {
  detailAccount.value = a
  showAccountDetail.value = true
}

function switchLedger(id: number) {
  ledgerStore.switchTo(id)
  loadAccounts()
  loadCategories()
  ElMessage.success('已切换账本')
}

async function setAsDefault(l: LedgerVO) {
  await ledgerStore.setDefault(l.id)
  await loadLedgers()
  loadAccounts()
  loadCategories()
  ElMessage.success(`已将「${l.name}」设为默认账本`)
}

function openAccountDialog() {
  if (!ledgerStore.currentLedgerId) {
    ElMessage.warning('请先创建或切换账本')
    return
  }
  accountForm.name = ''
  accountForm.type = 'asset'
  accountForm.balance = 0
  accountForm.balanceDisabled = true
  showAccountDialog.value = true
}

function openCategoryDialog() {
  if (!ledgerStore.currentLedgerId) {
    ElMessage.warning('请先创建或切换账本')
    return
  }
  categoryForm.name = ''
  categoryForm.type = categoryTab.value
  categoryForm.icon = ''
  showCategoryDialog.value = true
}

async function loadLedgers() {
  ledgers.value = await listLedgers()
}

async function loadAccounts() {
  if (!ledgerStore.currentLedgerId) return
  accounts.value = await listAccounts(ledgerStore.currentLedgerId)
}

async function loadCategories() {
  if (!ledgerStore.currentLedgerId) return
  categories.value = await listCategories(ledgerStore.currentLedgerId)
}

async function submitLedger() {
  if (!ledgerForm.name.trim()) {
    ElMessage.warning('请输入账本名称')
    return
  }
  creatingLedger.value = true
  try {
    const ledger = await createLedger({ name: ledgerForm.name.trim(), type: ledgerForm.type })
    ElMessage.success('账本已创建')
    showLedgerDialog.value = false
    ledgerForm.name = ''
    await loadLedgers()
    await ledgerStore.fetch()
    ledgerStore.switchTo(ledger.id)
    await Promise.all([loadAccounts(), loadCategories()])
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '创建失败')
  } finally {
    creatingLedger.value = false
  }
}

async function submitAccount() {
  if (!accountForm.name.trim()) {
    ElMessage.warning('请输入账户名称')
    return
  }
  creatingAccount.value = true
  try {
    await createAccount(ledgerStore.currentLedgerId, {
      name: accountForm.name.trim(),
      type: accountForm.type,
      balance: accountForm.balance || undefined,
    })
    ElMessage.success('账户已创建')
    showAccountDialog.value = false
    await loadAccounts()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '创建失败')
  } finally {
    creatingAccount.value = false
  }
}

async function submitCategory() {
  if (!categoryForm.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  creatingCategory.value = true
  try {
    await createCategory(ledgerStore.currentLedgerId, {
      name: categoryForm.name.trim(),
      type: categoryForm.type,
      icon: categoryForm.icon || undefined,
    })
    ElMessage.success('分类已创建')
    showCategoryDialog.value = false
    await loadCategories()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '创建失败')
  } finally {
    creatingCategory.value = false
  }
}

async function toggleCategory(c: CategoryVO, enabled: boolean) {
  try {
    await toggleCategoryApi(c.id, enabled)
    c.enabled = enabled ? 1 : 0
  } catch (e: any) {
    ElMessage.error('操作失败')
    await loadCategories()
  }
}

onMounted(async () => {
  await ledgerStore.fetch()
  await loadAll()
  registerRefresh?.(loadAll)
})
</script>

<style scoped>
.config-page {
  padding: 20px 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

/* 余额：默认置灰，点击置空 */
.balance-wrap {
  cursor: pointer;
  width: 100%;
}
.balance-wrap.is-gray .el-input__wrapper {
  background-color: #f5f7fa;
  box-shadow: none;
}
.balance-wrap.is-gray input {
  caret-color: transparent;
}

.page-header {
  margin-bottom: 16px;
  flex-shrink: 0;
}
.page-header h2 {
  margin: 0 0 4px 0;
  font-size: 20px;
}
.header-sub {
  font-size: 13px;
  color: #909399;
}

.config-columns {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  flex: 1;
  min-height: 0;
}

.config-col {
  background: #fff;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.col-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  border-bottom: 1px solid #f0f2f5;
  flex-shrink: 0;
}
.col-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.col-icon {
  font-size: 18px;
}
.col-icon.ledger { color: #409eff; }
.col-icon.account { color: #67c23a; }
.col-icon.category { color: #e6a23c; }

.col-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 14px;
}

.item-card {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 8px;
  margin-bottom: 6px;
  background: #fafbfc;
  border: 1px solid transparent;
  transition: all 0.15s;
  cursor: pointer;
}
.item-card:hover {
  background: #f0f2f5;
  border-color: #e4e7ed;
}
.item-card.active {
  background: #ecf5ff;
  border-color: #b3d8ff;
}
.item-card.default {
  border-color: #c6e2ff;
  background: #f0f7ff;
}
.default-tag {
  margin-left: 4px;
}
.item-icon {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  margin-right: 10px;
  flex-shrink: 0;
  border: 1px solid #ebeef5;
}
.item-main {
  flex: 1;
  min-width: 0;
}
.item-name {
  display: flex;
  align-items: center;
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}
.item-desc {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}
.item-balance {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  flex-shrink: 0;
}

.account-group {
  margin-bottom: 14px;
}
.group-label {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
  margin: 4px 2px 8px;
}

.cat-tab {
  margin-bottom: 10px;
  width: 100%;
}
.cat-tab :deep(.el-radio-group) {
  width: 100%;
}
.cat-list {
  display: flex;
  flex-direction: column;
}
.cat-item {
  cursor: default;
}

/* 移动端适配：三列改为单列纵向排列 */
@media (max-width: 768px) {
  .config-page {
    padding: 14px 12px;
  }
  .config-columns {
    grid-template-columns: 1fr;
  }
  .config-col {
    min-height: 0;
  }
}
</style>
