/// <reference types="vite/client" />

/**
 * 全局类型声明：.vue 单文件组件 + Vite 环境变量
 */

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>
  export default component
}

declare module '*.svg' {
  const content: string
  export default content
}

interface ImportMetaEnv {
  /** 应用标题 */
  readonly VITE_APP_TITLE: string
  /** 接口基础地址（默认 /api） */
  readonly VITE_API_BASE_URL: string
  /** dev server 端口 */
  readonly VITE_PORT?: string
  /** dev server 代理目标 */
  readonly VITE_PROXY_TARGET?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
