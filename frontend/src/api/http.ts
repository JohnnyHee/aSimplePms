/**
 * axios 封装：统一响应信封解包 + 业务错误提示 + 401 跳登录 + refreshToken 自动续期。
 *
 * 约定：
 *  - HTTP 2xx 且 `code === 0` → resolve 出信封里的 `data` 本身；
 *  - 其它情况 → reject 一个 {@link ApiError}，并用 ElMessage 弹出 message；
 *  - HTTP 401（或业务码 2000/2004/2005）→ 清除登录态并跳 `/login?redirect=<当前路径>`（登录/刷新接口除外，避免死循环）；
 *  - accessToken 过期时用 refreshToken 调 `/auth/refresh` 重试原请求一次，并发请求共享同一次刷新。
 */
import axios, {
  AxiosError,
  AxiosHeaders,
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig
} from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiEnvelope, LoginResult } from './types'

/** localStorage key */
export const ACCESS_TOKEN_KEY = 'pms_access_token'
export const REFRESH_TOKEN_KEY = 'pms_refresh_token'

/** 无需鉴权、且不参与「401 跳登录」的接口 */
const LOGIN_PATH = '/auth/login'
const REFRESH_PATH = '/auth/refresh'

/** 业务基础地址 */
const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

/** 触发「登录态失效」跳转的业务码 */
const UNAUTHORIZED_CODES = [2000, 2004, 2005]

/** 请求参数：query string */
export type QueryParams = Record<string, unknown> | undefined

/** 可以在请求上附加的自定义选项 */
export interface PmsRequestConfig extends AxiosRequestConfig {
  /** 为 true 时不弹出 ElMessage 错误提示（调用方自行处理） */
  silent?: boolean
}

/** 内部可重试的请求配置 */
interface RetriableConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
}

/** 业务错误：响应信封 code != 0，或 HTTP 层错误 */
export class ApiError extends Error {
  /** 业务码（信封里的 code），HTTP 层错误时为 HTTP 状态码 */
  readonly code: number
  /** HTTP 状态码 */
  readonly status?: number
  /** 信封里的 data（失败时通常为 null） */
  readonly data?: unknown

  constructor(message: string, code: number, status?: number, data?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
    this.data = data
    Object.setPrototypeOf(this, ApiError.prototype)
  }
}

/* ------------------------------------------------------------------ */
/* token 存储（localStorage 持久化）                                   */
/* ------------------------------------------------------------------ */

export const tokenStorage = {
  getAccessToken(): string | null {
    try {
      return window.localStorage.getItem(ACCESS_TOKEN_KEY)
    } catch {
      return null
    }
  },
  getRefreshToken(): string | null {
    try {
      return window.localStorage.getItem(REFRESH_TOKEN_KEY)
    } catch {
      return null
    }
  },
  set(accessToken: string | null, refreshToken?: string | null): void {
    try {
      if (accessToken) {
        window.localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
      } else {
        window.localStorage.removeItem(ACCESS_TOKEN_KEY)
      }
      if (refreshToken !== undefined) {
        if (refreshToken) {
          window.localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
        } else {
          window.localStorage.removeItem(REFRESH_TOKEN_KEY)
        }
      }
    } catch {
      /* localStorage 不可用时静默降级为内存态 */
    }
  },
  clear(): void {
    try {
      window.localStorage.removeItem(ACCESS_TOKEN_KEY)
      window.localStorage.removeItem(REFRESH_TOKEN_KEY)
    } catch {
      /* ignore */
    }
  }
}

/* ------------------------------------------------------------------ */
/* 工具函数                                                            */
/* ------------------------------------------------------------------ */

function isEnvelope(value: unknown): value is ApiEnvelope<unknown> {
  return typeof value === 'object' && value !== null && 'code' in value && 'message' in value
}

function notify(message: string, config?: PmsRequestConfig): void {
  if (config?.silent) return
  ElMessage.error(message)
}

function fallbackMessage(status?: number, raw?: string): string {
  switch (status) {
    case 400:
      return '请求参数有误'
    case 401:
      return '登录状态已失效，请重新登录'
    case 403:
      return '没有访问该资源的权限'
    case 404:
      return '请求的资源不存在'
    case 409:
      return '资源已存在或状态冲突'
    case 422:
      return '业务处理失败'
    case 500:
      return '服务器内部错误'
    default:
      return raw ? `网络请求失败：${raw}` : '网络请求失败，请检查后端服务是否已启动'
  }
}

/* ------------------------------------------------------------------ */
/* 实例与拦截器                                                        */
/* ------------------------------------------------------------------ */

const http: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 20000,
  headers: { 'Content-Type': 'application/json' }
})

/** 请求拦截：附加 Authorization: Bearer <accessToken> */
http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const url = config.url ?? ''
  const skipAuth = url.includes(LOGIN_PATH) || url.includes(REFRESH_PATH)
  const token = tokenStorage.getAccessToken()
  if (token && !skipAuth) {
    if (config.headers && typeof (config.headers as AxiosHeaders).set === 'function') {
      ;(config.headers as AxiosHeaders).set('Authorization', `Bearer ${token}`)
    } else {
      config.headers = new AxiosHeaders({ Authorization: `Bearer ${token}` })
    }
  }
  return config
})

/** 响应拦截：解包信封 / 统一错误 / 401 处理 / 令牌续期 */
http.interceptors.response.use(
  (response) => {
    // 二进制流直接透传
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response.data
    }
    const raw: unknown = response.data
    if (!isEnvelope(raw)) {
      return raw
    }
    if (raw.code === 0) {
      return raw.data
    }
    const error = new ApiError(raw.message || '请求失败', raw.code, response.status, raw.data)
    notify(error.message, response.config as PmsRequestConfig)
    return Promise.reject(error)
  },
  async (error: unknown) => {
    if (!axios.isAxiosError(error)) {
      const unknownError = new ApiError(
        error instanceof Error ? error.message : '请求失败，请稍后重试',
        -1
      )
      notify(unknownError.message)
      return Promise.reject(unknownError)
    }

    const axiosError = error as AxiosError<ApiEnvelope<unknown> | undefined>
    const status = axiosError.response?.status
    const envelope = axiosError.response?.data
    const apiCode = typeof envelope?.code === 'number' ? envelope.code : undefined
    const config = axiosError.config as RetriableConfig | undefined
    const url = config?.url ?? ''
    const isAuthEndpoint = url.includes(LOGIN_PATH) || url.includes(REFRESH_PATH)
    const message = envelope?.message || fallbackMessage(status, axiosError.message)

    // accessToken 过期：用 refreshToken 换新令牌后重试原请求一次（并发去重）
    if (status === 401 && config && !config._retry && !isAuthEndpoint && tokenStorage.getRefreshToken()) {
      config._retry = true
      try {
        await getRefreshPromise()
        // 重试时会重新经过请求拦截器，自动带上刚写入的新 accessToken
        return await http.request(config)
      } catch (refreshError) {
        const refreshMessage =
          refreshError instanceof ApiError ? refreshError.message : '登录状态已失效，请重新登录'
        await handleUnauthorized()
        notify(refreshMessage, config as PmsRequestConfig)
        return Promise.reject(
          refreshError instanceof ApiError
            ? refreshError
            : new ApiError(refreshMessage, 2000, 401)
        )
      }
    }

    // 未认证 / 令牌失效 → 清登录态并跳登录页
    if (status === 401 || (apiCode !== undefined && UNAUTHORIZED_CODES.includes(apiCode))) {
      if (!isAuthEndpoint) {
        await handleUnauthorized()
      }
      const authError = new ApiError(message, apiCode ?? 2000, status ?? 401, envelope?.data ?? null)
      notify(authError.message, config as PmsRequestConfig)
      return Promise.reject(authError)
    }

    const apiError = new ApiError(message, apiCode ?? status ?? -1, status, envelope?.data ?? null)
    notify(apiError.message, config as PmsRequestConfig)
    return Promise.reject(apiError)
  }
)

/* ------------------------------------------------------------------ */
/* 令牌刷新（并发去重）                                                */
/* ------------------------------------------------------------------ */

let refreshPromise: Promise<string> | null = null

function getRefreshPromise(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = doRefreshToken().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

/**
 * 用 refreshToken 换取新的 accessToken；使用裸 axios，避免再次进入拦截器造成递归。
 * 成功后会把新令牌写入 localStorage。
 */
async function doRefreshToken(): Promise<string> {
  const refreshToken = tokenStorage.getRefreshToken()
  if (!refreshToken) {
    throw new ApiError('登录状态已失效，请重新登录', 2000, 401)
  }
  const response = await axios.post<ApiEnvelope<LoginResult>>(
    `${BASE_URL}${REFRESH_PATH}`,
    { refreshToken },
    { timeout: 15000, headers: { 'Content-Type': 'application/json' } }
  )
  const envelope = response.data
  if (!envelope || envelope.code !== 0 || !envelope.data?.accessToken) {
    throw new ApiError(envelope?.message || '登录状态已失效，请重新登录', envelope?.code ?? 2000, response.status)
  }
  tokenStorage.set(envelope.data.accessToken, envelope.data.refreshToken)
  return envelope.data.accessToken
}

/** 清除登录态并跳转到 /login?redirect=<当前路径> */
let redirecting = false

async function handleUnauthorized(): Promise<void> {
  tokenStorage.clear()
  try {
    // 动态引入，避免 http.ts 与 store/router 形成初始化期循环依赖
    const { useAuthStore } = await import('@/stores/auth')
    useAuthStore().clearSession()
  } catch {
    /* store 尚未初始化时忽略 */
  }

  const { pathname, search, hash } = window.location
  if (pathname === '/login' || redirecting) return
  redirecting = true
  const redirect = `${pathname}${search}${hash}`
  try {
    const { default: router } = await import('@/router')
    await router.replace({ path: '/login', query: { redirect } })
  } catch {
    window.location.href = `/login?redirect=${encodeURIComponent(redirect)}`
  } finally {
    window.setTimeout(() => {
      redirecting = false
    }, 500)
  }
}

/* ------------------------------------------------------------------ */
/* 类型化的请求门面                                                    */
/* ------------------------------------------------------------------ */

/**
 * 因为响应拦截器已经把信封解包成 `data`，这里用泛型门面把 axios 的返回类型纠正为 `T`。
 */
export interface HttpClient {
  get<T>(url: string, params?: QueryParams, config?: PmsRequestConfig): Promise<T>
  post<T>(url: string, data?: unknown, config?: PmsRequestConfig): Promise<T>
  put<T>(url: string, data?: unknown, config?: PmsRequestConfig): Promise<T>
  patch<T>(url: string, data?: unknown, config?: PmsRequestConfig): Promise<T>
  delete<T>(url: string, params?: QueryParams, config?: PmsRequestConfig): Promise<T>
}

export const request: HttpClient = {
  get<T>(url: string, params?: QueryParams, config?: PmsRequestConfig): Promise<T> {
    return http.get(url, { params, ...config }) as unknown as Promise<T>
  },
  post<T>(url: string, data?: unknown, config?: PmsRequestConfig): Promise<T> {
    return http.post(url, data, config) as unknown as Promise<T>
  },
  put<T>(url: string, data?: unknown, config?: PmsRequestConfig): Promise<T> {
    return http.put(url, data, config) as unknown as Promise<T>
  },
  patch<T>(url: string, data?: unknown, config?: PmsRequestConfig): Promise<T> {
    return http.patch(url, data, config) as unknown as Promise<T>
  },
  delete<T>(url: string, params?: QueryParams, config?: PmsRequestConfig): Promise<T> {
    return http.delete(url, { params, ...config }) as unknown as Promise<T>
  }
}

/** 去掉 undefined / null / 空字符串的查询参数，避免发出 `?status=` 这类空值 */
export function compactParams(params?: Record<string, unknown>): Record<string, unknown> {
  const result: Record<string, unknown> = {}
  if (!params) return result
  Object.keys(params).forEach((key) => {
    const value = params[key]
    if (value === undefined || value === null || value === '') return
    result[key] = value
  })
  return result
}

/** 路径参数安全编码 */
export function pathParam(value: string | number): string {
  return encodeURIComponent(String(value))
}

export { http }
export default request
