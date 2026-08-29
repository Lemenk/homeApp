<template>
  <el-dialog
    :model-value="modelValue"
    :title="editingId ? '编辑账单' : '记一笔'"
    width="560px"
    :close-on-click-modal="false"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
    @closed="reset"
  >
    <el-steps :active="step" align-center finish-status="success" style="margin-bottom: 20px">
      <el-step title="类型" />
      <el-step title="金额分类" />
      <el-step title="账户" />
      <el-step title="补充" />
    </el-steps>

    <!-- Step 0: 类型 -->
    <div v-if="step === 0">
      <el-radio-group v-model="type" size="large" class="type-group">
        <el-radio-button value="expense">支出</el-radio-button>
        <el-radio-button value="income">收入</el-radio-button>
        <el-radio-button value="transfer">转账</el-radio-button>
      </el-radio-group>
      <div class="step-footer">
        <el-button type="primary" @click="step = 1">下一步</el-button>
      </div>
    </div>

    <!-- Step 1: 金额 + 分类 -->
    <div v-else-if="step === 1">
      <div class="amount-row">
        <span class="currency">¥</span>
        <el-input-number
          v-model="amount"
          :min="0.01"
          :precision="2"
          :controls="false"
          class="amount-input"
          placeholder="输入金额"
        />
      </div>
      <template v-if="type !== 'transfer'">
        <!-- 金额已填但未选分类时给出明确引导，避免“下一步”点了没反应 -->
        <el-alert
          v-if="amount !== null && amount > 0 && !categoryId"
          type="warning"
          :closable="false"
          show-icon
          title="请选择分类后继续"
          description="选择下方的支出/收入分类即可进入下一步"
          style="margin-bottom: 8px"
        />
        <div class="cat-grid">
          <div
            v-for="c in categories"
            :key="c.id"
            class="cat-item"
            :class="{ active: categoryId === c.id }"
            @click="categoryId = c.id"
          >
            <div class="cat-icon">{{ c.icon || '💰' }}</div>
            <div class="cat-name">{{ c.name }}</div>
          </div>
        </div>
      </template>
      <div class="step-footer">
        <el-button @click="step = 0">上一步</el-button>
        <el-button
          type="primary"
          :disabled="!amount || amount <= 0"
          @click="tryNext"
        >
          下一步
        </el-button>
      </div>
    </div>

    <!-- Step 2: 账户 -->
    <div v-else-if="step === 2">
      <template v-if="type === 'transfer'">
        <div class="field">
          <label>转出账户</label>
          <el-select v-model="transferFrom" placeholder="选择转出账户" filterable style="width: 100%">
            <el-option v-for="a in accounts" :key="a.id" :label="`${a.name}（余额 ${a.balance}）`" :value="a.id" />
          </el-select>
        </div>
        <div class="field">
          <label>转入账户</label>
          <el-select v-model="transferTo" placeholder="选择转入账户" filterable style="width: 100%">
            <el-option v-for="a in accounts" :key="a.id" :label="`${a.name}（余额 ${a.balance}）`" :value="a.id" />
          </el-select>
        </div>
        <el-alert v-if="transferFrom && transferTo && transferFrom === transferTo" type="warning" :closable="false"
          title="转出与转入账户不能相同" show-icon style="margin-top: 8px" />
      </template>
      <template v-else>
        <div class="split-list">
          <div v-for="it in items" :key="it.key" class="split-row">
            <el-select v-model="it.accountId" filterable placeholder="选择账户" style="flex: 1">
              <el-option v-for="a in accounts" :key="a.id" :label="`${a.name}（余额 ${a.balance}）`" :value="a.id" />
            </el-select>
            <el-input-number v-model="it.amount" :min="0.01" :precision="2" :controls="false" class="split-amount" />
            <el-button text type="danger" @click="removeItem(it.key)">删除</el-button>
          </div>
        </div>
        <el-button size="small" @click="addItem">+ 添加账户</el-button>
        <el-alert
          v-if="!splitOk"
          type="warning"
          :closable="false"
          title="各账户金额合计需等于账单金额"
          show-icon
          style="margin-top: 8px"
        />
      </template>
      <div class="step-footer">
        <el-button @click="step = 1">上一步</el-button>
        <el-button type="primary" :disabled="!step2Ok" @click="step = 3">下一步</el-button>
      </div>
    </div>

    <!-- Step 3: 补充信息 -->
    <div v-else>
      <div class="field">
        <label>日期</label>
        <el-date-picker
          v-model="billDate"
          type="datetime"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="选择时间"
          style="width: 100%"
        />
      </div>
      <div class="field">
        <label>备注</label>
        <el-input v-model="remark" maxlength="255" placeholder="记录一下这笔账的用途…" />
      </div>
      <div class="field">
        <label>标签</label>
        <el-select v-model="tagIds" multiple filterable collapse-tags placeholder="选择标签" style="width: 100%">
          <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
      </div>
      <div v-if="isPublicLedger" class="field">
        <label>记账人</label>
        <el-select v-model="memberId" filterable placeholder="为谁记账" style="width: 100%">
          <el-option v-for="m in members" :key="m.userId" :label="m.nickname" :value="m.userId" />
        </el-select>
      </div>
      <div class="step-footer">
        <el-button @click="step = 2">上一步</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createBill, updateBill } from '@/api/bill'
import type { BillVO } from '@/api/bill'
import { listAccounts } from '@/api/account'
import { listCategories, listTags } from '@/api/ledger'
import type { CategoryVO, TagVO } from '@/api/ledger'
import { useLedgerStore } from '@/stores/ledger'
import { useFamilyStore } from '@/stores/family'
import { useUserStore } from '@/stores/user'

const props = defineProps<{
  modelValue: boolean
  ledgerId: number
  editing?: BillVO | null
}>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'saved'): void
}>()

const ledgerStore = useLedgerStore()
const familyStore = useFamilyStore()
const userStore = useUserStore()

const step = ref(0)
const type = ref<'expense' | 'income' | 'transfer'>('expense')
const amount = ref<number | null>(null)
const categoryId = ref<number | null>(null)
const billDate = ref('')
const remark = ref('')
const tagIds = ref<number[]>([])
const memberId = ref<number | null>(null)
const submitting = ref(false)
const categories = ref<CategoryVO[]>([])
const tags = ref<TagVO[]>([])
const accounts = ref<Awaited<ReturnType<typeof listAccounts>>>([])
const members = ref<{ userId: number; nickname: string }[]>([])

let keySeq = 1
const items = ref<{ key: number; accountId: number | null; amount: number | null }[]>([])
const transferFrom = ref<number | null>(null)
const transferTo = ref<number | null>(null)

const editingId = computed(() => props.editing?.id || 0)
const isPublicLedger = computed(() => ledgerStore.ledgers.find((l) => l.id === props.ledgerId)?.type === 'public')

const splitOk = computed(() => {
  if (!amount.value) return false
  const sum = items.value.reduce((s, it) => s + (it.amount || 0), 0)
  return Math.abs(sum - amount.value) < 0.001
})

const step2Ok = computed(() => {
  if (type.value === 'transfer') {
    return transferFrom.value && transferTo.value && transferFrom.value !== transferTo.value
  }
  return items.value.length > 0 && items.value.every((it) => it.accountId && it.amount && it.amount > 0) && splitOk.value
})

function addItem(accountId: number | null = null, amt: number | null = null) {
  items.value.push({ key: keySeq++, accountId, amount: amt })
}
function removeItem(key: number) {
  items.value = items.value.filter((it) => it.key !== key)
}

/** 第 2 步入口：金额有效即可点击；支出/收入未选分类时给出明确提示，避免按钮无反应 */
function tryNext() {
  if (type.value !== 'transfer' && !categoryId.value) {
    ElMessage.warning('请先选择分类')
    return
  }
  step.value = 2
}

async function loadData() {
  accounts.value = await listAccounts(props.ledgerId)
  categories.value = (await listCategories(props.ledgerId)).filter((c) => c.enabled !== 0)
  tags.value = await listTags(props.ledgerId)
  if (isPublicLedger.value) {
    members.value = await familyStore.fetchMembers()
  }
}

async function loadForEdit() {
  if (!props.editing) return
  const b = props.editing
  type.value = b.type
  amount.value = b.amount
  categoryId.value = b.categoryId || null
  billDate.value = b.billDate.slice(0, 19)
  remark.value = b.remark || ''
  tagIds.value = b.tags.map((t) => t.id)
  memberId.value = b.memberId || null
  if (b.type === 'transfer') {
    const out = b.accounts.find((a) => a.direction === 'out')
    const inn = b.accounts.find((a) => a.direction === 'in')
    transferFrom.value = out?.accountId || null
    transferTo.value = inn?.accountId || null
  } else {
    items.value = b.accounts.map((a) => ({
      key: keySeq++,
      accountId: a.accountId,
      amount: a.amount,
    }))
  }
}

function reset() {
  step.value = 0
  type.value = 'expense'
  amount.value = null
  categoryId.value = null
  billDate.value = new Date().toISOString().slice(0, 19).replace('T', 'T')
  remark.value = ''
  tagIds.value = []
  memberId.value = userStore.user?.id || null
  items.value = []
  transferFrom.value = null
  transferTo.value = null
  keySeq = 1
}

async function submit() {
  if (!amount.value) return
  submitting.value = true
  try {
    let itemsPayload: { accountId: number; direction: 'out' | 'in'; amount: number }[]
    if (type.value === 'transfer') {
      itemsPayload = [
        { accountId: transferFrom.value!, direction: 'out', amount: amount.value },
        { accountId: transferTo.value!, direction: 'in', amount: amount.value },
      ]
    } else {
      const dir = type.value === 'expense' ? 'out' : 'in'
      itemsPayload = items.value.map((it) => ({ accountId: it.accountId!, direction: dir, amount: it.amount! }))
    }
    const payload = {
      type: type.value,
      categoryId: type.value === 'transfer' ? undefined : categoryId.value || undefined,
      memberId: memberId.value || undefined,
      amount: amount.value,
      billDate: billDate.value || undefined,
      remark: remark.value || undefined,
      tagIds: tagIds.value.length ? tagIds.value : undefined,
      items: itemsPayload,
    }
    if (editingId.value) {
      await updateBill(editingId.value, { ...payload, ledgerId: props.ledgerId })
      ElMessage.success('账单已更新')
    } else {
      await createBill(props.ledgerId, payload)
      ElMessage.success('记账成功')
    }
    emit('update:modelValue', false)
    emit('saved')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      loadData()
      if (props.editing) {
        loadForEdit()
      } else {
        reset()
      }
    }
  },
  // immediate：避免组件以 modelValue=true 首次挂载（如刷新/重挂载）时分类/账户为空
  { immediate: true }
)

onMounted(() => {
  reset()
})
</script>

<style scoped>
.step-footer {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.amount-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 16px;
}
.currency {
  font-size: 28px;
  color: var(--el-text-color-primary);
}
.amount-input {
  width: 240px;
}
.amount-input :deep(.el-input__inner) {
  font-size: 30px;
  font-weight: 600;
  text-align: left;
}
.cat-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
  max-height: 260px;
  overflow-y: auto;
}
.cat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 4px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid transparent;
}
.cat-item:hover {
  background: var(--el-fill-color-light);
}
.cat-item.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.cat-icon {
  font-size: 22px;
}
.cat-name {
  font-size: 12px;
  margin-top: 4px;
}
.field {
  margin-bottom: 14px;
}
.field label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.split-list {
  margin-bottom: 8px;
}
.split-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.split-amount {
  width: 120px;
}
.type-group {
  display: flex;
  justify-content: center;
  width: 100%;
}
</style>
