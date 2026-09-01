<template>
  <div class="family">
    <el-card v-if="familyStore.family">
      <div class="header">
        <div>
          <h2>{{ familyStore.family.name }}</h2>
          <span class="tag">{{ familyStore.family.role === 'creator' ? '创建者' : '成员' }}</span>
        </div>
        <el-button v-if="isCreator" type="primary" plain @click="onRefreshInvite">
          刷新邀请码
        </el-button>
      </div>
      <div v-if="isCreator" class="invite">
        <span>邀请码：</span>
        <b>{{ familyStore.family.inviteCode }}</b>
        <el-button link type="primary" @click="onCopy">复制</el-button>
      </div>
      <el-divider />
      <h3>家庭成员（{{ familyStore.family.members.length }}）</h3>
      <el-table :data="familyStore.family.members" style="width: 100%">
        <el-table-column label="昵称" prop="nickname" />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="row.role === 'creator' ? 'primary' : 'info'">
              {{ row.role === 'creator' ? '创建者' : '成员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="加入时间" width="180">
          <template #default="{ row }">{{ formatTime(row.joinedAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-empty v-else description="暂无家庭" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useFamilyStore } from '@/stores/family'
import { useUserStore } from '@/stores/user'

const familyStore = useFamilyStore()
const userStore = useUserStore()

const isCreator = computed(
  () => familyStore.family?.creatorId === userStore.user?.id
)

function formatTime(t: string) {
  return t ? t.replace('T', ' ').substring(0, 16) : ''
}

async function onRefreshInvite() {
  await familyStore.refresh()
  ElMessage.success('邀请码已刷新')
}

async function onCopy() {
  if (familyStore.family) {
    await navigator.clipboard.writeText(familyStore.family.inviteCode)
    ElMessage.success('已复制')
  }
}

onMounted(() => {
  if (!familyStore.family) {
    familyStore.fetch()
  }
})
</script>

<style scoped>
.family {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.tag {
  color: #909399;
  font-size: 13px;
}
.invite {
  margin-top: 12px;
  font-size: 15px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .family {
    padding: 14px 12px;
  }
}
</style>
