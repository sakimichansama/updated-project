<template>
  <div class="stock-record-manager">
    <div class="toolbar">
      <div>
        <h2>Stock Records</h2>
        <span>Manage inbound and outbound records and keep inventory synchronized</span>
      </div>
      <div class="actions">
        <el-input v-model="searchText" clearable class="search-input" placeholder="Search stock records" @input="resetPages" @clear="resetPages" />
        <el-button class="outline-btn" @click="downloadTemplate(activeRecordType)">
          <el-icon><Document /></el-icon> Download Template
        </el-button>
        <el-upload :http-request="activeImportHandler" :show-file-list="false" accept=".xlsx,.xls">
          <el-button class="outline-btn">
            <el-icon><Upload /></el-icon> Import
          </el-button>
        </el-upload>
        <el-button class="outline-btn" @click="exportRecords(activeRecordType)">
          <el-icon><Download /></el-icon> Export
        </el-button>
        <el-button class="red-btn" @click="activeTab === 'in' ? openInDialog() : openOutDialog()">
          <el-icon><Plus /></el-icon> {{ activeTab === 'in' ? 'New Inbound' : 'New Outbound' }}
        </el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="modern-tabs">
      <el-tab-pane label="Inbound Records" name="in">
        <div class="glass-card table-card">
          <el-table :data="pagedInRecords" stripe class="modern-table" :row-class-name="tableRowClassName">
            <el-table-column label="Product" min-width="150">
              <template #default="{ row }">{{ getProductName(row.productId) }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="Quantity" width="100" />
            <el-table-column prop="price" label="Inbound Price" width="120">
              <template #default="{ row }">¥{{ row.price }}</template>
            </el-table-column>
            <el-table-column prop="supplier" label="Supplier" min-width="150" />
            <el-table-column prop="createTime" label="Time" min-width="170" />
            <el-table-column label="Actions" width="110" fixed="right">
              <template #default="{ row }">
                <el-button class="table-action-btn table-action-danger" @click="deleteInRecord(row.id)">Delete</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-bar">
            <el-pagination
              v-model:current-page="inPage"
              v-model:page-size="inPageSize"
              background
              layout="total, sizes, prev, pager, next"
              :page-sizes="PAGE_SIZES"
              :total="filteredInRecords.length"
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="Outbound Records" name="out">
        <div class="glass-card table-card">
          <el-table :data="pagedOutRecords" stripe class="modern-table outbound-table" :row-class-name="tableRowClassName">
            <el-table-column label="Product" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">{{ getProductName(row.productId) }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="Quantity" width="100" />
            <el-table-column label="Reason" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">
                <el-tag :type="reasonTagType(row.reason)" effect="light" round>
                  {{ getReasonLabel(row.reason) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Source" width="110">
              <template #default="{ row }">
                <el-tag :type="sourceTagType(row)" effect="plain" round>
                  {{ sourceLabel(row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Linked ID" width="100">
              <template #default="{ row }">{{ row.orderId || row.wasteId || '-' }}</template>
            </el-table-column>
            <el-table-column prop="costPrice" label="Cost Price" width="110">
              <template #default="{ row }">¥{{ Number(row.costPrice || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="createTime" label="Time" min-width="180" show-overflow-tooltip />
            <el-table-column label="Actions" width="120" fixed="right" align="center">
              <template #default="{ row }">
                <el-tooltip v-if="row.orderId || row.wasteId" content="Linked records cannot be deleted here" placement="top">
                  <el-button class="table-action-btn table-action-danger" disabled>Delete</el-button>
                </el-tooltip>
                <el-button v-else class="table-action-btn table-action-danger" @click="deleteOutRecord(row.id)">Delete</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-bar">
            <el-pagination
              v-model:current-page="outPage"
              v-model:page-size="outPageSize"
              background
              layout="total, sizes, prev, pager, next"
              :page-sizes="PAGE_SIZES"
              :total="filteredOutRecords.length"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="inDialog" title="New Inbound" width="520px">
      <el-form label-width="90px">
        <el-form-item label="Product">
          <el-select v-model="inForm.productId" filterable placeholder="Select product" style="width: 100%">
            <el-option v-for="p in productList" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Quantity">
          <el-input-number v-model="inForm.quantity" :min="1" />
        </el-form-item>
        <el-form-item label="Inbound Price">
          <el-input-number v-model="inForm.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="Supplier">
          <el-input v-model="inForm.supplier" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inDialog = false">Cancel</el-button>
        <el-button class="red-btn" @click="submitInStock">Save</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="outDialog" title="New Outbound" width="520px">
      <el-form label-width="90px">
        <el-form-item label="Product">
          <el-select v-model="outForm.productId" filterable placeholder="Select product" style="width: 100%">
            <el-option v-for="p in productList" :key="p.id" :label="`${p.name} (stock:  ${p.stock || 0})`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Quantity">
          <el-input-number v-model="outForm.quantity" :min="1" />
        </el-form-item>
        <el-form-item label="Reason">
          <el-select v-model="outForm.reason" style="width: 100%">
            <el-option label="Gift" value="Gift" />
            <el-option label="Internal Use" value="Internal Use" />
            <el-option label="Inventory Adjustment" value="Inventory Adjustment" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="outDialog = false">Cancel</el-button>
        <el-button class="red-btn" @click="submitOutStock">Save</el-button>
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

const activeTab = ref('in')
const productList = ref([])
const inForm = ref({ productId: null, quantity: 1, price: 0, supplier: '' })
const outForm = ref({ productId: null, quantity: 1, reason: 'Internal Use' })
const inRecords = ref([])
const outRecords = ref([])
const searchText = ref('')
const inPage = ref(1)
const outPage = ref(1)
const inPageSize = ref(DEFAULT_PAGE_SIZE)
const outPageSize = ref(DEFAULT_PAGE_SIZE)
const inDialog = ref(false)
const outDialog = ref(false)
const activeRecordType = computed(() => activeTab.value === 'in' ? 'in-stock' : 'out-stock')
const activeImportHandler = computed(() => activeTab.value === 'in' ? importInStock : importOutStock)
const filteredInRecords = computed(() => filterRows(inRecords.value, searchText.value, row => getProductName(row.productId)))
const filteredOutRecords = computed(() => filterRows(outRecords.value, searchText.value, row => `${getProductName(row.productId)} ${getReasonLabel(row.reason)} ${sourceLabel(row)}`))
const pagedInRecords = computed(() => pageRows(filteredInRecords.value, inPage.value, inPageSize.value))
const pagedOutRecords = computed(() => pageRows(filteredOutRecords.value, outPage.value, outPageSize.value))

const loadProducts = async () => {
  productList.value = await request.get('/products')
}

const loadRecords = async () => {
  inRecords.value = await request.get('/inventory/in-records')
  outRecords.value = await request.get('/inventory/out-records')
}

const reloadAll = async () => {
  await Promise.all([loadProducts(), loadRecords()])
}

const resetPages = () => {
  inPage.value = 1
  outPage.value = 1
}

const getProductName = (productId) => productList.value.find(p => p.id === productId)?.name || 'Unknown Product'
const getReasonLabel = (reason) => ({
  Sales: 'Sales',
  Waste: 'Waste',
  Gift: 'Gift',
  'Internal Use': 'Internal Use',
  'Inventory Adjustment': 'Inventory Adjustment'
}[reason] || reason)
const sourceLabel = (row) => row.orderId ? 'Order' : row.wasteId ? 'Waste' : 'Manual'
const reasonTagType = (reason) => ({
  Sales: 'success',
  Waste: 'danger',
  Gift: 'warning',
  'Internal Use': 'info',
  'Inventory Adjustment': 'primary'
}[reason] || 'info')
const sourceTagType = (row) => row.orderId ? 'success' : row.wasteId ? 'danger' : 'info'

const openInDialog = () => {
  inForm.value = { productId: null, quantity: 1, price: 0, supplier: '' }
  inDialog.value = true
}

const openOutDialog = () => {
  outForm.value = { productId: null, quantity: 1, reason: 'Internal Use' }
  outDialog.value = true
}

const submitInStock = async () => {
  if (!inForm.value.productId) return ElMessage.warning('Please select a product')
  await request.post('/inventory/in', null, { params: inForm.value })
  ElMessage.success('Inbound saved; inventory increased')
  inDialog.value = false
  reloadAll()
}

const submitOutStock = async () => {
  if (!outForm.value.productId) return ElMessage.warning('Please select a product')
  await request.post('/inventory/out', null, { params: outForm.value })
  ElMessage.success('Outbound saved; inventory deducted')
  outDialog.value = false
  reloadAll()
}

const deleteInRecord = async (id) => {
  await ElMessageBox.confirm('Deleting this inbound record will deduct product inventory. Continue?', 'Confirm Delete', { type: 'warning' })
  await request.delete(`/inventory/in-records/${id}`)
  ElMessage.success('Deleted; inventory deducted')
  reloadAll()
}

const deleteOutRecord = async (id) => {
  await ElMessageBox.confirm('Deleting this outbound record will restore product inventory. Continue?', 'Confirm Delete', { type: 'warning' })
  await request.delete(`/inventory/out-records/${id}`)
  ElMessage.success('Deleted; inventory restored')
  reloadAll()
}

const downloadTemplate = (type) => {
  window.open(`/api/reports/template/${type}`)
}

const exportRecords = (type) => {
  window.open(`/api/reports/export/${type}`)
}

const importInStock = async ({ file }) => {
  const formData = new FormData()
  formData.append('file', file)
  const data = await request.post('/reports/import/in-stock', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  ElMessage.success(`Successfully imported ${data.imported} inbound records; inventory increased`)
  reloadAll()
}

const importOutStock = async ({ file }) => {
  const formData = new FormData()
  formData.append('file', file)
  const data = await request.post('/reports/import/out-stock', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  ElMessage.success(`Successfully imported ${data.imported} outbound records; inventory deducted`)
  reloadAll()
}

const tableRowClassName = ({ rowIndex }) => rowIndex % 2 === 0 ? 'even-row' : 'odd-row'

onMounted(reloadAll)
</script>

<style scoped>
.stock-record-manager {
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

.modern-tabs :deep(.el-tabs__header) {
  margin: 0 0 20px 0;
}

.modern-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.modern-tabs :deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 500;
  padding: 0 24px;
  height: 48px;
  line-height: 48px;
  color: #5b6e8c;
}

.modern-tabs :deep(.el-tabs__item.is-active) {
  color: #e4002b;
}

.modern-tabs :deep(.el-tabs__active-bar) {
  background-color: #e4002b;
  height: 3px;
}

.glass-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(228, 0, 43, 0.15);
  margin-bottom: 24px;
  overflow: hidden;
}

.red-btn {
  background: #e4002b;
  border-color: #e4002b;
  border-radius: 8px;
  font-weight: 700;
  color: white !important;
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
}

.outline-btn:hover,
.outline-btn:focus {
  border-color: #c80025;
  color: #c80025;
  background: #fff5f7;
}

.modern-table {
  margin: 16px 22px 12px;
  width: calc(100% - 44px);
  border-radius: 10px;
  overflow: hidden;
}

.pagination-bar {
  padding: 0 22px 18px;
}

.outbound-table {
  min-width: 980px;
}

:deep(.outbound-table .el-tag) {
  min-width: 72px;
  justify-content: center;
}

:deep(.outbound-table .el-table__cell) {
  padding: 10px 0;
}

:deep(.el-table th) {
  background-color: #f1f5f9;
  color: #1e293b;
  font-weight: 600;
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
