<template>
  <div class="settings-page">
    <el-card shadow="never" class="block">
      <template #header>账户信息</template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="昵称">{{ user?.nickname || '—' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ user?.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="用户 ID">{{ user?.id }}</el-descriptions-item>
        <el-descriptions-item label="登录方式">手机号验证码登录</el-descriptions-item>
      </el-descriptions>
      <div class="note">v1 暂不支持修改密码 / 解绑手机 / 微信登录（微信登录需备案域名与企业主体，已在后端预留接口）</div>
    </el-card>

    <el-card shadow="never" class="block">
      <template #header>家庭与成员</template>
      <template v-if="familyStore.family">
        <div class="family-line">
          家庭：<b>{{ familyStore.family.name }}</b>（邀请码 {{ familyStore.family.inviteCode }}）
        </div>
        <div class="member-list">
          <div v-for="m in familyStore.family.members" :key="m.userId" class="member-line">
            <el-avatar :size="28" :src="m.avatar">{{ m.nickname?.[0] }}</el-avatar>
            <span class="member-name">{{ m.nickname }}</span>
            <el-tag size="small" :type="m.role === 'creator' ? 'primary' : 'info'">
              {{ m.role === 'creator' ? '创建者' : '成员' }}
            </el-tag>
          </div>
        </div>
        <el-button text type="primary" @click="$router.push('/family')">管理家庭 →</el-button>
      </template>
      <el-empty v-else :image-size="60" description="尚未加入家庭">
        <el-button type="primary" @click="$router.push('/family')">去创建/加入家庭</el-button>
      </el-empty>
    </el-card>

    <el-card shadow="never" class="block">
      <template #header>关于</template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="应用">家庭记账 App</el-descriptions-item>
        <el-descriptions-item label="版本">v1.0（网页端）</el-descriptions-item>
        <el-descriptions-item label="技术栈">Vue 3 + Element Plus + Spring Boot 3</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <div class="logout">
      <el-button type="danger" plain @click="onLogout">退出登录</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { UserVO } from '@/types'
import { useUserStore } from '@/stores/user'
import { useFamilyStore } from '@/stores/family'

const router = useRouter()
const userStore = useUserStore()
const familyStore = useFamilyStore()
const user = ref<UserVO | null>(null)

function onLogout() {
  userStore.logout()
  router.push('/login')
}

onMounted(async () => {
  if (!userStore.user) {
    await userStore.fetchMe()
  }
  user.value = userStore.user
  await familyStore.fetch()
})
</script>

<style scoped>
.settings-page {
  padding: 20px;
  max-width: 720px;
  margin: 0 auto;
}
.block {
  margin-bottom: 16px;
  border-radius: 8px;
}
.note {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}
.family-line {
  margin-bottom: 12px;
}
.member-line {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
}
.member-name {
  flex: 1;
}
.logout {
  text-align: center;
  margin-top: 8px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .settings-page {
    padding: 14px 12px;
  }
}
</style>
