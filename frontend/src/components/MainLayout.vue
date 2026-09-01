<template>
  <div class="layout">
    <!-- 左侧窄侧边栏：纯图标菜单 -->
    <aside class="sidebar">
      <!-- 顶部：首页、统计 -->
      <div class="nav-top">
        <el-tooltip content="首页" placement="right" :show-after="300">
          <router-link to="/" class="nav-item" :class="{ active: $route.path === '/' }">
            <el-icon :size="22"><House /></el-icon>
          </router-link>
        </el-tooltip>
        <el-tooltip content="统计" placement="right" :show-after="300">
          <router-link to="/statistics" class="nav-item" :class="{ active: $route.path === '/statistics' }">
            <el-icon :size="22"><DataAnalysis /></el-icon>
          </router-link>
        </el-tooltip>
      </div>

      <!-- 底部：数据刷新、配置、个人信息 -->
      <div class="nav-bottom">
        <el-tooltip content="数据刷新" placement="right" :show-after="300">
          <div class="nav-item refresh-btn" :class="{ spinning: refreshing }" @click="triggerRefresh">
            <el-icon :size="22"><RefreshRight /></el-icon>
          </div>
        </el-tooltip>
        <el-tooltip content="配置" placement="right" :show-after="300">
          <router-link to="/config" class="nav-item" :class="{ active: $route.path === '/config' }">
            <el-icon :size="22"><Setting /></el-icon>
          </router-link>
        </el-tooltip>

        <!-- 个人信息：点击向上弹出选择框 -->
        <el-popover placement="top" :width="150" trigger="click" popper-class="user-popover">
          <div class="user-menu-popup-inner">
            <div class="user-menu-item" @click="onUserCommand('account')">
              <el-icon><User /></el-icon>账户信息
            </div>
            <div class="user-menu-item" @click="onUserCommand('family')">
              <el-icon><Avatar /></el-icon>家庭信息
            </div>
            <div class="user-menu-divider"></div>
            <div class="user-menu-item logout" @click="onUserCommand('logout')">
              <el-icon><SwitchButton /></el-icon>退出登录
            </div>
          </div>
          <template #reference>
            <div class="nav-item user-btn">
              <el-avatar :size="28" class="user-avatar">
                {{ avatarText }}
              </el-avatar>
            </div>
          </template>
        </el-popover>
      </div>
    </aside>

    <!-- 右侧数据展示区 -->
    <main class="content">
      <router-view />
    </main>

    <FamilySetupDialog v-model="familyStore.setupVisible" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, provide, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  House, DataAnalysis, RefreshRight, Setting, User, Avatar, SwitchButton,
} from '@element-plus/icons-vue'
import { useLedgerStore } from '@/stores/ledger'
import { useUserStore } from '@/stores/user'
import { useFamilyStore } from '@/stores/family'
import FamilySetupDialog from '@/components/FamilySetupDialog.vue'

const router = useRouter()
const ledgerStore = useLedgerStore()
const userStore = useUserStore()
const familyStore = useFamilyStore()

const avatarText = computed(() => {
  const name = userStore.user?.nickname || userStore.user?.phone || 'U'
  return name.slice(0, 1).toUpperCase()
})

/* 页面级数据刷新：各页面注册自己的刷新回调，点击侧边栏刷新图标时触发 */
const refreshCallback = ref<(() => Promise<void> | void) | null>(null)
const refreshing = ref(false)

function registerRefresh(fn: () => Promise<void> | void) {
  refreshCallback.value = fn
}

async function triggerRefresh() {
  if (refreshing.value) return
  refreshing.value = true
  try {
    // 先刷新基础数据（账本列表），再触发当前页面的刷新回调
    await ledgerStore.fetch()
    if (refreshCallback.value) {
      await refreshCallback.value()
    }
    ElMessage.success('数据已刷新')
  } catch (e) {
    ElMessage.error('刷新失败')
  } finally {
    refreshing.value = false
  }
}

provide('registerRefresh', registerRefresh)

function onUserCommand(cmd: string) {
  if (cmd === 'account') {
    router.push('/settings')
  } else if (cmd === 'family') {
    if (familyStore.family) {
      router.push('/family')
    } else {
      familyStore.openSetup()
    }
  } else if (cmd === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

onMounted(async () => {
  if (!userStore.user) {
    await userStore.fetchMe()
  }
  await ledgerStore.fetch()
  await familyStore.fetch().catch(() => {})
})
</script>

<style scoped>
.layout {
  display: flex;
  height: 100%;
  width: 100%;
}

/* 左侧窄侧边栏 */
.sidebar {
  width: 64px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
}

.nav-top,
.nav-bottom {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.nav-item {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #606266;
  text-decoration: none;
  transition: all 0.2s;
  cursor: pointer;
}

.nav-item:hover {
  background: #f0f2f5;
  color: #409eff;
}

.nav-item.active {
  background: #ecf5ff;
  color: #409eff;
}

.refresh-btn.spinning .el-icon {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.user-btn {
  padding: 0;
}

.user-avatar {
  background: linear-gradient(135deg, #409eff, #66b1ff);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

/* 右侧内容区 */
.content {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  background: #f5f7fa;
}
</style>

<style>
/* 个人信息弹出菜单 */
.user-popover {
  padding: 6px !important;
}
.user-popover .user-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 6px;
  font-size: 14px;
  color: #303133;
  cursor: pointer;
  transition: background 0.15s;
  white-space: nowrap;
}
.user-popover .user-menu-item:hover {
  background: #f5f7fa;
}
.user-popover .user-menu-item .el-icon {
  font-size: 15px;
  color: #606266;
}
.user-popover .user-menu-item.logout {
  color: #f56c6c;
}
.user-popover .user-menu-item.logout .el-icon {
  color: #f56c6c;
}
.user-popover .user-menu-divider {
  height: 1px;
  background: #ebeef5;
  margin: 4px 0;
}
</style>
