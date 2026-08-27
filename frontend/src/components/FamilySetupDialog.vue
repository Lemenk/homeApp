<template>
  <el-dialog
    :model-value="modelValue"
    title="家庭设置"
    width="440px"
    :close-on-click-modal="false"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <el-tabs v-model="tab">
      <el-tab-pane label="创建家庭" name="create">
        <p class="tip">创建家庭后，可建立公共账本与家人共享记账</p>
        <el-input v-model="familyName" placeholder="家庭名称，如：我们一家" maxlength="64" />
        <el-button type="primary" class="mt" :loading="creating" @click="onCreate">创建</el-button>
      </el-tab-pane>
      <el-tab-pane label="加入家庭" name="join">
        <p class="tip">输入家人分享的 8 位邀请码即可加入</p>
        <el-input v-model="inviteCode" placeholder="输入 8 位邀请码" maxlength="8" @keyup.enter="onJoin" />
        <el-button type="primary" class="mt" :loading="joining" @click="onJoin">加入</el-button>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useFamilyStore } from '@/stores/family'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const familyStore = useFamilyStore()

const tab = ref('create')
const familyName = ref('')
const inviteCode = ref('')
const creating = ref(false)
const joining = ref(false)

// 每次打开时重置表单
watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      familyName.value = ''
      inviteCode.value = ''
      tab.value = 'create'
    }
  }
)

async function onCreate() {
  if (!familyName.value.trim()) {
    ElMessage.warning('请输入家庭名称')
    return
  }
  creating.value = true
  try {
    await familyStore.create(familyName.value.trim())
    ElMessage.success('家庭创建成功')
    emit('update:modelValue', false)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

async function onJoin() {
  if (!inviteCode.value.trim()) {
    ElMessage.warning('请输入邀请码')
    return
  }
  joining.value = true
  try {
    await familyStore.join(inviteCode.value.trim().toUpperCase())
    ElMessage.success('已加入家庭')
    emit('update:modelValue', false)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加入失败')
  } finally {
    joining.value = false
  }
}
</script>

<style scoped>
.tip {
  color: #909399;
  font-size: 13px;
  margin: 0 0 12px;
}
.mt {
  margin-top: 16px;
  width: 100%;
}
</style>
