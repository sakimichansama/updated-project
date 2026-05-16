<template>
  <el-container class="app-wrapper">
    
    <el-aside :width="isCollapse ? '80px' : '260px'" class="sidebar-container">
      <div class="logo-container">
        <img src="/kfc-logo.png" alt="KFC" class="logo-img" />
        <span v-show="!isCollapse" class="logo-text">KFC Manager</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        class="sidebar-menu"
        background-color="transparent"
        text-color="#e2e8f0"
        active-text-color="#e4002b"
      >
        <el-menu-item index="/dashboard">
          <el-icon><TrendCharts /></el-icon>
          <span>Operations Home</span>
        </el-menu-item>
        <el-sub-menu index="inventory">
          <template #title>
            <el-icon><Box /></el-icon>
            <span>Inventory Management</span>
          </template>
          <el-menu-item index="/inventory/products">Product Catalog</el-menu-item>
          <el-menu-item index="/inventory/records">Stock Records</el-menu-item>
          <el-menu-item index="/inventory/alerts">Inventory Alerts</el-menu-item>
          <el-menu-item index="/inventory/waste">Waste Closure</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="hr">
          <template #title>
            <el-icon><User /></el-icon>
            <span>HR and Payroll</span>
          </template>
          <el-menu-item index="/hr/employees">Employee Records</el-menu-item>
          <el-menu-item index="/hr/payroll">Work Hours and Payroll</el-menu-item>
          <el-menu-item index="/hr/performance">Employee Performance</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="finance">
          <template #title>
            <el-icon><TrendCharts /></el-icon>
            <span>Sales and Finance</span>
          </template>
          <el-menu-item index="/finance/sales">Order Entry</el-menu-item>
          <el-menu-item index="/finance/sales-trend">Sales Trend</el-menu-item>
          <el-menu-item index="/finance/profit">Profit Statistics Analysis</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="tools">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>Smart Tools</span>
          </template>
          <el-menu-item index="/assistant">Knowledge Q&A</el-menu-item>
        </el-sub-menu>
      </el-menu>
      <div class="collapse-btn" @click="toggleSidebar">
        <el-icon><Fold /></el-icon>
      </div>
    </el-aside>

    
    <el-container class="main-container">
      
      <el-header class="header-container">
        <div class="header-left">
          <div class="breadcrumb">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/' }">Home</el-breadcrumb-item>
              <el-breadcrumb-item v-if="currentRoute">{{ currentRoute }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="36" :src="profile.avatar || '/kfc-logo.png'" />
              <span class="username">{{ profile.displayName || 'Store Manager' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">Profile</el-dropdown-item>
                <el-dropdown-item divided command="logout">Log Out</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Box, User, TrendCharts, Fold, ArrowDown, Document } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const isCollapse = ref(false)
const syncSidebarForViewport = () => {
  if (window.innerWidth <= 760) {
    isCollapse.value = true
  }
}

const activeMenu = computed(() => route.path)
const profile = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('kfc_profile') || '{}')
  } catch {
    return {}
  }
})


const currentRoute = computed(() => {
  const map = {
    '/dashboard': 'Operations Home',
    '/inventory/products': 'Product Catalog',
    '/inventory/records': 'Stock Records',
    '/inventory/alerts': 'Inventory Alerts',
    '/inventory/waste': 'Waste Closure',
    '/hr/employees': 'Employee Records',
    '/hr/payroll': 'Work Hours and Payroll',
    '/hr/performance': 'Employee Performance',
    '/finance/sales': 'Order Entry',
    '/finance/sales-trend': 'Sales Trend',
    '/finance/profit': 'Profit Statistics Analysis',
    '/assistant': 'Knowledge Q&A',
    '/profile': 'Profile'
  }
  return map[route.path] || ''
})

const toggleSidebar = () => {
  isCollapse.value = !isCollapse.value
}

const handleCommand = (cmd) => {
  if (cmd === 'logout') {
    localStorage.removeItem('kfc_token')
    localStorage.removeItem('kfc_profile')
    router.push('/login')
  } else if (cmd === 'profile') {
    router.push('/profile')
  }
}

onMounted(() => {
  syncSidebarForViewport()
  window.addEventListener('resize', syncSidebarForViewport)
})

onUnmounted(() => {
  window.removeEventListener('resize', syncSidebarForViewport)
})
</script>

<style scoped>
.app-wrapper {
  height: 100vh;
  width: 100%;
  background: linear-gradient(135deg, #f5f7fa 0%, #e9edf2 100%);
}


.sidebar-container {
  background: rgba(30, 35, 48, 0.85);
  backdrop-filter: blur(16px);
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  transition: width 0.3s cubic-bezier(0.2, 0.9, 0.4, 1.1);
  display: flex;
  flex-direction: column;
  position: relative;
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.08);
  z-index: 10;
}

.logo-container {
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  margin-bottom: 20px;
}
.logo-img {
  width: 36px;
  height: 36px;
  object-fit: contain;
}
.logo-text {
  color: white;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #fff, #e4002b);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
  background: transparent;
}
:deep(.el-sub-menu__title),
:deep(.el-menu-item) {
  border-radius: 16px;
  margin: 4px 12px;
  transition: all 0.2s;
}
:deep(.el-sub-menu__title:hover),
:deep(.el-menu-item:hover) {
  background-color: rgba(228, 0, 43, 0.2) !important;
  color: white;
}
:deep(.el-menu-item.is-active) {
  background: linear-gradient(95deg, #e4002b, #ff5e7e);
  color: white;
  box-shadow: 0 4px 12px rgba(228, 0, 43, 0.3);
}
:deep(.el-menu-item.is-active .el-icon) {
  color: white;
}

.collapse-btn {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.2s;
  color: white;
}
.collapse-btn:hover {
  background: rgba(228, 0, 43, 0.6);
  transform: translateX(-50%) scale(1.05);
}


.main-container {
  background: transparent;
}

.header-container {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}
.header-left {
  display: flex;
  align-items: center;
}
.breadcrumb {
  font-size: 14px;
}
:deep(.el-breadcrumb__inner) {
  color: #4a5568;
  font-weight: 500;
}
:deep(.el-breadcrumb__inner.is-link:hover) {
  color: #e4002b;
}
:deep(.el-breadcrumb__separator) {
  color: #a0aec0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 40px;
  transition: background 0.2s;
}
.user-info:hover {
  background: rgba(0, 0, 0, 0.04);
}
.username {
  font-size: 14px;
  font-weight: 500;
  color: #2d3748;
}


.main-content {
  background: transparent;
  padding: 24px 28px;
  overflow-y: auto;
  height: calc(100vh - 60px);
}


.fade-transform-leave-active,
.fade-transform-enter-active {
  transition: all 0.3s ease;
}
.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(20px);
}
.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

@media (max-width: 760px) {
  .sidebar-container {
    width: 80px !important;
  }

  .logo-container {
    padding: 0 12px;
  }

  .logo-text,
  .username {
    display: none;
  }

  .header-container {
    padding: 0 16px;
  }

  .main-content {
    padding: 16px;
  }
}
</style>
