import { createRouter, createWebHistory } from 'vue-router'

import Login from '@/views/auth/Login.vue'
import HomeDashboard from '@/views/dashboard/HomeDashboard.vue'
import ProductList from '@/views/inventory/ProductList.vue'
import StockRecords from '@/views/inventory/StockRecords.vue'
import InventoryAlerts from '@/views/inventory/InventoryAlerts.vue'
import WasteManagement from '@/views/inventory/WasteManagement.vue'
import EmployeeList from '@/views/hr/EmployeeList.vue'
import WorkHourPayroll from '@/views/hr/WorkHourPayroll.vue'
import EmployeePerformance from '@/views/hr/EmployeePerformance.vue'
import SalesDaily from '@/views/finance/SalesDaily.vue'
import SalesTrend from '@/views/finance/SalesTrend.vue'
import ProfitReport from '@/views/finance/ProfitReport.vue'
import ProductMarginAnalysis from '@/views/finance/ProductMarginAnalysis.vue'
import AiAssistant from '@/views/assistant/AiAssistant.vue'
import Profile from '@/views/profile/Profile.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'Login', component: Login, meta: { public: true } },
    { path: '/', redirect: '/dashboard' },
    { path: '/dashboard', name: 'HomeDashboard', component: HomeDashboard },
    { path: '/inventory/products', name: 'ProductList', component: ProductList },
    { path: '/inventory/records', name: 'StockRecords', component: StockRecords },
    { path: '/inventory/alerts', name: 'InventoryAlerts', component: InventoryAlerts },
    { path: '/inventory/waste', name: 'WasteManagement', component: WasteManagement },
    { path: '/hr/employees', name: 'EmployeeList', component: EmployeeList },
    { path: '/hr/payroll', name: 'WorkHourPayroll', component: WorkHourPayroll },
    { path: '/hr/performance', name: 'EmployeePerformance', component: EmployeePerformance },
    { path: '/finance/sales', name: 'SalesDaily', component: SalesDaily },
    { path: '/finance/sales-trend', name: 'SalesTrend', component: SalesTrend },
    { path: '/finance/profit', name: 'ProfitReport', component: ProfitReport },
    { path: '/finance/product-margin', name: 'ProductMarginAnalysis', component: ProductMarginAnalysis },
    { path: '/assistant', name: 'AiAssistant', component: AiAssistant },
    { path: '/profile', name: 'Profile', component: Profile },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('kfc_token')
  if (!to.meta.public && !token) {
    return '/login'
  }
  if (to.path === '/login' && token) {
    return '/dashboard'
  }
  return true
})

export default router
