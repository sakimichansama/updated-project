<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="brand">
        <img src="/kfc-logo.png" alt="KFC" />
        <div>
          <h1>KFC Smart Operations System</h1>
          <p>Store operations data platform</p>
        </div>
      </div>

      <el-form class="login-form" @keyup.enter="submitLogin">
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="Username" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" size="large" type="password" show-password placeholder="Password" />
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="submitLogin">
          Log In
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const form = ref({ username: 'admin', password: 'admin123' })

const submitLogin = async () => {
  if (!form.value.username || !form.value.password) {
    return ElMessage.warning('Please enter username and password')
  }
  loading.value = true
  try {
    const data = await request.post('/auth/login', form.value)
    localStorage.setItem('kfc_token', data.token)
    localStorage.setItem('kfc_profile', JSON.stringify(data.profile))
    ElMessage.success('Login successful')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background:
    linear-gradient(135deg, rgba(228, 0, 43, 0.1), rgba(255, 255, 255, 0.2)),
    url('/kfc-logo.png') center 18% / 220px no-repeat,
    #f4f6fb;
  padding: 24px;
}

.login-panel {
  width: min(420px, 100%);
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.12);
  padding: 32px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
}

.brand img {
  width: 48px;
  height: 48px;
  object-fit: contain;
}

.brand h1 {
  font-size: 24px;
  color: #111827;
  margin-bottom: 6px;
  letter-spacing: 0;
}

.brand p {
  color: #6b7280;
  font-size: 14px;
}

.login-form {
  display: grid;
  gap: 10px;
}

.login-btn {
  width: 100%;
  background: #e4002b;
  border: none;
  border-radius: 8px;
  font-weight: 700;
}
</style>
