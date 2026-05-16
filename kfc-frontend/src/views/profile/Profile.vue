<template>
  <div class="profile-page">
    <div class="toolbar">
      <div>
        <h2>Profile Management</h2>
        <span>Maintain manager account, contact details, and store information</span>
      </div>
    </div>

    <section class="panel">
      <div class="profile-head">
        <el-avatar :size="72" :src="form.avatar || defaultAvatar" />
        <div>
          <h3>{{ form.displayName || form.username }}</h3>
          <p>{{ form.role }} · {{ form.storeName }}</p>
        </div>
      </div>

      <el-form label-width="100px" class="profile-form">
        <el-form-item label="Username">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="Name">
          <el-input v-model="form.displayName" />
        </el-form-item>
        <el-form-item label="Role">
          <el-input v-model="form.role" />
        </el-form-item>
        <el-form-item label="Store">
          <el-input v-model="form.storeName" />
        </el-form-item>
        <el-form-item label="Phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="Avatar URL">
          <el-input v-model="form.avatar" />
        </el-form-item>
        <el-form-item label="New Password">
          <el-input v-model="form.password" type="password" show-password placeholder="Leave blank to keep unchanged" />
        </el-form-item>
        <el-form-item label="Bio">
          <el-input v-model="form.bio" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item>
          <el-button class="red-btn" @click="saveProfile">Save</el-button>
        </el-form-item>
      </el-form>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const defaultAvatar = '/kfc-logo.png'
const form = ref({})

const loadProfile = async () => {
  form.value = await request.get('/auth/profile')
}

const saveProfile = async () => {
  const payload = { ...form.value }
  const profile = await request.put('/auth/profile', payload)
  localStorage.setItem('kfc_profile', JSON.stringify(profile))
  form.value = { ...profile, password: '' }
  ElMessage.success('Profile saved')
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-page {
  max-width: 860px;
  margin: 0 auto;
}

.toolbar {
  margin-bottom: 16px;
}

.toolbar h2 {
  color: #111827;
  letter-spacing: 0;
}

.toolbar span {
  color: #64748b;
  font-size: 14px;
}

.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
  padding: 24px;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 20px;
}

.profile-head h3 {
  font-size: 22px;
  color: #111827;
  letter-spacing: 0;
}

.profile-head p {
  color: #64748b;
  margin-top: 6px;
}

.profile-form {
  max-width: 680px;
}
</style>
