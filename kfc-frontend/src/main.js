import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'  // Import Element Plus styles
import router from './router'          // Import router
import './assets/main.css'

const app = createApp(App)

app.use(ElementPlus)
app.use(router)

app.mount('#app')
