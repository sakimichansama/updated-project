<template>
  <div class="order-page">
    <div class="toolbar">
      <div>
        <h2>Order Entry</h2>
        <span>One order can include multiple products; saving creates sales outbound records and deducts inventory</span>
      </div>
      <div class="actions">
        <el-button class="outline-btn" @click="downloadTemplate">
          <el-icon><Document /></el-icon> Download Template
        </el-button>
        <el-upload :http-request="importOrders" :show-file-list="false" accept=".xlsx,.xls">
          <el-button class="outline-btn">
            <el-icon><Upload /></el-icon> Import
          </el-button>
        </el-upload>
        <el-button class="outline-btn" @click="exportOrders">
          <el-icon><Download /></el-icon> Export
        </el-button>
        <el-button class="red-btn" @click="openAddDialog">
          <el-icon><Plus /></el-icon> New Order
        </el-button>
      </div>
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <span>Orders</span>
        <strong>{{ orderList.length }}</strong>
      </div>
      <div class="summary-card">
        <span>Order Amount</span>
        <strong>¥{{ totalAmount.toFixed(2) }}</strong>
      </div>
      <div class="summary-card">
        <span>Average Ticket</span>
        <strong>¥{{ avgTicket.toFixed(2) }}</strong>
      </div>
    </div>

    <section class="panel">
      <div class="search-bar">
        <el-input v-model="searchText" clearable class="search-input" placeholder="Search orders" @input="currentPage = 1" @clear="currentPage = 1" />
        <div class="date-input-group">
          <label>Start Date</label>
          <input v-model="startDate" type="date" class="date-input" />
        </div>
        <div class="date-input-group">
          <label>End Date</label>
          <input v-model="endDate" type="date" class="date-input" />
        </div>
        <el-button class="red-btn" @click="loadOrders">Search</el-button>
        <el-button class="outline-btn" @click="resetDateRange">Last 7 Days</el-button>
      </div>

      <el-table :data="pagedOrders" stripe>
        <el-table-column prop="orderNo" label="Order No." min-width="170" />
        <el-table-column prop="orderTime" label="Order Time" min-width="170">
          <template #default="{ row }">{{ formatTime(row.orderTime) }}</template>
        </el-table-column>
        <el-table-column prop="creatorName" label="Creator" width="120">
          <template #default="{ row }">{{ row.creatorName || '-' }}</template>
        </el-table-column>
        <el-table-column label="Items" min-width="260">
          <template #default="{ row }">{{ itemSummary(row.items) }}</template>
        </el-table-column>
        <el-table-column prop="paymentMethod" label="Payment Method" width="110" />
        <el-table-column prop="orderAmount" label="Order Amount" width="120">
          <template #default="{ row }">¥{{ Number(row.orderAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="Status" width="90" />
        <el-table-column label="Actions" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="editOrder(row)">Edit</el-button>
            <el-button link type="danger" @click="deleteOrder(row.id)">Delete</el-button>
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
          :total="filteredOrders.length"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="900px" destroy-on-close>
      <el-form label-width="90px">
        <div class="form-grid">
          <el-form-item label="Order No.">
            <el-input v-model="form.orderNo" placeholder="Leave blank to auto-generate" />
          </el-form-item>
          <el-form-item label="Order Time">
            <input v-model="form.orderTime" type="datetime-local" class="date-input full" />
          </el-form-item>
          <el-form-item label="Customer">
            <el-input v-model="form.customerName" placeholder="Walk-in customer/member name" />
          </el-form-item>
          <el-form-item label="Creator">
            <el-input v-model="form.creatorName" disabled />
          </el-form-item>
          <el-form-item label="Payment Method">
            <el-select v-model="form.paymentMethod" placeholder="Select payment method">
              <el-option label="Cash" value="Cash" />
              <el-option label="WeChat Pay" value="WeChat Pay" />
              <el-option label="Alipay" value="Alipay" />
              <el-option label="Bank Card" value="Bank Card" />
            </el-select>
          </el-form-item>
          <el-form-item label="Discount">
            <el-input-number v-model="form.discountAmount" :min="0" :precision="2" />
          </el-form-item>
          <el-form-item label="Remark">
            <el-input v-model="form.remark" />
          </el-form-item>
        </div>

        <div class="items-header">
          <strong>Items</strong>
          <el-button class="red-btn small" @click="addItem">Add Product</el-button>
        </div>
        <div class="item-row item-row-head">
          <span>Product</span>
          <span>Qty</span>
          <span>Unit Price</span>
          <span>Amount</span>
          <span></span>
        </div>
        <div class="item-row" v-for="(item, index) in form.items" :key="index">
          <el-select v-model="item.productId" filterable placeholder="Product" @change="onProductChange(item)" class="product-select">
            <el-option v-for="product in productList" :key="product.id" :label="`${product.name} (stock:  ${product.stock || 0})`" :value="product.id" />
          </el-select>
          <el-input-number v-model="item.quantity" :min="1" controls-position="right" @change="syncAmount" />
          <span class="unit-price">¥{{ Number(item.salePrice || 0).toFixed(2) }}</span>
          <span class="line-amount">¥{{ lineAmount(item).toFixed(2) }}</span>
          <el-button link type="danger" @click="removeItem(index)">Delete</el-button>
        </div>
      </el-form>

      <div class="order-total">
        <span>Subtotal: ¥{{ subtotal.toFixed(2) }}</span>
        <span>Discount: ¥{{ Number(form.discountAmount || 0).toFixed(2) }}</span>
        <strong>Order Amount: ¥{{ orderAmount.toFixed(2) }}</strong>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button class="red-btn" :loading="saving" @click="saveOrder">Save</el-button>
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

const productList = ref([])
const orderList = ref([])
const searchText = ref('')
const currentPage = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const startDate = ref('')
const endDate = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('New Order')
const saving = ref(false)
const form = ref(createEmptyForm())

const totalAmount = computed(() => orderList.value.reduce((sum, item) => sum + Number(item.orderAmount || 0), 0))
const avgTicket = computed(() => orderList.value.length ? totalAmount.value / orderList.value.length : 0)
const subtotal = computed(() => form.value.items.reduce((sum, item) => sum + lineAmount(item), 0))
const orderAmount = computed(() => Math.max(0, subtotal.value - Number(form.value.discountAmount || 0)))
const filteredOrders = computed(() => filterRows(orderList.value, searchText.value, row => itemSummary(row.items)))
const pagedOrders = computed(() => pageRows(filteredOrders.value, currentPage.value, pageSize.value))

function createEmptyForm() {
  const user = currentUser()
  return {
    id: null,
    orderNo: '',
    orderTime: toLocalDateTimeInput(new Date()),
    customerName: 'Walk-in Customer',
    creatorId: user.id,
    creatorName: user.name,
    paymentMethod: 'WeChat Pay',
    discountAmount: 0,
    remark: '',
    items: [{ productId: null, quantity: 1, salePrice: 0 }]
  }
}

const loadProducts = async () => {
  productList.value = await request.get('/products')
}

const loadOrders = async () => {
  if (!startDate.value || !endDate.value) return ElMessage.warning('Please select a date range')
  orderList.value = await request.get('/orders', { params: { start: startDate.value, end: endDate.value } })
}

const resetDateRange = () => {
  const today = new Date()
  const weekAgo = new Date(today.getTime() - 7 * 24 * 3600 * 1000)
  startDate.value = weekAgo.toISOString().slice(0, 10)
  endDate.value = today.toISOString().slice(0, 10)
  loadOrders()
}

const openAddDialog = () => {
  dialogTitle.value = 'New Order'
  form.value = createEmptyForm()
  dialogVisible.value = true
}

const editOrder = (row) => {
  const user = currentUser()
  dialogTitle.value = 'Edit Order'
  form.value = {
    id: row.id,
    orderNo: row.orderNo,
    orderTime: String(row.orderTime || '').slice(0, 16),
    customerName: row.customerName || 'Walk-in Customer',
    creatorId: row.creatorId || user.id,
    creatorName: row.creatorName || user.name,
    paymentMethod: row.paymentMethod || 'WeChat Pay',
    discountAmount: row.discountAmount || 0,
    remark: row.remark || '',
    items: (row.items || []).map(item => ({
      productId: item.productId,
      quantity: item.quantity,
      salePrice: item.salePrice
    }))
  }
  if (!form.value.items.length) addItem()
  dialogVisible.value = true
}

const saveOrder = async () => {
  const items = form.value.items.filter(item => item.productId && item.quantity > 0)
  if (!items.length) return ElMessage.warning('Please add at least one product')
  saving.value = true
  try {
    const payload = { ...form.value, orderAmount: orderAmount.value, items }
    if (form.value.id) {
      await request.put(`/orders/${form.value.id}`, payload)
      ElMessage.success('Order updated; inventory and outbound records are synchronized')
    } else {
      await request.post('/orders', payload)
      ElMessage.success('Order saved; sales outbound records have been created')
    }
    dialogVisible.value = false
    await Promise.all([loadOrders(), loadProducts()])
  } finally {
    saving.value = false
  }
}

const deleteOrder = async (id) => {
  await ElMessageBox.confirm('Deleting this order will restore product inventory and remove linked sales outbound records. Continue?', 'Confirm Delete', { type: 'warning' })
  await request.delete(`/orders/${id}`)
  ElMessage.success('Order deleted and inventory restored')
  await Promise.all([loadOrders(), loadProducts()])
}

const addItem = () => {
  form.value.items.push({ productId: null, quantity: 1, salePrice: 0 })
}

const removeItem = (index) => {
  form.value.items.splice(index, 1)
  if (!form.value.items.length) addItem()
}

const onProductChange = (item) => {
  const product = productList.value.find(product => product.id === item.productId)
  item.salePrice = product?.salePrice || 0
}

const lineAmount = (item) => Number(item.quantity || 0) * Number(item.salePrice || 0)
const syncAmount = () => {}

function currentUser() {
  try {
    const profile = JSON.parse(localStorage.getItem('kfc_profile') || '{}')
    return {
      id: profile.id || 1,
      name: profile.displayName || profile.username || 'Store Manager'
    }
  } catch {
    return { id: 1, name: 'Store Manager' }
  }
}

function toLocalDateTimeInput(date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 16)
}

const itemSummary = (items = []) => {
  return items.map(item => `${item.productName || item.productId} x${item.quantity}`).join(', ')
}

const formatTime = (value) => String(value || '').replace('T', ' ').slice(0, 19)

const downloadTemplate = () => {
  window.open('/api/reports/template/orders')
}

const importOrders = async ({ file }) => {
  const formData = new FormData()
  formData.append('file', file)
  const data = await request.post('/reports/import/orders', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  ElMessage.success(`Successfully imported ${data.imported} orders`)
  await Promise.all([loadOrders(), loadProducts()])
}

const exportOrders = () => {
  if (!startDate.value || !endDate.value) return ElMessage.warning('Please select a date range')
  const params = new URLSearchParams({ start: startDate.value, end: endDate.value })
  window.open(`/api/reports/export/orders?${params.toString()}`)
}

onMounted(async () => {
  await loadProducts()
  resetDateRange()
})
</script>

<style scoped>
.order-page {
  max-width: 1450px;
  margin: 0 auto;
}

.toolbar,
.actions,
.search-bar,
.items-header,
.order-total {
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

.actions,
.search-bar {
  flex-wrap: wrap;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(180px, 1fr));
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
}

.summary-card span {
  display: block;
  color: #64748b;
  font-size: 13px;
  margin-bottom: 8px;
}

.summary-card strong {
  color: #111827;
  font-size: 26px;
}

.panel {
  padding: 16px;
}

.pagination-bar {
  padding-top: 14px;
}

.date-input {
  height: 32px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 0 10px;
}

.date-input.full {
  width: 100%;
}

.date-input-group {
  display: grid;
  gap: 6px;
  color: #64748b;
  font-size: 13px;
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

.red-btn.small {
  height: 28px;
  padding: 4px 12px;
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

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(220px, 1fr));
  gap: 0 12px;
}

.items-header {
  justify-content: space-between;
  margin: 8px 0 12px;
}

.item-row {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 130px 110px 110px 70px;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.item-row-head {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 6px;
}

.product-select {
  width: 100%;
}

.unit-price,
.line-amount {
  min-width: 90px;
  color: #111827;
  font-weight: 700;
}

.unit-price {
  color: #475569;
}

.order-total {
  justify-content: flex-end;
  border-top: 1px solid #e5e7eb;
  margin-top: 12px;
  padding-top: 12px;
  color: #475569;
}

.order-total strong {
  color: #e4002b;
  font-size: 18px;
}

@media (max-width: 900px) {
  .toolbar,
  .summary-grid,
  .form-grid,
  .item-row {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
