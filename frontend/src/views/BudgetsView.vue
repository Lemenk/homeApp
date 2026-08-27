<template>
  <div class="budgets-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-head">
          <span>预算</span>
          <el-button type="primary" size="small" @click="openCreate">＋ 新建预算</el-button>
        </div>
      </template>

      <template v-if="budgets.length">
        <div v-for="b in budgets" :key="b.id" class="budget-row">
          <div class="budget-icon">{{ b.categoryIcon || '💰' }}</div>
          <div class="budget-main">
            <div class="budget-top">
              <span class="budget-name">{{ b.categoryName }}</span>
              <el-tag v-if="b.overBudget" type="danger" size="small">已超支</el-tag>
              <el-tag v-else-if="b.percent >= 80" type="warning" size="small">接近上限</el-tag>
              <span class="budget-period">
                {{ periodLabel(b) }}
              </span>
            </div>
            <el-progress
              :percentage="Math.min(b.percent, 100)"
              :status="b.overBudget ? 'exception' : b.percent >= 80 ? 'warning' : undefined"
              :stroke-width="12"
            />
            <div class="budget-nums">
              已用 ¥{{ b.usage.toFixed(2) }} / 预算 ¥{{ b.amount.toFixed(2) }}（{{ b.percent }}%）
            </div>
          </div>
          <div class="budget-actions">
            <el-button text size="small" @click="openEdit(b)">编辑</el-button>
            <el-popconfirm title="确定删除该预算？" confirm-button-text="删除" cancel-button-text="取消" @confirm="onDelete(b.id)">
              <template #reference>
                <el-button text size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </template>
      <el-empty v-else description="还没有设置预算，点击右上角“新建预算”为分类设置额度" />
    </el-card>

    <!-- 新建/编辑预算 -->
    <el-dialog v-model="showForm" :title="editingId ? '编辑预算' : '新建预算'" width="480px">
      <el-form label-width="90px">
        <el-form-item label="支出分类">
          <el-select v-model="form.categoryId" filterable style="width: 100%" placeholder="选择分类">
            <el-option
              v-for="c in expenseCategories"
              :key="c.id"
              :label="`${c.icon || ''} ${c.name}`"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="周期类型">
          <el-radio-group v-model="form.periodType">
            <el-radio-button value="monthly">每月</el-radio-button>
            <el-radio-button value="custom">自定义</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.periodType === 'custom'" label="周期">
          <el-date-picker
            v-model="form.range"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="预算金额">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" maxlength="64" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listBudgets, createBudget, updateBudget, deleteBudget } from '@/api/budget'
import type { BudgetVO } from '@/api/budget'
import { listCategories } from '@/api/ledger'
import type { CategoryVO } from '@/api/ledger'
import { useLedgerStore } from '@/stores/ledger'

const ledgerStore = useLedgerStore()
const budgets = ref<BudgetVO[]>([])
const expenseCategories = ref<CategoryVO[]>([])
const showForm = ref(false)
const saving = ref(false)
const editingId = ref(0)
const form = reactive<{
  categoryId: number | null
  periodType: 'monthly' | 'custom'
  range: [string, string] | null
  amount: number | null
  remark: string
}>({ categoryId: null, periodType: 'monthly', range: null, amount: null, remark: '' })

function periodLabel(b: BudgetVO) {
  if (b.periodType === 'monthly') return '本月'
  return `${b.startDate} ~ ${b.endDate}`
}

async function load() {
  if (!ledgerStore.currentLedgerId) return
  budgets.value = await listBudgets(ledgerStore.currentLedgerId)
}

async function loadCategories() {
  if (!ledgerStore.currentLedgerId) return
  expenseCategories.value = (await listCategories(ledgerStore.currentLedgerId)).filter((c) => c.enabled !== 0)
}

function openCreate() {
  editingId.value = 0
  form.categoryId = null
  form.periodType = 'monthly'
  form.range = null
  form.amount = null
  form.remark = ''
  showForm.value = true
}

function openEdit(b: BudgetVO) {
  editingId.value = b.id
  form.categoryId = b.categoryId
  form.periodType = b.periodType
  form.range = b.startDate && b.endDate ? [b.startDate, b.endDate] : null
  form.amount = b.amount
  form.remark = b.remark || ''
  showForm.value = true
}

async function submit() {
  if (!form.categoryId || !form.amount || form.amount <= 0) {
    ElMessage.warning('请选择分类并填写金额')
    return
  }
  if (form.periodType === 'custom' && !form.range) {
    ElMessage.warning('自定义周期需要选择起止日期')
    return
  }
  saving.value = true
  try {
    const payload: any = {
      categoryId: form.categoryId,
      periodType: form.periodType,
      amount: form.amount,
      remark: form.remark || undefined,
    }
    if (form.periodType === 'custom' && form.range) {
      payload.startDate = form.range[0]
      payload.endDate = form.range[1]
    }
    if (editingId.value) {
      await updateBudget(editingId.value, payload)
      ElMessage.success('预算已更新')
    } else {
      await createBudget(ledgerStore.currentLedgerId, payload)
      ElMessage.success('预算已创建')
    }
    showForm.value = false
    await load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function onDelete(id: number) {
  await deleteBudget(id)
  ElMessage.success('已删除')
  await load()
}

onMounted(async () => {
  await ledgerStore.fetch()
  await Promise.all([load(), loadCategories()])
})
</script>

<style scoped>
.budgets-page {
  padding: 20px;
  max-width: 960px;
  margin: 0 auto;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.budget-row {
  display: flex;
  align-items: flex-start;
  padding: 14px 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.budget-icon {
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
.budget-main {
  flex: 1;
  min-width: 0;
}
.budget-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.budget-name {
  font-size: 14px;
  font-weight: 600;
}
.budget-period {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
}
.budget-nums {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.budget-actions {
  display: flex;
}
</style>
