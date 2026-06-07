<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>Product Margin Analysis</h2>
        <span>Rank product and category profitability from maintained order items</span>
      </div>
      <div class="actions">
        <input v-model="startDate" type="date" class="date-input" />
        <input v-model="endDate" type="date" class="date-input" />
        <el-button class="red-btn" @click="loadReport">Analyze</el-button>
        <el-button class="outline-btn" @click="resetLast30Days">Last 30 Days</el-button>
      </div>
    </div>

    <div class="summary-grid">
      <div class="summary-card" v-for="item in kpiCards" :key="item.label">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.sub }}</small>
      </div>
    </div>

    <div class="grid">
      <section class="panel chart-panel wide">
        <div class="panel-title">
          <div>Top Product Gross Profit Ranking</div>
          <small>Sorted by gross profit in the selected date range</small>
        </div>
        <div ref="productChartRef" class="chart"></div>
      </section>

      <section class="panel chart-panel">
        <div class="panel-title">
          <div>Category Gross Profit Ranking</div>
          <small>Compare category profit contribution and margin level</small>
        </div>
        <div ref="categoryChartRef" class="chart"></div>
      </section>
    </div>

    <section class="panel">
      <div class="list-tools">
        <div class="panel-title">Product Margin Ranking</div>
        <el-input v-model="productSearch" clearable class="search-input" placeholder="Search products" @input="productPage = 1" @clear="productPage = 1" />
      </div>
      <el-table :data="pagedProductRows" stripe class="adaptive-table">
        <el-table-column prop="rank" label="Rank" width="80" />
        <el-table-column prop="productName" label="Product" min-width="160" />
        <el-table-column prop="category" label="Category" min-width="120" />
        <el-table-column prop="quantity" label="Qty" width="90" />
        <el-table-column prop="salesAmount" label="Sales" min-width="130">
          <template #default="{ row }">{{ money(row.salesAmount) }}</template>
        </el-table-column>
        <el-table-column prop="costAmount" label="Cost" min-width="130">
          <template #default="{ row }">{{ money(row.costAmount) }}</template>
        </el-table-column>
        <el-table-column prop="grossProfit" label="Gross Profit" min-width="140">
          <template #default="{ row }">
            <span :class="['profit-value', profitClass(row.grossProfit)]">{{ money(row.grossProfit) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="grossMargin" label="Gross Margin" min-width="130">
          <template #default="{ row }">
            <el-tag :type="marginTag(row.grossMargin)">{{ percent(row.grossMargin) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="productPage"
          v-model:page-size="productPageSize"
          background
          layout="total, sizes, prev, pager, next"
          :page-sizes="PAGE_SIZES"
          :total="filteredProductRows.length"
        />
      </div>
    </section>

    <section class="panel">
      <div class="list-tools">
        <div class="panel-title">Category Margin Ranking</div>
        <el-input v-model="categorySearch" clearable class="search-input" placeholder="Search categories" @input="categoryPage = 1" @clear="categoryPage = 1" />
      </div>
      <el-table :data="pagedCategoryRows" stripe class="adaptive-table">
        <el-table-column prop="rank" label="Rank" width="80" />
        <el-table-column prop="category" label="Category" min-width="160" />
        <el-table-column prop="quantity" label="Qty" width="90" />
        <el-table-column prop="salesAmount" label="Sales" min-width="130">
          <template #default="{ row }">{{ money(row.salesAmount) }}</template>
        </el-table-column>
        <el-table-column prop="costAmount" label="Cost" min-width="130">
          <template #default="{ row }">{{ money(row.costAmount) }}</template>
        </el-table-column>
        <el-table-column prop="grossProfit" label="Gross Profit" min-width="140">
          <template #default="{ row }">
            <span :class="['profit-value', profitClass(row.grossProfit)]">{{ money(row.grossProfit) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="grossMargin" label="Gross Margin" min-width="130">
          <template #default="{ row }">
            <el-tag :type="marginTag(row.grossMargin)">{{ percent(row.grossMargin) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="categoryPage"
          v-model:page-size="categoryPageSize"
          background
          layout="total, sizes, prev, pager, next"
          :page-sizes="PAGE_SIZES"
          :total="filteredCategoryRows.length"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'
import { DEFAULT_PAGE_SIZE, PAGE_SIZES, filterRows, pageRows } from '@/utils/listTools'

const startDate = ref('')
const endDate = ref('')
const summary = ref({})
const productList = ref([])
const categoryList = ref([])
const productSearch = ref('')
const categorySearch = ref('')
const productPage = ref(1)
const categoryPage = ref(1)
const productPageSize = ref(DEFAULT_PAGE_SIZE)
const categoryPageSize = ref(DEFAULT_PAGE_SIZE)
const productChartRef = ref(null)
const categoryChartRef = ref(null)
let productChart = null
let categoryChart = null

const toNumber = value => Number(value || 0)
const money = value => `¥${toNumber(value).toLocaleString(undefined, { maximumFractionDigits: 2 })}`
const percent = value => `${toNumber(value).toLocaleString(undefined, { maximumFractionDigits: 2 })}%`
const profitClass = value => toNumber(value) < 0 ? 'negative' : toNumber(value) > 0 ? 'positive' : 'neutral'
const marginTag = value => {
  const margin = toNumber(value)
  if (margin >= 40) return 'success'
  if (margin >= 20) return 'warning'
  return 'danger'
}

const productRows = computed(() => productList.value.map((item, index) => ({ ...item, rank: index + 1 })))
const categoryRows = computed(() => categoryList.value.map((item, index) => ({ ...item, rank: index + 1 })))
const filteredProductRows = computed(() => filterRows(productRows.value, productSearch.value))
const filteredCategoryRows = computed(() => filterRows(categoryRows.value, categorySearch.value))
const pagedProductRows = computed(() => pageRows(filteredProductRows.value, productPage.value, productPageSize.value))
const pagedCategoryRows = computed(() => pageRows(filteredCategoryRows.value, categoryPage.value, categoryPageSize.value))
const kpiCards = computed(() => [
  { label: 'Total Sales', value: money(summary.value.totalSales), sub: `${productList.value.length} maintained products` },
  { label: 'Total Cost', value: money(summary.value.totalCost), sub: 'Order item cost amount' },
  { label: 'Gross Profit', value: money(summary.value.grossProfit), sub: 'Sales minus item cost' },
  { label: 'Gross Margin', value: percent(summary.value.grossMargin), sub: 'Gross profit / sales' },
  { label: 'Top Product', value: summary.value.topProduct || '-', sub: 'Highest gross profit product' },
  { label: 'Top Category', value: summary.value.topCategory || '-', sub: `${summary.value.categoryCount || 0} categories ranked` }
])

const loadReport = async () => {
  if (!startDate.value || !endDate.value) return
  const data = await request.get('/profit/product-margin', { params: { start: startDate.value, end: endDate.value } })
  summary.value = data.summary || {}
  productList.value = data.productList || []
  categoryList.value = data.categoryList || []
  productPage.value = 1
  categoryPage.value = 1
  await nextTick()
  renderCharts()
}

const renderCharts = () => {
  productChart?.dispose()
  categoryChart?.dispose()
  if (!productChartRef.value || !categoryChartRef.value) return

  const productRows = productList.value.slice(0, 10).reverse()
  productChart = echarts.init(productChartRef.value)
  productChart.setOption({
    color: ['#e4002b', '#2563eb'],
    graphic: productRows.length ? [] : emptyGraphic('No product margin data in this range'),
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: params => {
        const row = productRows[params[0]?.dataIndex] || {}
        return [
          row.productName || '',
          `Sales: ${money(row.salesAmount)}`,
          `Cost: ${money(row.costAmount)}`,
          `Gross Profit: ${money(row.grossProfit)}`,
          `Gross Margin: ${percent(row.grossMargin)}`
        ].join('<br/>')
      }
    },
    legend: { top: 0, textStyle: { color: '#475569' } },
    grid: { left: 120, right: 34, top: 44, bottom: 32, containLabel: true },
    xAxis: {
      type: 'value',
      axisLabel: { color: '#64748b', formatter: value => `¥${value}` },
      splitLine: { lineStyle: { color: '#eef2f7' } }
    },
    yAxis: {
      type: 'category',
      data: productRows.map(item => item.productName),
      axisLabel: { color: '#64748b' },
      axisLine: { lineStyle: { color: '#cbd5e1' } }
    },
    series: [
      {
        name: 'Gross Profit',
        type: 'bar',
        barWidth: 14,
        data: productRows.map(item => toNumber(item.grossProfit)),
        itemStyle: { borderRadius: [0, 5, 5, 0] }
      },
      {
        name: 'Sales',
        type: 'bar',
        barWidth: 14,
        data: productRows.map(item => toNumber(item.salesAmount)),
        itemStyle: { borderRadius: [0, 5, 5, 0], opacity: 0.42 }
      }
    ]
  })

  const categoryRows = categoryList.value.slice(0, 8)
  categoryChart = echarts.init(categoryChartRef.value)
  categoryChart.setOption({
    color: ['#e4002b', '#2563eb'],
    graphic: categoryRows.length ? [] : emptyGraphic('No category margin data in this range'),
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: params => {
        const row = categoryRows[params[0]?.dataIndex] || {}
        return [
          row.category || '',
          `Gross Profit: ${money(row.grossProfit)}`,
          `Gross Margin: ${percent(row.grossMargin)}`
        ].join('<br/>')
      }
    },
    legend: { top: 0, textStyle: { color: '#475569' } },
    grid: { left: 44, right: 48, top: 48, bottom: 64, containLabel: true },
    xAxis: {
      type: 'category',
      data: categoryRows.map(item => item.category),
      axisLabel: { color: '#64748b', rotate: 25 },
      axisLine: { lineStyle: { color: '#cbd5e1' } }
    },
    yAxis: [
      {
        type: 'value',
        name: 'Profit',
        axisLabel: { color: '#64748b', formatter: value => `¥${value}` },
        splitLine: { lineStyle: { color: '#eef2f7' } }
      },
      {
        type: 'value',
        name: 'Margin',
        axisLabel: { color: '#64748b', formatter: value => `${value}%` },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: 'Gross Profit',
        type: 'bar',
        barWidth: 18,
        data: categoryRows.map(item => toNumber(item.grossProfit)),
        itemStyle: { borderRadius: [5, 5, 0, 0] }
      },
      {
        name: 'Gross Margin',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbolSize: 7,
        data: categoryRows.map(item => toNumber(item.grossMargin)),
        lineStyle: { width: 3 }
      }
    ]
  })
}

const emptyGraphic = text => [{
  type: 'text',
  left: 'center',
  top: 'middle',
  style: { text, fill: '#94a3b8', fontSize: 14 }
}]

const resetLast30Days = () => {
  const end = new Date()
  const start = new Date(end.getTime() - 29 * 24 * 3600 * 1000)
  startDate.value = start.toISOString().slice(0, 10)
  endDate.value = end.toISOString().slice(0, 10)
  loadReport()
}

const resize = () => {
  productChart?.resize()
  categoryChart?.resize()
}

onMounted(() => {
  resetLast30Days()
  window.addEventListener('resize', resize)
})

onUnmounted(() => {
  productChart?.dispose()
  categoryChart?.dispose()
  window.removeEventListener('resize', resize)
})
</script>

<style scoped>
.page {
  max-width: 1400px;
  margin: 0 auto;
}

.toolbar,
.actions,
.list-tools {
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

.toolbar span,
.summary-card small,
.panel-title small {
  color: #64748b;
  font-size: 13px;
}

.actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.date-input {
  height: 32px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 0 10px;
  background: #fff;
  color: #111827;
  font: inherit;
  outline: none;
}

.date-input:focus {
  border-color: #e4002b;
  box-shadow: 0 0 0 3px rgba(228, 0, 43, 0.12);
}

.red-btn {
  background: #e4002b;
  border-color: #e4002b;
  color: #fff;
  font-weight: 700;
  border-radius: 6px;
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
  border-radius: 6px;
}

.outline-btn:hover,
.outline-btn:focus {
  border-color: #c80025;
  color: #c80025;
  background: #fff5f7;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.summary-card,
.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.summary-card {
  padding: 18px;
  display: grid;
  gap: 8px;
  min-height: 96px;
}

.summary-card span {
  color: #64748b;
  font-size: 13px;
}

.summary-card strong {
  font-size: 24px;
  color: #111827;
  line-height: 1.18;
}

.grid {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.wide {
  grid-column: 1 / -1;
}

.panel {
  padding: 16px;
  margin-bottom: 16px;
}

.panel-title {
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 12px;
}

.panel-title small {
  display: block;
  margin-top: 4px;
  font-weight: 500;
}

.list-tools {
  justify-content: space-between;
  margin-bottom: 12px;
}

.list-tools .panel-title {
  margin-bottom: 0;
}

.search-input {
  width: 260px;
}

.adaptive-table {
  width: 100%;
}

:deep(.adaptive-table .cell) {
  white-space: normal;
  word-break: normal;
}

.profit-value {
  font-weight: 700;
}

.profit-value.positive {
  color: #059669;
}

.profit-value.negative {
  color: #dc2626;
}

.profit-value.neutral {
  color: #64748b;
}

.chart {
  height: 360px;
}

.wide .chart {
  min-height: 390px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
}

@media (max-width: 900px) {
  .toolbar,
  .summary-grid,
  .grid,
  .list-tools {
    display: grid;
    grid-template-columns: 1fr;
  }

  .search-input {
    width: 100%;
  }
}
</style>
