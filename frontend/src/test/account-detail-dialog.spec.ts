import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import ElementPlus from 'element-plus'
import AccountDetailDialog from '@/components/AccountDetailDialog.vue'
import { updateAccount } from '@/api/account'

vi.mock('@/api/account', () => ({
  updateAccount: vi.fn(),
  ACCOUNT_TYPE_LABELS: { asset: '资金账户', credit: '信贷账户', stored_value: '储值账户' },
  ACCOUNT_TYPE_ORDER: ['asset', 'credit', 'stored_value'],
}))

// el-dialog stub：避免 teleport 到 body，方便断言
const dialogStub = {
  props: ['modelValue'],
  emits: ['update:modelValue', 'closed'],
  template: '<div v-if="modelValue" class="dialog-stub"><slot /><slot name="footer" /></div>',
}

const account = {
  id: 1,
  ledgerId: 1,
  type: 'asset' as const,
  name: '测试卡A',
  icon: 'cash',
  initialBalance: 500,
  balance: 411.5,
  groupName: '日常',
  remark: '备注',
  includeInTotal: 1,
}

function mountDialog() {
  return mount(AccountDetailDialog, {
    props: { modelValue: true, account },
    global: { plugins: [ElementPlus], stubs: { 'el-dialog': dialogStub } },
  })
}

describe('AccountDetailDialog 账户详情/编辑', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    document.body.innerHTML = ''
  })

  it('查看模式：第一行余额、第二行账户分组（类型）', async () => {
    const w = mountDialog()
    await nextTick()
    const labels = w.findAll('.el-descriptions__label').map((n) => n.text())
    const contents = w.findAll('.el-descriptions__content').map((n) => n.text())
    // 第一行余额
    expect(labels[0]).toBe('余额')
    expect(contents[0]).toContain('411.50')
    // 第二行账户分组展示类型（资金账户）
    expect(labels[1]).toBe('账户分组')
    expect(contents[1]).toBe('资金账户')
  })

  it('查看模式展示账户其他信息（初始余额/备注/计入总资产）', async () => {
    const w = mountDialog()
    await nextTick()
    const labels = w.findAll('.el-descriptions__label').map((n) => n.text())
    const contents = w.findAll('.el-descriptions__content').map((n) => n.text())
    expect(labels).toContain('初始余额')
    expect(labels).toContain('备注')
    expect(labels).toContain('计入总资产')
    expect(contents[2]).toContain('500.00')
  })

  it('编辑账户时账户分组为下拉框，选项为资金/信贷/储值', async () => {
    const w = mountDialog()
    await nextTick()
    const editBtn = w.findAll('button').find((b) => b.text().includes('编辑账户'))
    expect(editBtn).toBeTruthy()
    await editBtn!.trigger('click')
    await nextTick()

    const select = w.findComponent({ name: 'ElSelect' })
    expect(select.exists()).toBe(true)
    // 展开下拉框（选项 teleport 到 body），检查三个类型选项
    await w.find('.el-select__wrapper').trigger('click')
    await flushPromises()
    const opts = Array.from(document.querySelectorAll('.el-select-dropdown__item')).map((n) => n.textContent)
    expect(opts).toContain('资金账户')
    expect(opts).toContain('信贷账户')
    expect(opts).toContain('储值账户')
  })

  it('保存时携带账户类型提交', async () => {
    const w = mountDialog()
    await nextTick()
    await w.findAll('button').find((b) => b.text().includes('编辑账户'))!.trigger('click')
    await nextTick()
    await w.findAll('button').find((b) => b.text().includes('保存'))!.trigger('click')
    await flushPromises()
    expect(updateAccount).toHaveBeenCalledWith(1, expect.objectContaining({ type: 'asset' }))
  })
})
