import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { pinia } from './stores'
import { setupPermissionDirective } from './utils/permission'
import './styles/index.scss'

const app = createApp(App)

// 全局注册 Element Plus 图标（模板里可直接 <el-icon><User /></el-icon> 或用图标名做动态组件）
Object.entries(ElementPlusIconsVue).forEach(([name, component]) => {
  app.component(name, component)
})

app.use(pinia)
app.use(router)
// 中文界面
app.use(ElementPlus, { locale: zhCn, size: 'default', zIndex: 3000 })

// v-permission 指令
setupPermissionDirective(app)

app.mount('#app')
