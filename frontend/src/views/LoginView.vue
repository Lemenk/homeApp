<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="title">家庭记账</h2>
      <el-form @submit.prevent="onLogin">
        <el-form-item>
          <el-input v-model="phone" placeholder="手机号" maxlength="11" />
        </el-form-item>
        <el-form-item>
          <div class="code-row">
            <el-input v-model="code" placeholder="验证码" maxlength="6" />
            <el-button :disabled="counting" @click="onSendCode">
              {{ counting ? `${countdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="onLogin">
          登录 / 注册
        </el-button>
        <p v-if="showDebugHint" class="hint">开发环境验证码：123456</p>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendCode } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const phone = ref('')
const code = ref('')
const loading = ref(false)
const counting = ref(false)
const countdown = ref(60)
const showDebugHint = ref(import.meta.env.DEV)

let timer: ReturnType<typeof setInterval> | null = null

async function onSendCode() {
  if (!/^1\d{10}$/.test(phone.value)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  await sendCode(phone.value)
  ElMessage.success('验证码已发送')
  counting.value = true
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      counting.value = false
      if (timer) clearInterval(timer)
    }
  }, 1000)
}

async function onLogin() {
  if (!/^1\d{10}$/.test(phone.value)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (!code.value) {
    ElMessage.warning('请输入验证码')
    return
  }
  loading.value = true
  try {
    await userStore.login(phone.value, code.value)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 380px;
}
.title {
  text-align: center;
  margin-bottom: 20px;
}
.code-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.code-row .el-input {
  flex: 1;
}
.submit {
  width: 100%;
}
.hint {
  color: #999;
  font-size: 12px;
  text-align: center;
  margin-top: 12px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .login-card {
    width: calc(100vw - 40px);
    max-width: 380px;
  }
}
</style>
