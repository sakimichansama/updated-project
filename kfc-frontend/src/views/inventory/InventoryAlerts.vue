<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>Inventory Alerts and Smart Replenishment</h2>
        <span>Sorted by inventory risk; prioritize products below safety stock</span>
      </div>
      <div class="actions">
        <el-input v-model="searchText" clearable class="search-input" placeholder="Search alerts" @input="currentPage = 1" @clear="currentPage = 1" />
        <el-button class="outline-btn" @click="loadData">Refresh</el-button>
        <el-button class="red-btn" @click="exportExcel">Export Excel</el-button>
      </div>
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <span>WarningProduct</span>
        <strong>{{ alertCount }}</strong>
      </div>
      <div class="summary-card">
        <span>Suggested Replenishment</span>
        <strong>{{ totalSuggested }}</strong>
      </div>
      <div class="summary-card">
        <span>Estimated Purchase Cost</span>
        <strong>¥{{ totalCost }}</strong>
      </div>
    </div>

    <div class="panel">
      <el-table :data="pagedSuggestions" stripe>
        <el-table-column prop="productName" label="Product" min-width="150" />
        <el-table-column prop="category" label="Category" min-width="110" />
        <el-table-column prop="stock" label="Current Stock" width="100" />
        <el-table-column prop="minStock" label="Min Stock" width="100" />
        <el-table-column label="Risk" width="110">
          <template #default="{ row }">
            <el-tag :type="row.level === 'critical' ? 'danger' : row.level === 'warning' ? 'warning' : 'success'">
              {{ levelText(row.level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="suggestedQuantity" label="Suggested Qty" width="110" />
        <el-table-column prop="estimatedCost" label="Estimated Cost" width="120">
          <template #default="{ row }">¥{{ row.estimatedCost }}</template>
        </el-table-column>
        <el-table-column prop="suggestion" label="Advice" min-width="220" />
        <el-table-column label="Actions" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!row.suggestedQuantity" @click="quickReplenish(row)">
              One-click Inbound
            </el-button>
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
          :total="filteredSuggestions.length"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '@/utils/request'
import { DEFAULT_PAGE_SIZE, PAGE_SIZES, filterRows, pageRows } from '@/utils/listTools'
import { ElMessage, ElMessageBox } from 'element-plus'

const suggestions = ref([])
const searchText = ref('')
const currentPage = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)

const alertCount = computed(() => suggestions.value.filter(item => item.level !== 'normal').length)
const totalSuggested = computed(() => suggestions.value.reduce((sum, item) => sum + (item.suggestedQuantity || 0), 0))
const totalCost = computed(() => suggestions.value.reduce((sum, item) => sum + (item.estimatedCost || 0), 0).toFixed(2))
const filteredSuggestions = computed(() => filterRows(suggestions.value, searchText.value, row => levelText(row.level)))
const pagedSuggestions = computed(() => pageRows(filteredSuggestions.value, currentPage.value, pageSize.value))

const loadData = async () => {
  suggestions.value = await request.get('/inventory/replenishment-suggestions')
}

const levelText = (level) => {
  const map = { critical: 'Critical', warning: 'Warning', normal: 'Normal' }
  return map[level] || level
}

const quickReplenish = async (row) => {
  await ElMessageBox.confirm(`Confirm replenishing ${row.productName} inbound quantity ${row.suggestedQuantity}${row.unit || ''}?`, 'Smart Replenishment')
  const price = row.suggestedQuantity ? row.estimatedCost / row.suggestedQuantity : 0
  await request.post('/inventory/in', null, {
    params: {
      productId: row.productId,
      quantity: row.suggestedQuantity,
      price,
      supplier: 'Smart Replenishment Advice'
    }
  })
  ElMessage.success('Inbound completed')
  loadData()
}

const exportExcel = () => {
  window.open('/api/reports/export/inventory-alerts')
}

onMounted(loadData)
</script>

<style scoped>
.page {
  max-width: 1400px;
  margin: 0 auto;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
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
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
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
  padding: 14px;
}

.pagination-bar {
  padding-top: 14px;
}

@media (max-width: 800px) {
  .toolbar,
  .summary-grid {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
