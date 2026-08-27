<template>
  <div class="ledger-list">
    <div class="toolbar">
      <h2>我的账本</h2>
      <el-button type="primary" @click="showNew = true">新建账本</el-button>
    </div>

    <el-empty v-if="!ledgerStore.ledgers.length" description="还没有账本，点击右上角创建" />

    <div class="grid">
      <el-card
        v-for="l in ledgerStore.ledgers"
        :key="l.id"
        class="ledger-card"
        shadow="hover"
        @click="openLedger(l)"
      >
        <div class="card-head">
          <el-tag :type="l.type === 'public' ? 'primary' : 'info'" size="small">
            {{ l.type === 'public' ? '公共' : '个人' }}
          </el-tag>
          <span class="count">{{ l.memberCount }} 人</span>
        </div>
        <div class="name">{{ l.name }}</div>
        <div class="actions" @click.stop>
          <el-button
            v-if="l.ownerId === userStore.user?.id"
            link
            type="danger"
            size="small"
            @click="onDelete(l)"
          >
            删除
          </el-button>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="showNew" title="新建账本" width="420px">
      <el-form label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" maxlength="64" placeholder="如：家庭账本" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type" @change="onTypeChange">
            <el-radio value="public">公共账本（家人共享）</el-radio>
            <el-radio value="personal">个人账本（仅自己）</el-radio>
          </el-radio-group>
          <div v-if="form.type === 'public' && !familyStore.hasFamily" class="family-tip">
            创建公共账本需要先有家庭，<el-link type="primary" @click="goFamily">去创建/加入家庭</el-link>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showNew = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="onCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useLedgerStore } from '@/stores/ledger'
import { useUserStore } from '@/stores/user'
import { useFamilyStore } from '@/stores/family'
import type { LedgerVO } from '@/types'

const router = useRouter()
const ledgerStore = useLedgerStore()
const userStore = useUserStore()
const familyStore = useFamilyStore()

const showNew = ref(false)
const creating = ref(false)
const form = reactive({ name: '', type: 'personal' as 'public' | 'personal' })

onMounted(async () => {
  if (!ledgerStore.ledgers.length) {
    ledgerStore.fetch()
  }
  if (!familyStore.family) {
    await familyStore.fetch()
  }
  // 有家庭时默认公共账本，否则默认个人账本
  form.type = familyStore.hasFamily ? 'public' : 'personal'
})

function onTypeChange(t: 'public' | 'personal') {
  if (t === 'public' && !familyStore.hasFamily) {
    form.type = t
  }
}

function goFamily() {
  showNew.value = false
  familyStore.openSetup()
}

function openLedger(l: LedgerVO) {
  ledgerStore.switchTo(l.id)
  router.push(`/ledgers/${l.id}`)
}

async function onCreate() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入账本名称')
    return
  }
  if (form.type === 'public' && !familyStore.hasFamily) {
    ElMessage.warning('创建公共账本需要先创建或加入家庭')
    return
  }
  creating.value = true
  try {
    const ledger = await ledgerStore.create({ name: form.name.trim(), type: form.type })
    ElMessage.success('创建成功')
    showNew.value = false
    router.push(`/ledgers/${ledger.id}`)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

async function onDelete(l: LedgerVO) {
  await ElMessageBox.confirm(`确定删除账本「${l.name}」吗？删除后其中账单将不可用。`, '删除确认', {
    type: 'warning',
  })
  await ledgerStore.remove(l.id)
  ElMessage.success('已删除')
}
</script>

<style scoped>
.ledger-list {
  padding: 20px;
  max-width: 960px;
  margin: 0 auto;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}
.ledger-card {
  cursor: pointer;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.name {
  font-size: 18px;
  font-weight: 600;
  margin: 12px 0;
}
.count {
  color: #909399;
  font-size: 13px;
}
.family-tip {
  font-size: 12px;
  color: #e6a23c;
  margin-top: 4px;
}
</style>
