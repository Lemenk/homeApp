import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import BillFormDialog from '@/components/BillFormDialog.vue'

vi.mock('@/stores/ledger', () => ({
  useLedgerStore: () => ({ ledgers: [{ id: 1, type: 'personal', name: '测试账本' }] }),
}))
vi.mock('@/stores/family', () => ({
  useFamilyStore: () => ({ fetchMembers: vi.fn().mockResolvedValue([]) }),
}))
vi.mock('@/stores/user', () => ({
  useUserStore: () => ({ user: { id: 1 } }),
}))
vi.mock('@/api/account', () => ({ listAccounts: vi.fn().mockResolvedValue([]) }))
vi.mock('@/api/ledger', () => ({
  listCategories: vi.fn().mockResolvedValue([
    { id: 1, name: '餐饮', type: 'expense', icon: 'food', enabled: 1 },
    { id: 2, name: '交通', type: 'expense', icon: 'traffic', enabled: 1 },
  ]),
  listTags: vi.fn().mockResolvedValue([]),
}))
vi.mock('@/api/bill', () => ({ createBill: vi.fn(), updateBill: vi.fn() }))

// 用原生 input 模拟 el-input-number 的 v-model 行为（真实组件输入时实时 emit update:modelValue）
const inputNumberStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template:
    '<input data-test="amount" type="number" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))" />',
}

const dialogStub = { template: '<div class="dlg"><slot /></div>' }

function mountDialog() {
  // 模拟真实交互：先关闭，再打开（modelValue false→true 触发加载）
  const wrapper = mount(BillFormDialog, {
    props: { modelValue: false, ledgerId: 1 },
    global: {
      plugins: [ElementPlus],
      stubs: {
        'el-dialog': dialogStub,
        'el-input-number': inputNumberStub,
      },
    },
  })
  wrapper.setProps({ modelValue: true })
  return wrapper
}

function nextButton(wrapper: any) {
  return wrapper.findAll('button').find((b: any) => b.text().includes('下一步'))
}

describe('BillFormDialog 记账金额步骤', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('输入金额后未选分类：显示引导提示且点“下一步”不跳转', async () => {
    const wrapper = mountDialog()
    await flushPromises()
    // Step 0 → 下一步
    await nextButton(wrapper)!.trigger('click')
    await flushPromises()

    // 输入金额
    await wrapper.find('input[data-test="amount"]').setValue(100)
    await flushPromises()

    // 金额已填但未选分类 → 出现引导提示
    expect(wrapper.text()).toContain('请选择分类后继续')

    // 点击“下一步”：仍停留在金额步骤（未选分类）
    await nextButton(wrapper)!.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('餐饮') // 仍在分类选择界面
    expect(wrapper.text()).not.toContain('转出账户')
  })

  it('选择分类后提示消失，可进入账户步骤', async () => {
    const wrapper = mountDialog()
    await flushPromises()
    await nextButton(wrapper)!.trigger('click')
    await flushPromises()

    await wrapper.find('input[data-test="amount"]').setValue(100)
    await flushPromises()

    // 点击“餐饮”分类
    const food = wrapper.findAll('.cat-name').find((d: any) => d.text() === '餐饮')
    await food!.trigger('click')
    await flushPromises()

    // 引导提示消失，且下一步可进入账户步骤
    expect(wrapper.text()).not.toContain('请选择分类后继续')
    await nextButton(wrapper)!.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('添加账户')
  })

  it('转账类型无需分类即可进入账户步骤', async () => {
    const wrapper = mountDialog()
    await flushPromises()
    // Step 0 选择转账
    const transfer = wrapper.findAll('input').find((i: any) => (i.element as HTMLInputElement).value === 'transfer')
    await transfer!.setValue('transfer')
    await nextButton(wrapper)!.trigger('click')
    await flushPromises()

    await wrapper.find('input[data-test="amount"]').setValue(200)
    await flushPromises()
    // 转账无分类要求，可直接下一步到账户步骤（显示转出/转入账户）
    await nextButton(wrapper)!.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('转出账户')
  })
})
