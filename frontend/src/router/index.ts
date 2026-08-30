import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('@/components/MainLayout.vue'),
      children: [
        { path: '', name: 'home', component: () => import('@/views/HomeView.vue') },
        { path: 'refresh', name: 'refresh', component: () => import('@/views/DataRefreshView.vue') },
        { path: 'config', name: 'config', component: () => import('@/views/ConfigView.vue') },
        { path: 'ledgers', name: 'ledgers', component: () => import('@/views/LedgerListView.vue') },
        { path: 'bills/:id', name: 'bill-detail', component: () => import('@/views/BillDetailView.vue') },
        { path: 'accounts', name: 'accounts', component: () => import('@/views/AccountsView.vue') },
        { path: 'budgets', name: 'budgets', component: () => import('@/views/BudgetsView.vue') },
        { path: 'statistics', name: 'statistics', component: () => import('@/views/StatisticsView.vue') },
        {
          path: 'ledgers/:id',
          name: 'ledger-detail',
          component: () => import('@/views/LedgerDetailView.vue'),
        },
        {
          path: 'ledgers/:id/settings',
          name: 'ledger-settings',
          component: () => import('@/views/LedgerSettingsView.vue'),
        },
        { path: 'family', name: 'family', component: () => import('@/views/FamilyView.vue') },
        { path: 'settings', name: 'settings', component: () => import('@/views/SettingsView.vue') },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) {
    return { path: '/login' }
  }
  if (to.path === '/login' && token) {
    return { path: '/' }
  }
  return true
})

export default router
