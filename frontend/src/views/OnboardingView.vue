<template>
  <div class="onboarding">
    <el-card class="card">
      <h2>欢迎使用家庭记账</h2>
      <p class="sub">加入一个家庭，和家人一起记账</p>
      <el-tabs v-model="tab">
        <el-tab-pane label="创建家庭" name="create">
          <el-input v-model="familyName" placeholder="家庭名称，如：我们一家" maxlength="64" />
          <el-button type="primary" class="mt" :loading="loading" @click="onCreate">
            创建
          </el-button>
        </el-tab-pane>
        <el-tab-pane label="加入家庭" name="join">
          <el-input v-model="inviteCode" placeholder="输入 8 位邀请码" maxlength="8" />
          <el-button type="primary" class="mt" :loading="loading" @click="onJoin">
            加入
          </el-button>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useFamilyStore } from '@/stores/family'

const router = useRouter()
const familyStore = useFamilyStore()

const tab = ref('create')
const familyName = ref('')
const inviteCode = ref('')
const loading = ref(false)

async function onCreate() {
  if (!familyName.value.trim()) {
    ElMessage.warning('请输入家庭名称')
    return
  }
  loading.value = true
  try {
    await familyStore.create(familyName.value.trim())
    ElMessage.success('家庭创建成功')
    router.push('/')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '创建失败')
  } finally {
    loading.value = false
  }
}

async function onJoin() {
  if (!inviteCode.value.trim()) {
    ElMessage.warning('请输入邀请码')
    return
  }
  loading.value = true
  try {
    await familyStore.join(inviteCode.value.trim().toUpperCase())
    ElMessage.success('已加入家庭')
    router.push('/')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加入失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.onboarding {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.card {
  width: 420px;
}
.sub {
  color: #888;
  margin-bottom: 16px;
}
.mt {
  margin-top: 16px;
  width: 100%;
}
</style>
