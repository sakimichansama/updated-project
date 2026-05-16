<template>
  <div class="product-manager">
    <div class="toolbar">
      <div>
        <h2>Product Catalog</h2>
        <span>Manage product price, stock, category, and replenishment thresholds</span>
      </div>
      <div class="actions">
        <el-button class="outline-btn" @click="downloadTemplate">
          <el-icon><Document /></el-icon> Download Template
        </el-button>
        <el-upload :http-request="importProducts" :show-file-list="false" accept=".xlsx,.xls">
          <el-button class="outline-btn">
            <el-icon><Upload /></el-icon> Import Products
          </el-button>
        </el-upload>
        <el-button class="outline-btn" @click="exportProducts">
          <el-icon><Download /></el-icon> Export Current View
        </el-button>
        <el-button class="red-btn" @click="openAddDialog">
          <el-icon><Plus /></el-icon> New Product
        </el-button>
      </div>
    </div>

    <div class="catalog-layout">
      <aside class="category-card">
        <div class="category-header">
          <div>
            <h3>Categories</h3>
            <span>{{ categoryNames.length }} maintained</span>
          </div>
          <el-button class="icon-btn" title="New category" @click="openCategoryDialog()">
            <el-icon><Plus /></el-icon>
          </el-button>
        </div>
        <el-tree
          :data="categoryTree"
          node-key="key"
          default-expand-all
          highlight-current
          :current-node-key="selectedTreeKey"
          :expand-on-click-node="false"
          @node-click="selectCategory"
        >
          <template #default="{ data }">
            <div class="category-node">
              <span class="category-name">{{ data.label }}</span>
              <span class="category-count">{{ data.count }}</span>
              <span v-if="data.category && canMaintainCategory(data.category)" class="category-actions">
                <el-button link title="Rename category" @click.stop="openCategoryDialog(data.category)">
                  <el-icon><EditPen /></el-icon>
                </el-button>
                <el-button link title="Delete category" @click.stop="deleteCategory(data.category)">
                  <el-icon><DeleteIcon /></el-icon>
                </el-button>
              </span>
            </div>
          </template>
        </el-tree>
      </aside>

      <section class="glass-card">
        <div class="list-tools">
          <div>
            <div class="panel-title">Products</div>
            <span class="active-filter">{{ selectedCategoryLabel }} · {{ filteredProducts.length }} items</span>
          </div>
          <el-input v-model="searchText" clearable class="search-input" placeholder="Search products" @input="currentPage = 1" @clear="currentPage = 1" />
        </div>
        <el-table :data="pagedProducts" stripe class="modern-table" :row-class-name="tableRowClassName">
          <el-table-column prop="name" label="Name" min-width="160" />
          <el-table-column prop="specification" label="Specification" min-width="130" />
          <el-table-column prop="category" label="Category" min-width="130">
            <template #default="{ row }">{{ normalizeCategory(row.category) }}</template>
          </el-table-column>
          <el-table-column prop="unit" label="Unit" min-width="90" />
          <el-table-column prop="purchasePrice" label="Purchase Price" min-width="130">
            <template #default="{ row }">¥{{ row.purchasePrice }}</template>
          </el-table-column>
          <el-table-column prop="salePrice" label="Sale Price" min-width="120">
            <template #default="{ row }">¥{{ row.salePrice }}</template>
          </el-table-column>
          <el-table-column prop="stock" label="Stock" min-width="100" />
          <el-table-column prop="minStock" label="Min Stock" min-width="110" />
          <el-table-column label="Actions" width="160" fixed="right">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button class="action-btn edit-btn" @click="editProduct(row)">Edit</el-button>
                <el-button class="action-btn delete-btn" @click="deleteProduct(row.id)">Delete</el-button>
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
            :total="filteredProducts.length"
          />
        </div>
      </section>
    </div>

    <el-dialog v-model="categoryDialogVisible" :title="categoryDialogTitle" width="420px" class="modern-dialog">
      <el-form label-width="110px" class="dialog-form">
        <el-form-item label="Category">
          <el-input v-model="categoryForm.name" class="modern-input" placeholder="Category name" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="outline-btn" @click="categoryDialogVisible = false">Cancel</el-button>
        <el-button class="red-btn" @click="saveCategory">Save</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" class="modern-dialog">
      <el-form :model="form" label-width="120px" class="dialog-form">
        <el-form-item label="Name">
          <el-input v-model="form.name" class="modern-input" placeholder="Product name" />
        </el-form-item>
        <el-form-item label="Specification">
          <el-input v-model="form.specification" class="modern-input" placeholder="e.g. 500ml" />
        </el-form-item>
        <el-form-item label="Category">
          <el-select
            v-model="form.category"
            filterable
            allow-create
            default-first-option
            class="category-select"
            placeholder="Select or create category"
          >
            <el-option v-for="item in categoryNames" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="Unit">
          <el-input v-model="form.unit" class="modern-input" placeholder="bottle, bag, box" />
        </el-form-item>
        <el-form-item label="Purchase Price">
          <el-input-number v-model="form.purchasePrice" :min="0" :precision="2" controls-position="right" class="modern-number" />
        </el-form-item>
        <el-form-item label="Sale Price">
          <el-input-number v-model="form.salePrice" :min="0" :precision="2" controls-position="right" class="modern-number" />
        </el-form-item>
        <el-form-item label="Min Stock">
          <el-input-number v-model="form.minStock" :min="0" controls-position="right" class="modern-number" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false" class="outline-btn">Cancel</el-button>
        <el-button class="red-btn" @click="saveProduct">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import request from '@/utils/request'
import { DEFAULT_PAGE_SIZE, PAGE_SIZES, filterRows, pageRows } from '@/utils/listTools'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete as DeleteIcon, Document, Download, EditPen, Plus, Upload } from '@element-plus/icons-vue'

const ALL_CATEGORY_KEY = '__all__'
const DEFAULT_CATEGORY = 'Uncategorized'
const STORAGE_KEY = 'kfc_product_categories'
const productList = ref([])
const searchText = ref('')
const currentPage = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const dialogVisible = ref(false)
const dialogTitle = ref('New Product')
const form = ref({})
const selectedCategory = ref('')
const localCategories = ref(loadLocalCategories())
const categoryDialogVisible = ref(false)
const editingCategory = ref('')
const categoryForm = ref({ name: '' })
const categoryDialogTitle = computed(() => editingCategory.value ? 'Rename Category' : 'New Category')
const selectedTreeKey = computed(() => selectedCategory.value ? `category:${selectedCategory.value}` : ALL_CATEGORY_KEY)
const selectedCategoryLabel = computed(() => selectedCategory.value || 'All Products')
const categoryStats = computed(() => {
  const stats = new Map()
  productList.value.forEach(product => {
    const category = normalizeCategory(product.category)
    stats.set(category, (stats.get(category) || 0) + 1)
  })
  localCategories.value.forEach(category => {
    if (!stats.has(category)) stats.set(category, 0)
  })
  return stats
})
const categoryNames = computed(() => [...categoryStats.value.keys()].sort((a, b) => a.localeCompare(b)))
const categoryTree = computed(() => [
  {
    key: ALL_CATEGORY_KEY,
    label: 'All Products',
    count: productList.value.length,
    children: categoryNames.value.map(category => ({
      key: `category:${category}`,
      label: category,
      category,
      count: categoryStats.value.get(category) || 0
    }))
  }
])
const filteredProducts = computed(() => {
  const categoryRows = selectedCategory.value
    ? productList.value.filter(product => normalizeCategory(product.category) === selectedCategory.value)
    : productList.value
  return filterRows(categoryRows, searchText.value)
})
const pagedProducts = computed(() => pageRows(filteredProducts.value, currentPage.value, pageSize.value))

function loadLocalCategories() {
  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
    return Array.isArray(parsed) ? parsed.map(normalizeCategory).filter(Boolean) : []
  } catch {
    return []
  }
}

const saveLocalCategories = () => {
  const unique = [...new Set(localCategories.value.map(normalizeCategory).filter(Boolean))]
  localCategories.value = unique
  localStorage.setItem(STORAGE_KEY, JSON.stringify(unique))
}

function normalizeCategory(value) {
  const text = String(value || '').trim()
  return text || DEFAULT_CATEGORY
}

const canMaintainCategory = category => normalizeCategory(category) !== DEFAULT_CATEGORY

const selectCategory = data => {
  selectedCategory.value = data.category || ''
  currentPage.value = 1
}

const loadProducts = async () => {
  const data = await request.get('/products')
  productList.value = data || []
}

const openAddDialog = () => {
  dialogTitle.value = 'New Product'
  form.value = {
    category: selectedCategory.value || '',
    stock: 0,
    minStock: 0,
    purchasePrice: 0,
    salePrice: 0
  }
  dialogVisible.value = true
}

const editProduct = (row) => {
  dialogTitle.value = 'Edit Product'
  form.value = { ...row }
  dialogVisible.value = true
}

const saveProduct = async () => {
  form.value.category = normalizeCategory(form.value.category)
  if (form.value.id) {
    await request.put(`/products/${form.value.id}`, form.value)
    ElMessage.success('Product updated')
  } else {
    await request.post('/products', form.value)
    ElMessage.success('Product added')
  }
  if (!localCategories.value.includes(form.value.category)) {
    localCategories.value.push(form.value.category)
    saveLocalCategories()
  }
  dialogVisible.value = false
  loadProducts()
}

const openCategoryDialog = (category = '') => {
  editingCategory.value = category
  categoryForm.value = { name: category }
  categoryDialogVisible.value = true
}

const saveCategory = async () => {
  const nextName = normalizeCategory(categoryForm.value.name)
  const previousName = editingCategory.value ? normalizeCategory(editingCategory.value) : ''
  if (nextName === DEFAULT_CATEGORY) {
    return ElMessage.warning('Please enter a category name')
  }
  if (categoryNames.value.includes(nextName) && nextName !== previousName) {
    return ElMessage.warning('Category already exists')
  }
  if (!previousName) {
    localCategories.value.push(nextName)
    selectedCategory.value = nextName
    saveLocalCategories()
    categoryDialogVisible.value = false
    currentPage.value = 1
    return ElMessage.success('Category added')
  }

  const changedProducts = productList.value.filter(product => normalizeCategory(product.category) === previousName)
  await Promise.all(changedProducts.map(product => request.put(`/products/${product.id}`, { ...product, category: nextName })))
  localCategories.value = localCategories.value
    .filter(category => normalizeCategory(category) !== previousName)
    .concat(nextName)
  selectedCategory.value = selectedCategory.value === previousName ? nextName : selectedCategory.value
  saveLocalCategories()
  categoryDialogVisible.value = false
  await loadProducts()
  ElMessage.success('Category renamed')
}

const deleteCategory = async (category) => {
  const targetCategory = normalizeCategory(category)
  const affectedProducts = productList.value.filter(product => normalizeCategory(product.category) === targetCategory)
  const message = affectedProducts.length
    ? `Delete ${targetCategory}? ${affectedProducts.length} products will be moved to ${DEFAULT_CATEGORY}.`
    : `Delete empty category ${targetCategory}?`
  await ElMessageBox.confirm(message, 'Confirm Delete', { type: 'warning' })
  await Promise.all(affectedProducts.map(product => request.put(`/products/${product.id}`, { ...product, category: DEFAULT_CATEGORY })))
  localCategories.value = localCategories.value.filter(item => normalizeCategory(item) !== targetCategory)
  if (selectedCategory.value === targetCategory) {
    selectedCategory.value = ''
  }
  saveLocalCategories()
  await loadProducts()
  ElMessage.success('Category deleted')
}

const importProducts = async ({ file }) => {
  const formData = new FormData()
  formData.append('file', file)
  const data = await request.post('/reports/import/products', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  ElMessage.success(`Successfully imported ${data.imported} products; category tree refreshed`)
  await loadProducts()
}

const downloadTemplate = () => {
  window.open('/api/reports/template/products')
}

const exportProducts = () => {
  const rows = filteredProducts.value
  if (!rows.length) return ElMessage.warning('No products to export')
  const headers = ['ID', 'Name', 'Specification', 'Category', 'Unit', 'Purchase Price', 'Sale Price', 'Stock', 'Min Stock']
  const csvRows = [
    headers,
    ...rows.map(product => [
      product.id || '',
      product.name || '',
      product.specification || '',
      normalizeCategory(product.category),
      product.unit || '',
      product.purchasePrice || 0,
      product.salePrice || 0,
      product.stock || 0,
      product.minStock || 0
    ])
  ]
  const csv = csvRows.map(row => row.map(escapeCsvValue).join(',')).join('\r\n')
  const blob = new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  const suffix = selectedCategory.value ? selectedCategory.value.replace(/[\\/:*?"<>|]/g, '-') : 'all'
  link.href = url
  link.download = `products-${suffix}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

const escapeCsvValue = value => {
  const text = String(value ?? '')
  if (/[",\r\n]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

const deleteProduct = (id) => {
  ElMessageBox.confirm('Delete this product?', 'Confirm Delete', { type: 'warning' }).then(async () => {
    await request.delete(`/products/${id}`)
    ElMessage.success('Product deleted')
    loadProducts()
  })
}

const tableRowClassName = ({ rowIndex }) => {
  return rowIndex % 2 === 0 ? 'even-row' : 'odd-row'
}

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
.product-manager {
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

.catalog-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.glass-card,
.category-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.glass-card {
  overflow: hidden;
  min-width: 0;
}

.category-card {
  padding: 16px;
  position: sticky;
  top: 0;
}

.category-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.category-header h3 {
  margin: 0 0 4px;
  color: #111827;
  font-size: 16px;
  line-height: 1.2;
}

.category-header span,
.active-filter {
  color: #94a3b8;
  font-size: 12px;
}

.icon-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 8px;
  color: #e4002b;
  border-color: #fecdd3;
}

.category-node {
  width: 100%;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  line-height: 1.4;
}

.category-name {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-count {
  flex: 0 0 auto;
  min-width: 28px;
  padding: 1px 7px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  text-align: center;
}

.category-actions {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 2px;
}

.category-actions .el-button {
  padding: 0 2px;
  color: #64748b;
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.panel-title {
  color: #1f2937;
  font-weight: 700;
  margin-bottom: 4px;
}

.modern-table {
  margin: 12px 24px 12px 24px;
  width: calc(100% - 48px);
  border-radius: 20px;
  overflow: hidden;
}

:deep(.modern-table .cell) {
  white-space: normal;
  word-break: normal;
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
.modern-number :deep(.el-input-number__input) {
  border-radius: 20px;
}

.category-select {
  width: 100%;
}

.category-select :deep(.el-select__wrapper) {
  border-radius: 20px;
  min-height: 38px;
}

.dialog-form {
  margin-top: 8px;
}

@media (max-width: 900px) {
  .toolbar,
  .catalog-layout {
    display: grid;
    grid-template-columns: 1fr;
  }

  .actions {
    justify-content: flex-start;
  }

  .category-card {
    position: static;
  }

  .modern-table {
    margin: 12px 12px;
    width: calc(100% - 24px);
  }

  .list-tools,
  .pagination-bar {
    padding-left: 12px;
    padding-right: 12px;
  }
}
</style>
