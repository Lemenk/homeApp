<template>
  <el-dialog
    :model-value="modelValue"
    :title="editing ? '编辑账户' : '账户详情'"
    width="480px"
    class="account-detail-dialog"
    @update:model-value="$emit('update:modelValue', $event)"
    @closed="onClosed"
  >
    <!-- 查看模式 -->
    <div v-if="!editing && account" class="detail-view">
      <div class="detail-header">
        <div class="detail-icon" :style="{ background: iconBg(account.icon) }">
          <AppIcon :icon="account.icon" :size="32" />
        </div>
        <div class="detail-title">
          <div class="detail-name">{{ account.name }}</div>
          <div class="detail-type">{{ typeLabel(account.type) }}</div>
        </div>
      </div>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="余额">¥ {{ account.balance.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="账户分组">{{ typeLabel(account.type) }}</el-descriptions-item>
        <el-descriptions-item label="初始余额">¥ {{ account.initialBalance.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ account.remark || '—' }}</el-descriptions-item>
        <el-descriptions-item label="计入总资产">{{ account.includeInTotal === 0 ? '否' : '是' }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- 编辑模式 -->
    <el-form v-else label-width="96px" class="edit-form">
      <el-form-item label="账户名称">
        <el-input v-model="editForm.name" placeholder="请输入账户名称" maxlength="64" />
      </el-form-item>
      <el-form-item label="账户余额">
        <el-input-number v-model="editForm.balance" :precision="2" :controls="false" style="width: 100%" />
      </el-form-item>
      <el-form-item label="账户分组">
        <el-select v-model="editForm.type" style="width: 100%">
          <el-option v-for="t in TYPE_ORDER" :key="t" :value="t" :label="ACCOUNT_TYPE_LABELS[t]" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="editForm.remark" type="textarea" :rows="2" placeholder="选填" maxlength="255" />
      </el-form-item>
      <el-form-item label="计入总资产">
        <el-switch v-model="editForm.includeInTotal" :active-value="1" :inactive-value="0" />
        <span class="edit-include-tip">{{ editForm.includeInTotal === 1 ? '余额计入资产总额' : '不计入资产总额' }}</span>
      </el-form-item>
    </el-form>

    <template #footer>
      <template v-if="!editing">
        <el-button @click="$emit('update:modelValue', false)">关闭</el-button>
        <el-button type="primary" @click="startEdit">编辑账户</el-button>
      </template>
      <template v-else>
        <el-button @click="editing = false">取消</el-button>
        <el-button type="primary" :loading="updating" @click="submitEdit">保存</el-button>
      </template>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { updateAccount, ACCOUNT_TYPE_LABELS, ACCOUNT_TYPE_ORDER } from '@/api/account'
import type { AccountVO, AccountType } from '@/api/account'
import { iconBg } from '@/utils/accountIcon'
import AppIcon from '@/components/AppIcon.vue'

const props = defineProps<{
  modelValue: boolean
  account: AccountVO | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'saved'): void
}>()

const TYPE_ORDER = ACCOUNT_TYPE_ORDER

const editing = ref(false)
const updating = ref(false)
const editForm = reactive({ name: '', balance: 0, type: 'asset' as AccountType, remark: '', includeInTotal: 1 })

function typeLabel(t: AccountType): string {
  return ACCOUNT_TYPE_LABELS[t] || t
}

/** 每次打开时把当前账户快照填入编辑表单（immediate：挂载时即初始化） */
watch(
  () => [props.modelValue, props.account] as const,
  ([visible, a]) => {
    if (visible && a) {
      editing.value = false
      editForm.name = a.name
      editForm.balance = a.balance
      editForm.type = a.type
      editForm.remark = a.remark || ''
      editForm.includeInTotal = a.includeInTotal ?? 1
    }
  },
  { immediate: true }
)

function onClosed() {
  editing.value = false
}

function startEdit() {
  editing.value = true
}

async function submitEdit() {
  if (!props.account) return
  if (!editForm.name.trim()) {
    ElMessage.warning('请输入账户名称')
    return
  }
  updating.value = true
  try {
    await updateAccount(props.account.id, {
      name: editForm.name.trim(),
      type: editForm.type,
      remark: editForm.remark.trim() || undefined,
      includeInTotal: editForm.includeInTotal,
      balance: editForm.balance,
    })
    ElMessage.success('账户已更新')
    emit('update:modelValue', false)
    emit('saved')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '更新失败')
  } finally {
    updating.value = false
  }
}
</script>

<style scoped>
.account-detail-dialog :deep(.el-dialog__body) {
  padding: 8px 24px 16px;
}
.detail-view {
  padding: 4px 0;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}
.detail-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.detail-title {
  min-width: 0;
}
.detail-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.detail-type {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.edit-include-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}
</style>
