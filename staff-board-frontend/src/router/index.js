import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/views/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '看板总览' }
      },
      {
        path: 'employees',
        name: 'EmployeeList',
        component: () => import('@/views/employee/list.vue'),
        meta: { title: '员工列表' }
      },
      {
        path: 'employees/card',
        name: 'EmployeeCard',
        component: () => import('@/views/employee/card.vue'),
        meta: { title: '卡片视图' }
      },
      {
        path: 'employees/:id',
        name: 'EmployeeDetail',
        component: () => import('@/views/employee/detail.vue'),
        meta: { title: '员工详情' }
      },
      {
        path: 'import',
        name: 'Import',
        component: () => import('@/views/import/index.vue'),
        meta: { title: '批量导入' }
      },
      {
        path: 'budget',
        name: 'Budget',
        component: () => import('@/views/budget/index.vue'),
        meta: { title: '成本预算' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.public) {
    next()
  } else if (!getToken()) {
    next('/login')
  } else {
    next()
  }
})

export default router
