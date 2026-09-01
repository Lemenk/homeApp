<template>
  <div class="settings">
    <el-card>
      <el-tabs v-model="tab">
        <el-tab-pane label="分类管理" name="category">
          <el-radio-group v-model="catType" class="mb">
            <el-radio-button value="expense">支出分类</el-radio-button>
            <el-radio-button value="income">收入分类</el-radio-button>
          </el-radio-group>
          <div class="cat-add">
            <el-input v-model="newCatName" placeholder="新分类名称" style="width: 240px" />
            <el-button type="primary" @click="onAddCategory">添加</el-button>
          </div>
          <el-table :data="filteredCategories" style="width: 100%">
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="icon" label="图标" width="120" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">
                  {{ row.enabled === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="onToggleCategory(row)">
                  {{ row.enabled === 1 ? '停用' : '启用' }}
                </el-button>
                <el-button link type="danger" size="small" @click="onDeleteCategory(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="标签管理" name="tag">
          <div class="cat-add">
            <el-input v-model="newTagName" placeholder="新标签名称" style="width: 240px" />
            <el-button type="primary" @click="onAddTag">添加</el-button>
          </div>
          <el-tag
            v-for="t in tags"
            :key="t.id"
            closable
            class="tag"
            @close="onDeleteTag(t)"
          >
            {{ t.name }}
          </el-tag>
          <el-empty v-if="!tags.length" description="暂无标签" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  createCategory,
  createTag,
  deleteCategory,
  deleteTag,
  listCategories,
  listTags,
  toggleCategory,
  type CategoryVO,
  type TagVO,
} from '@/api/ledger'

const route = useRoute()
const ledgerId = Number(route.params.id)

const tab = ref('category')
const catType = ref<'expense' | 'income'>('expense')
const categories = ref<CategoryVO[]>([])
const tags = ref<TagVO[]>([])
const newCatName = ref('')
const newTagName = ref('')

const filteredCategories = computed(() => categories.value.filter((c) => c.type === catType.value))

onMounted(async () => {
  categories.value = await listCategories(ledgerId)
  tags.value = await listTags(ledgerId)
})

async function onAddCategory() {
  if (!newCatName.value.trim()) return
  await createCategory(ledgerId, { name: newCatName.value.trim(), type: catType.value })
  newCatName.value = ''
  categories.value = await listCategories(ledgerId)
  ElMessage.success('分类已添加')
}

async function onToggleCategory(row: CategoryVO) {
  await toggleCategory(row.id, row.enabled !== 1)
  categories.value = await listCategories(ledgerId)
}

async function onDeleteCategory(row: CategoryVO) {
  await deleteCategory(row.id)
  categories.value = await listCategories(ledgerId)
  ElMessage.success('已删除')
}

async function onAddTag() {
  if (!newTagName.value.trim()) return
  await createTag(ledgerId, { name: newTagName.value.trim() })
  newTagName.value = ''
  tags.value = await listTags(ledgerId)
  ElMessage.success('标签已添加')
}

async function onDeleteTag(row: TagVO) {
  await deleteTag(row.id)
  tags.value = await listTags(ledgerId)
  ElMessage.success('已删除')
}
</script>

<style scoped>
.settings {
  padding: 20px;
  max-width: 900px;
  margin: 0 auto;
}
.mb {
  margin-bottom: 16px;
}
.cat-add {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.tag {
  margin: 4px 8px 4px 0;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .settings {
    padding: 14px 12px;
  }
}
</style>
