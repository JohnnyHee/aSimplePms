/**
 * 应用级 UI 状态 store：侧边栏折叠、主题、设备类型。
 */
import { ref, watch } from 'vue'
import { defineStore } from 'pinia'

export type ThemeMode = 'light' | 'dark'
export type DeviceType = 'desktop' | 'mobile'

const SIDEBAR_KEY = 'pms_sidebar_collapsed'
const THEME_KEY = 'pms_theme'

function readString(key: string, fallback: string): string {
  try {
    return window.localStorage.getItem(key) ?? fallback
  } catch {
    return fallback
  }
}

function writeString(key: string, value: string): void {
  try {
    window.localStorage.setItem(key, value)
  } catch {
    /* ignore */
  }
}

function readBoolean(key: string, fallback: boolean): boolean {
  return readString(key, String(fallback)) === 'true'
}

export const useAppStore = defineStore('app', () => {
  /* ---------------- state ---------------- */
  /** 侧边栏是否折叠 */
  const sidebarCollapsed = ref<boolean>(readBoolean(SIDEBAR_KEY, false))
  /** 当前主题 */
  const theme = ref<ThemeMode>(readString(THEME_KEY, 'light') === 'dark' ? 'dark' : 'light')
  /** 设备类型（窄屏时自动折叠侧边栏） */
  const device = ref<DeviceType>('desktop')

  /* ---------------- actions ---------------- */

  /** 把主题写到 <html data-theme>，供 CSS 变量切换 */
  function applyTheme(): void {
    const root = document.documentElement
    root.dataset.theme = theme.value
    root.classList.toggle('dark', theme.value === 'dark')
  }

  function setTheme(value: ThemeMode): void {
    theme.value = value
    writeString(THEME_KEY, value)
    applyTheme()
  }

  function toggleTheme(): void {
    setTheme(theme.value === 'dark' ? 'light' : 'dark')
  }

  function setSidebarCollapsed(value: boolean): void {
    sidebarCollapsed.value = value
    writeString(SIDEBAR_KEY, String(value))
  }

  function toggleSidebar(): void {
    setSidebarCollapsed(!sidebarCollapsed.value)
  }

  function setDevice(value: DeviceType): void {
    device.value = value
    if (value === 'mobile') {
      setSidebarCollapsed(true)
    }
  }

  watch(theme, applyTheme, { immediate: true })

  return {
    // state
    sidebarCollapsed,
    theme,
    device,
    // actions
    applyTheme,
    setTheme,
    toggleTheme,
    setSidebarCollapsed,
    toggleSidebar,
    setDevice
  }
})
