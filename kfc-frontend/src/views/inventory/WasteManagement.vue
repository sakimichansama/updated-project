<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>Waste Closure Management</h2>
        <span>Record waste, track handling status, and capture corrective actions</span>
      </div>
      <div class="actions">
        <el-input v-model="searchText" clearable class="search-input" placeholder="Search waste records" @input="currentPage = 1" @clear="currentPage = 1" />
        <el-date-picker v-model="month" type="month" value-format="YYYY-MM" @change="loadStats" />
        <el-button class="outline-btn" @click="downloadTemplate">
          <el-icon><Document /></el-icon> Download Template
        </el-button>
        <el-upload :http-request="importWaste" :show-file-list="false" accept=".xlsx,.xls">
          <el-button class="outline-btn">
            <el-icon><Upload /></el-icon> Import
          </el-button>
        </el-upload>
        <el-button class="outline-btn" @click="exportExcel">
          <el-icon><Download /></el-icon> Export
        </el-button>
        <el-button class="red-btn" @click="openAddDialog">
          <el-icon><Plus /></el-icon> New Waste
        </el-button>
      </div>
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <span>Monthly Waste Records</span>
        <strong>{{ stats.totalCount || 0 }}</strong>
      </div>
      <div class="summary-card">
        <span>Pending Closure</span>
        <strong>{{ stats.pendingCount || 0 }}</strong>
      </div>
      <div class="summary-card">
        <span>Waste Cost</span>
        <strong>¥{{ stats.totalCost || 0 }}</strong>
      </div>
      <div class="summary-card">
        <span>Closure Rate</span>
        <strong>{{ stats.closureRate || 0 }}%</strong>
      </div>
    </div>

    <section class="panel">
      <div class="panel-title">Waste Records</div>
      <el-table :data="pagedRecords" stripe>
        <el-table-column label="Product" min-width="150">
          <template #default="{ row }">{{ productName(row.productId) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="Quantity" width="90" />
        <el-table-column prop="reason" label="Reason" width="120" />
        <el-table-column prop="unitCost" label="Unit Cost" width="110">
          <template #default="{ row }">¥{{ row.unitCost || 0 }}</template>
        </el-table-column>
        <el-table-column label="Status" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'Closed' ? 'success' : 'warning'">{{ row.status || 'Pending' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handler" label="Handler" width="110" />
        <el-table-column prop="action" label="Action Taken" min-width="180" />
        <el-table-column prop="createTime" label="Created At" min-width="170" />
        <el-table-column label="Actions" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.status === 'Closed'" @click="openCloseDialog(row)">Close</el-button>
            <el-button link type="danger" @click="deleteWaste(row.id)">Delete</el-button>
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
          :total="filteredRecords.length"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" title="New Waste Record" width="520px">
      <el-form label-width="90px">
        <el-form-item label="Product">
          <el-select v-model="form.productId" placeholder="Select product" filterable style="width: 100%">
            <el-option v-for="item in products" :key="item.id" :label="`${item.name} (stock:  ${item.stock || 0})`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Quantity">
          <el-input-number v-model="form.quantity" :min="1" />
        </el-form-item>
        <el-form-item label="Reason">
          <el-select v-model="form.reason" style="width: 100%">
            <el-option label="Expired" value="Expired" />
            <el-option label="Damaged" value="Damaged" />
            <el-option label="Production Loss" value="Production Loss" />
            <el-option label="Inventory Variance" value="Inventory Variance" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button class="red-btn" @click="submitWaste">Save</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="closeDialog" title="Waste Closure Handling" width="460px">
      <el-form label-width="90px">
        <el-form-item label="Handler">
          <el-input v-model="closeForm.handler" />
        </el-form-item>
        <el-form-item label="Action Taken">
          <el-input v-model="closeForm.action" type="textarea" :rows="4" placeholder="Enter cause review, responsibility handling, or preventive actions" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog = false">Cancel</el-button>
        <el-button class="red-btn" @click="closeWaste">Confirm Closure</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '@/utils/request'
import { DEFAULT_PAGE_SIZE, PAGE_SIZES, filterRows, pageRows } from '@/utils/listTools'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Download, Plus, Upload } from '@element-plus/icons-vue'

const products = ref([])
const records = ref([])
const searchText = ref('')
const currentPage = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const stats = ref({})
const month = ref('')
const form = ref({ productId: null, quantity: 1, reason: 'Expired' })
const dialogVisible = ref(false)
const closeDialog = ref(false)
const closeForm = ref({ id: null, handler: '', action: '' })

const loadProducts = async () => {
  products.value = await request.get('/products')
}

const loadRecords = async () => {
  records.value = await request.get('/waste')
}

const loadStats = async () => {
  stats.value = await request.get('/waste/stats', { params: { month: month.value } })
}

const loadAll = async () => {
  await Promise.all([loadProducts(), loadRecords(), loadStats()])
}

const productName = (id) => products.value.find(item => item.id === id)?.name || id
const filteredRecords = computed(() => filterRows(records.value, searchText.value, row => productName(row.productId)))
const pagedRecords = computed(() => pageRows(filteredRecords.value, currentPage.value, pageSize.value))

const openAddDialog = () => {
  form.value = { productId: null, quantity: 1, reason: 'Expired' }
  dialogVisible.value = true
}

const submitWaste = async () => {
  if (!form.value.productId) return ElMessage.warning('Please select a product')
  await request.post('/waste', null, { params: form.value })
  ElMessage.success('Waste record saved; inventory deducted')
  dialogVisible.value = false
  loadAll()
}

const openCloseDialog = (row) => {
  closeForm.value = { id: row.id, handler: '', action: '' }
  closeDialog.value = true
}

const closeWaste = async () => {
  if (!closeForm.value.handler || !closeForm.value.action) {
    return ElMessage.warning('Please enter handler and action taken')
  }
  await request.put(`/waste/${closeForm.value.id}/close`, closeForm.value)
  ElMessage.success('Waste record closed')
  closeDialog.value = false
  loadAll()
}

const deleteWaste = async (id) => {
  await ElMessageBox.confirm('Deleting this record will restore inventory. Continue?', 'Confirm Delete', { type: 'warning' })
  await request.delete(`/waste/${id}`)
  ElMessage.success('Deleted and inventory restored')
  loadAll()
}

const downloadTemplate = () => {
  window.open('/api/reports/template/waste')
}

const importWaste = async ({ file }) => {
  const formData = new FormData()
  formData.append('file', file)
  const data = await request.post('/reports/import/waste', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  ElMessage.success(`Successfully imported ${data.imported} waste records; inventory deducted`)
  loadAll()
}

const exportExcel = () => {
  window.open('/api/reports/export/waste')
}

onMounted(() => {
  const now = new Date()
  month.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  loadAll()
})
</script>

<style scoped>
.page {
  max-width: 1400px;
  margin: 0 auto;
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
}

.toolbar span {
  color: #64748b;
  font-size: 14px;
}

.actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(160px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.summary-card,
.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.summary-card {
  padding: 18px;
  display: grid;
  gap: 8px;
}

.summary-card span {
  color: #64748b;
  font-size: 13px;
}

.summary-card strong {
  font-size: 28px;
  color: #111827;
}

.panel {
  padding: 16px;
  margin-bottom: 16px;
}

.panel-title {
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 14px;
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
  .summary-grid {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
