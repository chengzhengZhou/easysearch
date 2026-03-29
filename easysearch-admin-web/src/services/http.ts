import axios from 'axios'

// Default: use same-origin `/api` (dev will proxy via Vite).
export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE ?? '',
  timeout: 15000,
})

function getDevUser(): string {
  return (localStorage.getItem('DEV_USER') ?? import.meta.env.VITE_DEV_USER ?? 'dev').toString()
}

function getDevRoles(): string {
  return (localStorage.getItem('DEV_ROLES') ?? import.meta.env.VITE_DEV_ROLES ?? 'admin').toString()
}

http.interceptors.request.use((config) => {
  config.headers = config.headers ?? {}
  config.headers['X-User'] = getDevUser()
  config.headers['X-Roles'] = getDevRoles()
  return config
})

