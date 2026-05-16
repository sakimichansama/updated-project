<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>Work Hours and Payroll</h2>
        <span>Enter daily work hours and calculate monthly payroll</span>
      </div>
      <div class="actions">
        <el-button class="outline-btn" @click="downloadTemplate">
          <el-icon><Document /></el-icon> Download Template
        </el-button>
        <el-upload :http-request="importWorkHours" :show-file-list="false" accept=".xlsx,.xls">
          <el-button class="outline-btn">
            <el-icon><Upload /></el-icon> Import Hours
          </el-button>
        </el-upload>
        <el-button class="outline-btn" @click="exportWorkHours">
          <el-icon><Download /></el-icon> Export Hours
        </el-button>
        <el-button class="red-btn" @click="openHourDialog">
          <el-icon><Plus /></el-icon> New Work Hours
        </el-button>
      </div>
    </div>

    <section class="panel">
      <div class="panel-header">
        <div class="panel-title"><el-icon><Money /></el-icon> Monthly Payroll</div>
        <div class="actions">
          <el-input v-model="searchText" clearable class="search-input" placeholder="Search payroll" @input="currentPage = 1" @clear="currentPage = 1" />
          <el-date-picker v-model="month" type="month" value-format="YYYY-MM" placeholder="Select Month" @change="loadPayroll" />
          <el-button class="red-btn" @click="exportPayroll">
            <el-icon><Download /></el-icon> Export Payroll
          </el-button>
        </div>
      </div>
      <el-table :data="pagedPayrollList" stripe>
        <el-table-column prop="employeeName" label="Name" min-width="120" />
        <el-table-column prop="totalHours" label="Total Hours" width="120" />
        <el-table-column prop="baseSalary" label="Base Salary" width="130">
          <template #default="{ row }">¥{{ row.baseSalary || 0 }}</template>
        </el-table-column>
        <el-table-column prop="totalSalary" label="Gross Pay" width="130">
          <template #default="{ row }">¥{{ row.totalSalary || 0 }}</template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          background
          layout="total, sizes, prev, pager, next"
          :page-sizes="PAGE_SIZES"
          :total="filteredPayrollList.length"
        />
      </div>
    </section>

    <el-dialog v-model="hourDialog" title="New Work Hours" width="520px">
      <el-form label-width="90px">
        <el-form-item label="Employee">
          <el-select v-model="hourForm.employeeId" placeholder="Select employee" filterable style="width: 100%">
            <el-option v-for="employee in employeeList" :key="employee.id" :label="employee.name" :value="employee.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Date">
          <el-date-picker v-model="hourForm.workDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="Hours">
          <el-input-number v-model="hourForm.hours" :min="0" :step="0.5" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="hourDialog = false">Cancel</el-button>
        <el-button class="red-btn" @click="submitWorkHour">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import request from '@/utils/request'
import { DEFAULT_PAGE_SIZE, PAGE_SIZES, filterRows, pageRows } from '@/utils/listTools'
import { ElMessage } from 'element-plus'
import { Document, Download, Money, Plus, Upload } from '@element-plus/icons-vue'

const employeeList = ref([])
const hourDialog = ref(false)
const hourForm = ref({ employeeId: null, workDate: '', hours: 8 })
const month = ref('')
const payrollList = ref([])
const searchText = ref('')
const currentPage = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const filteredPayrollList = computed(() => filterRows(payrollList.value, searchText.value))
const pagedPayrollList = computed(() => pageRows(filteredPayrollList.value, currentPage.value, pageSize.value))

const loadEmployees = async () => {
  employeeList.value = await request.get('/employees')
}

const openHourDialog = () => {
  const today = new Date().toISOString().slice(0, 10)
  hourForm.value = { employeeId: null, workDate: today, hours: 8 }
  hourDialog.value = true
}

const submitWorkHour = async () => {
  if (!hourForm.value.employeeId) return ElMessage.warning('Please select an employee')
  if (!hourForm.value.workDate) return ElMessage.warning('Please select a date')
  await request.post('/workhours', null, { params: hourForm.value })
  ElMessage.success('Work hours saved')
  hourDialog.value = false
  loadPayroll()
}

const loadPayroll = async () => {
  if (!month.value) return
  payrollList.value = await request.get('/payroll/month', { params: { month: month.value } })
}

const downloadTemplate = () => {
  window.open('/api/reports/template/work-hours')
}

const importWorkHours = async ({ file }) => {
  const formData = new FormData()
  formData.append('file', file)
  const data = await request.post('/reports/import/work-hours', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  ElMessage.success(`Successfully imported ${data.imported} work hour records`)
  loadPayroll()
}

const exportWorkHours = () => {
  if (!month.value) return ElMessage.warning('Please select a month')
  window.open(`/api/reports/export/work-hours?month=${month.value}`)
}

const exportPayroll = () => {
  if (!month.value) return ElMessage.warning('Please select a month')
  window.open(`/api/reports/export/payroll?month=${month.value}`)
}

onMounted(() => {
  loadEmployees()
  const now = new Date()
  month.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  loadPayroll()
})
</script>

<style scoped>
.page {
  max-width: 1400px;
  margin: 0 auto;
}

.toolbar,
.actions,
.panel-header,
.panel-title {
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
}

.toolbar span {
  color: #64748b;
  font-size: 14px;
}

.actions {
  flex-wrap: wrap;
}

.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
  padding: 16px;
}

.panel-header {
  justify-content: space-between;
  margin-bottom: 14px;
}

.panel-title {
  font-weight: 700;
  color: #1f2937;
}

.panel-title .el-icon {
  color: #e4002b;
}

.pagination-bar {
  padding-top: 14px;
}

.red-btn {
  background: #e4002b;
  border-color: #e4002b;
  color: #fff;
  font-weight: 700;
}

.red-btn:hover,
.red-btn:focus {
  background: #c80025;
  border-color: #c80025;
  color: #fff;
}

.outline-btn {
  border-color: #e4002b;
  color: #e4002b;
  background: #fff;
}

.outline-btn:hover,
.outline-btn:focus {
  border-color: #c80025;
  color: #c80025;
  background: #fff5f7;
}

@media (max-width: 900px) {
  .toolbar,
  .panel-header {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
