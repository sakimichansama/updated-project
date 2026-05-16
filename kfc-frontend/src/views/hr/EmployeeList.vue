<template>
  <div class="employee-manager">
    <div class="toolbar">
      <div>
        <h2>Employee Records</h2>
        <span>Manage employee profiles, positions, hire dates, and salary data</span>
      </div>
      <div class="actions">
        <el-button class="outline-btn" @click="downloadTemplate">
          <el-icon><Document /></el-icon> Download Template
        </el-button>
        <el-upload :http-request="importEmployees" :show-file-list="false" accept=".xlsx,.xls">
          <el-button class="outline-btn">
            <el-icon><Upload /></el-icon> Import
          </el-button>
        </el-upload>
        <el-button class="outline-btn" @click="exportEmployees">
          <el-icon><Download /></el-icon> Export
        </el-button>
        <el-button class="red-btn" @click="openAddDialog">
          <el-icon><Plus /></el-icon> New Employee
        </el-button>
      </div>
    </div>

    <div class="glass-card">
      <div class="list-tools">
        <el-input v-model="searchText" clearable class="search-input" placeholder="Search employees" @input="currentPage = 1" @clear="currentPage = 1" />
      </div>
      <el-table :data="pagedEmployees" stripe class="modern-table" :row-class-name="tableRowClassName">
        <el-table-column prop="name" label="Name" min-width="120" />
        <el-table-column prop="phone" label="Phone" min-width="130" />
        <el-table-column prop="idCard" label="ID Card" min-width="180" />
        <el-table-column prop="hireDate" label="Hire Date" width="120" />
        <el-table-column prop="position" label="Position" min-width="120" />
        <el-table-column prop="monthlySalary" label="Monthly Salary" width="140">
          <template #default="{ row }">¥{{ row.monthlySalary || '-' }}</template>
        </el-table-column>
        <el-table-column label="Actions" width="160" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button class="action-btn edit-btn" @click="editEmployee(row)">Edit</el-button>
              <el-button class="action-btn delete-btn" @click="deleteEmployee(row.id)">Delete</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          background
          layout="total, sizes, prev, pager, next"
          :page-sizes="PAGE_SIZES"
          :total="filteredEmployees.length"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" class="modern-dialog">
      <el-form :model="form" label-width="100px" class="dialog-form">
        <el-form-item label="Name">
          <el-input v-model="form.name" placeholder="Enter name" class="modern-input" />
        </el-form-item>
        <el-form-item label="Phone">
          <el-input v-model="form.phone" placeholder="Phone number" class="modern-input" />
        </el-form-item>
        <el-form-item label="ID Card">
          <el-input v-model="form.idCard" placeholder="ID Card" class="modern-input" />
        </el-form-item>
        <el-form-item label="Hire Date">
          <el-date-picker v-model="form.hireDate" type="date" value-format="YYYY-MM-DD" class="modern-datepicker" />
        </el-form-item>
        <el-form-item label="Position">
          <el-input v-model="form.position" placeholder="Position" class="modern-input" />
        </el-form-item>
        <el-form-item label="Monthly Salary">
          <el-input-number v-model="form.monthlySalary" :min="0" controls-position="right" class="modern-number" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false" class="outline-btn">Cancel</el-button>
        <el-button class="red-btn" @click="saveEmployee">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import request from '@/utils/request'
import { DEFAULT_PAGE_SIZE, PAGE_SIZES, filterRows, pageRows } from '@/utils/listTools'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Download, Plus, Upload } from '@element-plus/icons-vue'

const employeeList = ref([])
const searchText = ref('')
const currentPage = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const dialogVisible = ref(false)
const dialogTitle = ref('New Employee')
const form = ref({})
const filteredEmployees = computed(() => filterRows(employeeList.value, searchText.value))
const pagedEmployees = computed(() => pageRows(filteredEmployees.value, currentPage.value, pageSize.value))

const loadEmployees = async () => {
  const data = await request.get('/employees')
  employeeList.value = data
}

const openAddDialog = () => {
  dialogTitle.value = 'New Employee'
  form.value = {}
  dialogVisible.value = true
}

const editEmployee = (row) => {
  dialogTitle.value = 'Edit Employee'
  form.value = { ...row }
  dialogVisible.value = true
}

const saveEmployee = async () => {
  if (form.value.id) {
    await request.put(`/employees/${form.value.id}`, form.value)
    ElMessage.success('Employee updated')
  } else {
    await request.post('/employees', form.value)
    ElMessage.success('Employee added')
  }
  dialogVisible.value = false
  loadEmployees()
}

const downloadTemplate = () => {
  window.open('/api/reports/template/employees')
}

const importEmployees = async ({ file }) => {
  const formData = new FormData()
  formData.append('file', file)
  const data = await request.post('/reports/import/employees', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  ElMessage.success(`Successfully imported ${data.imported} employees`)
  loadEmployees()
}

const exportEmployees = () => {
  window.open('/api/reports/export/employees')
}

const deleteEmployee = (id) => {
  ElMessageBox.confirm('Delete this employee?', 'Confirm Delete', { type: 'warning' }).then(async () => {
    await request.delete(`/employees/${id}`)
    ElMessage.success('Employee deleted')
    loadEmployees()
  })
}

const tableRowClassName = ({ rowIndex }) => {
  return rowIndex % 2 === 0 ? 'even-row' : 'odd-row'
}

onMounted(() => loadEmployees())
</script>

<style scoped>
.employee-manager {
  max-width: 1400px;
  margin: 0 auto;
  padding: 4px;
}

.toolbar,
.actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar {
  justify-content: space-between;
  margin-bottom: 16px;
}

.toolbar h2 {
  color: #111827;
  letter-spacing: 0;
  margin: 0 0 4px;
  font-size: 24px;
  font-weight: 700;
}

.toolbar span {
  color: #64748b;
  font-size: 14px;
}

.actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

/* Glass card */
.glass-card {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-radius: 32px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.05);
  transition: all 0.3s cubic-bezier(0.2, 0.9, 0.4, 1.1);
  border: 1px solid rgba(228, 0, 43, 0.15);
  margin-bottom: 24px;
  overflow: hidden;
}
.glass-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 35px -8px rgba(0, 0, 0, 0.12);
  border-color: rgba(228, 0, 43, 0.4);
}

.red-btn {
  background: #e4002b;
  border-color: #e4002b;
  border-radius: 8px;
  padding: 8px 20px;
  font-weight: 600;
  color: white !important;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.red-btn:hover,
.red-btn:focus {
  background: #c80025;
  border-color: #c80025;
  color: white;
}
.outline-btn {
  border-radius: 8px;
  border: 1px solid #e4002b;
  color: #e4002b;
  background: white;
  padding: 8px 20px;
}
.outline-btn:hover {
  border-color: #e4002b;
  color: #e4002b;
  background: rgba(228, 0, 43, 0.05);
}

/* Table layout */
.list-tools {
  padding: 18px 24px 0;
}

.modern-table {
  margin: 12px 24px 12px 24px;
  width: calc(100% - 48px);
  border-radius: 20px;
  overflow: hidden;
}

.pagination-bar {
  padding: 0 24px 18px;
}
:deep(.el-table th) {
  background-color: #f1f5f9;
  color: #1e293b;
  font-weight: 600;
  font-size: 14px;
  border-bottom: none;
  padding: 14px 0;
}
:deep(.el-table td) {
  border-bottom: 1px solid #eef2ff;
  padding: 14px 0;
}
:deep(.el-table .cell) {
  padding: 0 16px;
}
:deep(.el-table .even-row) {
  background-color: #ffffff;
}
:deep(.el-table .odd-row) {
  background-color: #fafcff;
}
:deep(.el-table__row:hover > td) {
  background-color: #ffefef !important;
}
:deep(.el-table__inner-wrapper::before) {
  display: none;
}

/* Action button group */
.action-buttons {
  display: flex;
  gap: 8px;
  align-items: center;
}
/* Base action button style */
.action-btn {
  border-radius: 24px;
  padding: 5px 14px;
  font-size: 13px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  background-color: #f1f5f9;
  color: #334155;
}
.action-btn:hover {
  transform: translateY(-1px);
}
/* Edit button */
.edit-btn {
  background-color: #eef2ff;
  color: #4f46e5;
}
.edit-btn:hover {
  background-color: #e0e7ff;
  color: #4338ca;
  box-shadow: 0 2px 6px rgba(79, 70, 229, 0.2);
}
/* Delete button */
.delete-btn {
  background-color: #fef2f2;
  color: #e4002b;
}
.delete-btn:hover {
  background-color: #fee2e2;
  color: #c0001f;
  box-shadow: 0 2px 6px rgba(228, 0, 43, 0.2);
}

/* Dialog glass effect */
:deep(.modern-dialog .el-dialog) {
  border-radius: 32px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
}
:deep(.modern-dialog .el-dialog__header) {
  border-bottom: 1px solid rgba(228, 0, 43, 0.2);
  padding: 20px 24px;
}
:deep(.modern-dialog .el-dialog__title) {
  font-weight: 700;
  color: #1e293b;
}
:deep(.modern-dialog .el-dialog__body) {
  padding: 24px;
}
:deep(.modern-dialog .el-dialog__footer) {
  border-top: 1px solid #eef2ff;
  padding: 16px 24px;
}

/* Form input style */
.modern-input {
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 8px 16px;
  font-size: 14px;
  background: white;
  transition: all 0.2s;
  outline: none;
}
.modern-input:focus {
  border-color: #e4002b;
  box-shadow: 0 0 0 3px rgba(228, 0, 43, 0.2);
}
.modern-datepicker {
  border-radius: 20px;
}
.modern-number :deep(.el-input-number__input) {
  border-radius: 20px;
}
.dialog-form {
  margin-top: 8px;
}

@media (max-width: 900px) {
  .toolbar {
    display: grid;
    grid-template-columns: 1fr;
  }

  .actions {
    justify-content: flex-start;
  }
}
</style>
