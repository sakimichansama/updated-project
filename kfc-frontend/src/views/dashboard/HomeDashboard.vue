<template>
  <div class="dashboard-page">
    <div class="toolbar">
      <div>
        <h2>Operations Dashboard</h2>
        <span>{{ month }} Store operations overview</span>
      </div>
      <el-date-picker v-model="month" type="month" value-format="YYYY-MM" @change="loadDashboard" />
    </div>

    <div class="kpi-grid">
      <div class="kpi-card" v-for="item in kpiCards" :key="item.label" :style="{ '--accent': item.accent }">
        <div class="kpi-label">{{ item.label }}</div>
        <div class="kpi-value">{{ item.value }}</div>
        <div class="kpi-sub">{{ item.sub }}</div>
      </div>
    </div>

    <div class="content-grid">
      <section class="panel wide">
        <div class="panel-title">
          <div>
            <span>Sales Trend</span>
            <small>Source: monthly sales daily records</small>
          </div>
          <el-tag type="success">Orders {{ kpis.totalOrders || 0 }}</el-tag>
        </div>
        <div ref="salesChartRef" class="chart"></div>
      </section>

      <section class="panel">
        <div class="panel-title">
          <div>
            <span>Monthly Profit</span>
            <small>Source: profit monthly report</small>
          </div>
          <el-tag type="info">Net {{ money(kpis.netProfit) }}</el-tag>
        </div>
        <div ref="profitChartRef" class="chart"></div>
      </section>

      <section class="panel">
        <div class="panel-title">
          <div>
            <span>Cost Structure</span>
            <small>Source: profit monthly report</small>
          </div>
          <el-tag>Net Margin {{ kpis.profitMargin || 0 }}%</el-tag>
        </div>
        <div ref="costChartRef" class="chart"></div>
      </section>

      <section class="panel">
        <div class="panel-title">
          <div>
            <span>Employee Performance</span>
            <small>Source: employee performance monthly report</small>
          </div>
          <el-tag type="warning">{{ kpis.avgEmployeeScore || 0 }} Avg</el-tag>
        </div>
        <div ref="employeeChartRef" class="chart"></div>
      </section>

      <section class="panel">
        <div class="panel-title">
          <div>
            <span>Inventory Risk</span>
            <small>Source: replenishment suggestions</small>
          </div>
          <el-tag type="danger">{{ inventoryAlerts.length }} Alerts</el-tag>
        </div>
        <div ref="inventoryChartRef" class="chart"></div>
      </section>

      <section class="panel waste-panel">
        <div class="panel-title">
          <div>
            <span>Waste Closure</span>
            <small>Source: monthly waste statistics</small>
          </div>
          <el-tag>{{ wasteStats.closureRate || 0 }}%</el-tag>
        </div>
        <div ref="wasteChartRef" class="chart compact-chart"></div>
        <div class="closure">
          <div class="closure-stats">
            <p>Pending: {{ wasteStats.pendingCount || 0 }}</p>
            <p>Waste Cost: {{ money(wasteStats.totalCost) }}</p>
          </div>
        </div>
      </section>

      <section class="panel wide">
        <div class="panel-title">
          <span>Inventory Alerts</span>
          <el-button link type="primary" @click="$router.push('/inventory/alerts')">View Advice</el-button>
        </div>
        <div class="list-tools">
          <el-input v-model="inventorySearchText" clearable class="search-input" placeholder="Search inventory alerts" @input="inventoryPage = 1" @clear="inventoryPage = 1" />
        </div>
        <el-table :data="pagedInventoryAlerts" height="260">
          <el-table-column prop="productName" label="Product" min-width="140" />
          <el-table-column prop="stock" label="Stock" width="90" />
          <el-table-column prop="minStock" label="Min Stock" width="100" />
          <el-table-column prop="suggestedQuantity" label="Suggested Qty" width="110" />
          <el-table-column prop="estimatedCost" label="Estimated Cost" width="110">
            <template #default="{ row }">{{ money(row.estimatedCost) }}</template>
          </el-table-column>
          <el-table-column prop="suggestion" label="Advice" min-width="180" />
        </el-table>
        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="inventoryPage"
            v-model:page-size="inventoryPageSize"
            background
            layout="total, prev, pager, next"
            :total="filteredInventoryAlerts.length"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'
import { DEFAULT_PAGE_SIZE, filterRows, pageRows } from '@/utils/listTools'

const month = ref('')
const kpis = ref({})
const inventoryAlerts = ref([])
const inventorySearchText = ref('')
const inventoryPage = ref(1)
const inventoryPageSize = ref(DEFAULT_PAGE_SIZE)
const wasteStats = ref({})
const salesChartRef = ref(null)
const profitChartRef = ref(null)
const costChartRef = ref(null)
const employeeChartRef = ref(null)
const inventoryChartRef = ref(null)
const wasteChartRef = ref(null)
let salesChart = null
let profitChart = null
let costChart = null
let employeeChart = null
let inventoryChart = null
let wasteChart = null

const toNumber = value => Number(value ?? 0) || 0
const money = value => `¥${toNumber(value).toLocaleString(undefined, { maximumFractionDigits: 2 })}`
const kpiCards = computed(() => [
  { label: 'Sales', value: money(kpis.value.totalSales), sub: `Daily avg ${money(kpis.value.avgDailySales)}`, accent: '#e4002b' },
  { label: 'Net Profit', value: money(kpis.value.netProfit), sub: `Net margin ${kpis.value.profitMargin || 0}%`, accent: '#10b981' },
  { label: 'Inventory Alerts', value: kpis.value.lowStockCount || 0, sub: `Inventory value ${money(kpis.value.stockValue)}`, accent: '#f97316' },
  { label: 'Employee Performance', value: kpis.value.avgEmployeeScore || 0, sub: `${kpis.value.employeeCount || 0} employees`, accent: '#2563eb' }
])
const filteredInventoryAlerts = computed(() => filterRows(inventoryAlerts.value, inventorySearchText.value))
const pagedInventoryAlerts = computed(() => pageRows(filteredInventoryAlerts.value, inventoryPage.value, inventoryPageSize.value))
const emptyGraphic = (hasData, text) => hasData ? [] : [{
  type: 'text',
  left: 'center',
  top: 'middle',
  style: { text, fill: '#94a3b8', fontSize: 14 }
}]

const loadDashboard = async () => {
  const data = await request.get('/dashboard/overview', { params: { month: month.value } })
  kpis.value = data.kpis || {}
  inventoryAlerts.value = data.inventoryAlerts || []
  wasteStats.value = data.wasteStats || {}
  await nextTick()
  renderSalesChart(data.salesTrend || [])
  renderProfitChart(data.profitDailyList || [])
  renderCostChart(data.costBreakdown || {})
  renderEmployeeChart(data.employeePerformance || [])
  renderInventoryChart(data.inventoryAlerts || [])
  renderWasteChart(data.wasteStats || {})
}

const renderSalesChart = (rows) => {
  salesChart?.dispose()
  if (!salesChartRef.value) return
  salesChart = echarts.init(salesChartRef.value)
  const dates = rows.map(item => item.date)
  const salesValues = rows.map(item => toNumber(item.totalAmount))
  const orderValues = rows.map(item => toNumber(item.orderCount ?? item.orders ?? item.totalOrders))
  const hasOrders = orderValues.some(value => value > 0)
  salesChart.setOption({
    color: ['#e4002b', '#2563eb'],
    graphic: emptyGraphic(rows.length, 'No maintained sales records for this month'),
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: params => {
        const title = params[0]?.axisValue || ''
        const lines = params.map(item => {
          const value = item.seriesName === 'Orders' ? item.value : `¥${Number(item.value || 0).toLocaleString()}`
          return `${item.marker}${item.seriesName}: ${value}`
        })
        return [title, ...lines].join('<br/>')
      }
    },
    legend: { top: 0, textStyle: { color: '#475569' } },
    grid: { left: 42, right: hasOrders ? 52 : 22, top: 44, bottom: 36, containLabel: true },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false,
      axisLabel: { rotate: 25, color: '#64748b' },
      axisLine: { lineStyle: { color: '#cbd5e1' } }
    },
    yAxis: [
      {
        type: 'value',
        name: 'Sales',
        axisLabel: { color: '#64748b', formatter: value => `¥${value}` },
        splitLine: { lineStyle: { color: '#eef2f7' } }
      },
      {
        type: 'value',
        name: 'Orders',
        show: hasOrders,
        axisLabel: { color: '#64748b' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: 'Sales',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: salesValues,
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(228, 0, 43, 0.22)' },
            { offset: 1, color: 'rgba(228, 0, 43, 0.02)' }
          ])
        },
        markLine: {
          symbol: 'none',
          lineStyle: { color: '#94a3b8', type: 'dashed' },
          label: { formatter: 'Avg Sales' },
          data: [{ type: 'average', name: 'Average Sales' }]
        }
      },
      ...(hasOrders
        ? [{
            name: 'Orders',
            type: 'bar',
            yAxisIndex: 1,
            barWidth: 12,
            data: orderValues,
            itemStyle: { borderRadius: [5, 5, 0, 0] }
          }]
        : [])
    ]
  })
}

const renderProfitChart = (rows) => {
  profitChart?.dispose()
  if (!profitChartRef.value) return
  profitChart = echarts.init(profitChartRef.value)
  const dates = rows.map(item => item.date)
  const profits = rows.map(item => toNumber(item.profit))
  const cumulative = profits.reduce((result, value, index) => {
    result.push((result[index - 1] || 0) + value)
    return result
  }, [])
  profitChart.setOption({
    color: ['#10b981', '#2563eb'],
    graphic: emptyGraphic(rows.length, 'No maintained profit records for this month'),
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: params => {
        const title = params[0]?.axisValue || ''
        const lines = params.map(item => `${item.marker}${item.seriesName}: ${money(item.value)}`)
        return [title, ...lines].join('<br/>')
      }
    },
    legend: { top: 0, textStyle: { color: '#475569' } },
    grid: { left: 42, right: 28, top: 44, bottom: 36, containLabel: true },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: { rotate: 25, color: '#64748b' },
      axisLine: { lineStyle: { color: '#cbd5e1' } }
    },
    yAxis: {
      type: 'value',
      name: 'Gross Profit',
      axisLabel: { color: '#64748b', formatter: value => `¥${value}` },
      splitLine: { lineStyle: { color: '#eef2f7' } }
    },
    series: [
      {
        name: 'Daily Gross Profit',
        type: 'bar',
        barWidth: 12,
        data: profits,
        itemStyle: { borderRadius: [5, 5, 0, 0] }
      },
      {
        name: 'Cumulative Gross Profit',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: cumulative,
        lineStyle: { width: 3 }
      }
    ]
  })
}

const renderCostChart = (cost) => {
  costChart?.dispose()
  if (!costChartRef.value) return
  costChart = echarts.init(costChartRef.value)
  const data = [
    { name: 'Material', value: toNumber(cost.materialCost), itemStyle: { color: '#f97316' } },
    { name: 'Labor', value: toNumber(cost.laborCost), itemStyle: { color: '#2563eb' } },
    { name: 'Waste', value: toNumber(cost.wasteCost), itemStyle: { color: '#e4002b' } }
  ]
  const totalCost = data.reduce((sum, item) => sum + item.value, 0)
  costChart.setOption({
    graphic: emptyGraphic(totalCost, 'No maintained cost records for this month'),
    tooltip: { trigger: 'item', formatter: '{b}: {d}% (¥{c})' },
    legend: { bottom: 0, textStyle: { color: '#475569' } },
    title: {
      text: `¥${totalCost.toLocaleString()}`,
      subtext: 'Total Cost',
      left: 'center',
      top: '39%',
      textStyle: { color: '#111827', fontSize: 20, fontWeight: 800 },
      subtextStyle: { color: '#64748b', fontSize: 12 }
    },
    series: [
      {
        type: 'pie',
        radius: ['50%', '72%'],
        center: ['50%', '45%'],
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{d}%', color: '#334155' },
        data
      }
    ]
  })
}

const renderEmployeeChart = (rows) => {
  employeeChart?.dispose()
  if (!employeeChartRef.value) return
  const chartRows = [...rows]
    .sort((a, b) => toNumber(b.score) - toNumber(a.score))
    .slice(0, 8)
  employeeChart = echarts.init(employeeChartRef.value)
  employeeChart.setOption({
    graphic: emptyGraphic(chartRows.length, 'No maintained employee performance data for this month'),
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: params => {
        const row = chartRows[params[0]?.dataIndex] || {}
        return `${row.employeeName || ''}<br/>Score: ${row.score || 0}<br/>Hours: ${row.totalHours || 0}<br/>Attendance: ${row.attendanceRate || 0}%<br/>Cost per Hour: ${money(row.salaryPerHour)}`
      }
    },
    grid: { left: 86, right: 28, top: 20, bottom: 28, containLabel: true },
    xAxis: {
      type: 'value',
      max: 100,
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { color: '#eef2f7' } }
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: chartRows.map(item => item.employeeName),
      axisLabel: { color: '#475569' },
      axisLine: { lineStyle: { color: '#cbd5e1' } }
    },
    series: [
      {
        name: 'Score',
        type: 'bar',
        barWidth: 14,
        data: chartRows.map(item => ({
          value: toNumber(item.score),
          itemStyle: {
            color: toNumber(item.score) >= 90 ? '#10b981' : toNumber(item.score) >= 75 ? '#2563eb' : toNumber(item.score) >= 60 ? '#f97316' : '#e4002b',
            borderRadius: [0, 5, 5, 0]
          }
        })),
        label: { show: true, position: 'right', color: '#334155' }
      }
    ]
  })
}

const renderInventoryChart = (rows) => {
  inventoryChart?.dispose()
  if (!inventoryChartRef.value) return
  const chartRows = [...rows]
    .sort((a, b) => toNumber(b.suggestedQuantity) - toNumber(a.suggestedQuantity))
    .slice(0, 8)
  inventoryChart = echarts.init(inventoryChartRef.value)
  inventoryChart.setOption({
    color: ['#e4002b'],
    graphic: emptyGraphic(chartRows.length, 'No inventory replenishment alerts'),
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: params => {
        const row = chartRows[params[0]?.dataIndex] || {}
        return `${row.productName || ''}<br/>Stock: ${row.stock || 0}<br/>Min Stock: ${row.minStock || 0}<br/>Suggested Qty: ${row.suggestedQuantity || 0}<br/>Estimated Cost: ¥${row.estimatedCost || 0}`
      }
    },
    grid: { left: 78, right: 26, top: 20, bottom: 28, containLabel: true },
    xAxis: {
      type: 'value',
      name: 'Suggested Qty',
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { color: '#eef2f7' } }
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: chartRows.map(item => item.productName),
      axisLabel: { color: '#475569' },
      axisLine: { lineStyle: { color: '#cbd5e1' } }
    },
    series: [
      {
        name: 'Suggested Qty',
        type: 'bar',
        barWidth: 14,
        data: chartRows.map(item => toNumber(item.suggestedQuantity)),
        itemStyle: { borderRadius: [0, 5, 5, 0] },
        label: { show: true, position: 'right', color: '#334155' }
      }
    ]
  })
}

const renderWasteChart = (stats) => {
  wasteChart?.dispose()
  if (!wasteChartRef.value) return
  const hasWasteData = toNumber(stats.totalCount) || toNumber(stats.totalCost) || toNumber(stats.pendingCount)
  const closureRate = hasWasteData ? Math.max(0, Math.min(100, toNumber(stats.closureRate))) : 0
  wasteChart = echarts.init(wasteChartRef.value)
  wasteChart.setOption({
    graphic: emptyGraphic(hasWasteData, 'No maintained waste records for this month'),
    series: [
      {
        name: 'Closure Rate',
        type: 'gauge',
        radius: '92%',
        startAngle: 205,
        endAngle: -25,
        min: 0,
        max: 100,
        progress: { show: true, roundCap: true, width: 16, itemStyle: { color: '#e4002b' } },
        axisLine: { roundCap: true, lineStyle: { width: 16, color: [[1, '#f1f5f9']] } },
        axisTick: { show: false },
        splitLine: { length: 8, lineStyle: { color: '#94a3b8', width: 1 } },
        axisLabel: { color: '#64748b', distance: 24 },
        pointer: { show: false },
        title: { offsetCenter: [0, '32%'], color: '#64748b', fontSize: 13 },
        detail: {
          valueAnimation: true,
          formatter: '{value}%',
          color: '#111827',
          fontSize: 28,
          fontWeight: 800,
          offsetCenter: [0, '8%']
        },
        data: [{ value: closureRate, name: 'Closure Rate' }]
      }
    ]
  })
}

const resizeCharts = () => {
  salesChart?.resize()
  profitChart?.resize()
  costChart?.resize()
  employeeChart?.resize()
  inventoryChart?.resize()
  wasteChart?.resize()
}

onMounted(() => {
  const now = new Date()
  month.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  loadDashboard()
  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  salesChart?.dispose()
  profitChart?.dispose()
  costChart?.dispose()
  employeeChart?.dispose()
  inventoryChart?.dispose()
  wasteChart?.dispose()
  window.removeEventListener('resize', resizeCharts)
})
</script>

<style scoped>
.dashboard-page {
  max-width: 1500px;
  margin: 0 auto;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.toolbar h2 {
  font-size: 24px;
  color: #111827;
  letter-spacing: 0;
}

.toolbar span {
  color: #64748b;
  font-size: 14px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.kpi-card,
.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.kpi-card {
  padding: 18px;
  border-top: 3px solid var(--accent, #e4002b);
}

.kpi-label {
  color: #64748b;
  font-size: 13px;
}

.kpi-value {
  font-size: 28px;
  font-weight: 800;
  color: #111827;
  margin: 8px 0;
}

.kpi-sub {
  color: #94a3b8;
  font-size: 13px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 16px;
}

.wide {
  grid-column: 1 / -1;
  min-width: 0;
}

.panel {
  padding: 16px;
  min-height: 320px;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  font-weight: 700;
  color: #1f2937;
}

.panel-title small {
  display: block;
  margin-top: 4px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 500;
}

.list-tools {
  margin-bottom: 10px;
}

.pagination-bar {
  padding-top: 10px;
}

.chart {
  height: 260px;
  width: 100%;
}

.compact-chart {
  height: 220px;
}

.waste-panel {
  grid-column: 1 / -1;
  min-height: 280px;
}

.closure {
  display: flex;
  justify-content: center;
  color: #475569;
}

.closure-stats {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.closure-stats p {
  margin: 0;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
  color: #334155;
  font-size: 13px;
}

@media (max-width: 1100px) {
  .kpi-grid,
  .content-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 760px) {
  .toolbar,
  .kpi-grid,
  .content-grid {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
