import axios from 'axios'

export const TOKEN_KEY = 'gc_token'
export const USUARIO_KEY = 'gc_usuario'
export const AUTH_EXPIRADO = 'auth:expirado'

const api = axios.create({
  baseURL: '/api',
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (resposta) => resposta,
  (erro: { response?: { status?: number } }) => {
    if (erro.response?.status === 401 && window.location.pathname !== '/login') {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USUARIO_KEY)
      window.dispatchEvent(new Event(AUTH_EXPIRADO))
    }
    return Promise.reject(erro)
  },
)

export default api
