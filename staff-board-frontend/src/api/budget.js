import request from './request'

export function getBudgetSummary() {
  return request({ url: '/budget/summary', method: 'get' })
}

export function getDepartmentBudget(dept) {
  return request({ url: `/budget/department/${dept}`, method: 'get' })
}

export function getBudgetTrend() {
  return request({ url: '/budget/trend', method: 'get' })
}
