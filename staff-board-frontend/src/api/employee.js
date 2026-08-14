import request from './request'

export function getEmployees(params) {
  return request({ url: '/employees', method: 'get', params })
}

export function getEmployee(id) {
  return request({ url: `/employees/${id}`, method: 'get' })
}

export function createEmployee(data) {
  return request({ url: '/employees', method: 'post', data })
}

export function updateEmployee(id, data) {
  return request({ url: `/employees/${id}`, method: 'put', data })
}

export function deleteEmployee(id) {
  return request({ url: `/employees/${id}`, method: 'delete' })
}

export function importEmployees(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({ url: '/employees/import', method: 'post', data: formData,
    headers: { 'Content-Type': 'multipart/form-data' } })
}

export function downloadTemplate() {
  return request({ url: '/employees/template', method: 'get', responseType: 'blob' })
}

export function batchSetWhitelist(data) {
  return request({ url: '/employees/whitelist', method: 'put', data })
}
