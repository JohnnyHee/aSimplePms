/**
 * Pinia 实例与 store 汇总入口。
 *
 * 用法：`import { useAuthStore, useAppStore } from '@/stores'`
 */
import { createPinia } from 'pinia'

/** 全局唯一的 pinia 实例（main.ts 中 app.use(pinia)） */
export const pinia = createPinia()

export default pinia

export * from './auth'
export * from './app'
