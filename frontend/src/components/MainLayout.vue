<template>
  <el-container class="layout">
    <el-aside width="200px" class="aside">
      <div class="logo">家庭记账</div>
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/ledgers">账本</el-menu-item>
        <el-menu-item index="/accounts">账户</el-menu-item>
        <el-menu-item index="/budgets">预算</el-menu-item>
        <el-menu-item index="/statistics">统计</el-menu-item>
        <el-menu-item index="/family">家庭</el-menu-item>
        <el-menu-item index="/settings">设置</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="left">
          <el-select
            v-if="ledgerStore.ledgers.length"
            v-model="currentId"
            style="width: 200px"
            @change="onSwitch"
          >
            <el-option
              v-for="l in ledgerStore.ledgers"
              :key="l.id"
              :label="`${l.name}（${l.type === 'public' ? '公共' : '个人'}）`"
              :value="l.id"
            />
          </el-select>
          <span v-else class="no-ledger" @click="$router.push('/ledgers')">还没有账本，去创建</span>
        </div>
        <div class="right">
          <el-button type="primary" round @click="goRecord">＋ 记账</el-button>
          <el-dropdown @command="onUserCommand">
            <span class="user">
              {{ userStore.user?.nickname || userStore.user?.phone }}
              <el-icon><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { useLedgerStore } from '@/stores/ledger'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const ledgerStore = useLedgerStore()
const userStore = useUserStore()

const currentId = computed({
  get: () => ledgerStore.currentLedgerId,
  set: (v: number) => ledgerStore.switchTo(v),
})

function onSwitch(v: number) {
  ledgerStore.switchTo(v)
}

function goRecord() {
  const id = ledgerStore.currentLedgerId
  if (id) {
    router.push(`/ledgers/${id}`)
  } else {
    router.push('/ledgers')
  }
}

function onUserCommand(cmd: string) {
  if (cmd === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

onMounted(async () => {
  if (!userStore.user) {
    await userStore.fetchMe()
  }
  await ledgerStore.fetch()
})
</script>

<style scoped>
.layout {
  height: 100%;
}
.aside {
  background: #fff;
  border-right: 1px solid #ebeef5;
}
.logo {
  font-size: 18px;
  font-weight: 600;
  padding: 18px 20px;
  color: #409eff;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}
.left,
.right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}
.no-ledger {
  color: #409eff;
  cursor: pointer;
  font-size: 14px;
}
.main {
  background: #f5f7fa;
}
</style>
