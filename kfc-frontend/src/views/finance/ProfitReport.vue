<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>Profit Statistics Analysis</h2>
        <span>Track sales, material cost, labor cost, waste cost, and profit margin</span>
      </div>
      <div class="actions">
        <input v-model="month" type="month" class="date-input" @change="loadReport" />
        <el-button class="red-btn" @click="loadReport">Analyze</el-button>
        <el-button class="outline-btn" @click="exportExcel">Export Excel</el-button>
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
          <div>Daily Gross Profit Trend</div>
          <small>Daily sales minus maintained sales outbound material cost</small>
        </div>
        <div ref="dailyChartRef" class="chart"></div>
      </section>

      <section class="panel chart-panel">
        <div class="panel-title">
          <div>Cost Breakdown</div>
          <small>Material, labor, and waste costs from maintained modules</small>
        </div>
        <div ref="costChartRef" class="chart"></div>
      </section>

      <section class="panel chart-panel">
        <div class="panel-title">
          <div>Net Margin</div>
          <small>Monthly net profit divided by monthly sales</small>
        </div>
        <div ref="marginChartRef" class="chart"></div>
      </section>
    </div>

    <section class="panel">
      <div class="list-tools">
        <div class="panel-title">Daily Gross Profit Details</div>
        <el-input v-model="searchText" clearable class="search-input" placeholder="Search profit details" @input="currentPage = 1" @clear="currentPage = 1" />
      </div>
      <el-table :data="pagedDailyList" stripe class="adaptive-table">
        <el-table-column prop="date" label="Date" min-width="130" />
        <el-table-column prop="weekday" label="Weekday" min-width="100" />
        <el-table-column prop="profit" label="Gross Profit" min-width="140">
          <template #default="{ row }">{{ money(row.profit) }}</template>
        </el-table-column>
        <el-table-column prop="profitShare" label="Profit Share" min-width="120">
          <template #default="{ row }">{{ percent(row.profitShare) }}</template>
        </el-table-column>
        <el-table-column prop="previousDayChange" label="Vs Previous Day" min-width="150">
          <template #default="{ row }">
            <span :class="['change-value', changeClass(row.previousDayChange)]">{{ signedMoney(row.previousDayChange) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="cumulativeProfit" label="Cumulative Gross Profit" min-width="190">
          <template #default="{ row }">{{ money(row.cumulativeProfit) }}</template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          background
          layout="total, sizes, prev, pager, next"
          :page-sizes="PAGE_SIZES"
          :total="filteredDailyList.length"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import request from '@/utils/request'
import { DEFAULT_PAGE_SIZE, PAGE_SIZES, filterRows, pageRows } from '@/utils/listTools'
import * as echarts from 'echarts'

const month = ref('')
const summary = ref({})
const dailyList = ref([])
const costBreakdown = ref({})
const searchText = ref('')
const currentPage = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const dailyChartRef = ref(null)
const costChartRef = ref(null)
const marginChartRef = ref(null)
let dailyChart = null
let costChart = null
let marginChart = null
const toNumber = value => Number(value || 0)
const money = value => `¥${toNumber(value).toLocaleString(undefined, { maximumFractionDigits: 2 })}`
const percent = value => `${toNumber(value).toLocaleString(undefined, { maximumFractionDigits: 2 })}%`
const signedMoney = value => {
  if (value == null) return '-'
  const amount = toNumber(value)
  const prefix = amount > 0 ? '+' : amount < 0 ? '-' : ''
  return `${prefix}${money(Math.abs(amount))}`
}
const changeClass = value => {
  const amount = toNumber(value)
  if (amount > 0) return 'positive'
  if (amount < 0) return 'negative'
  return 'neutral'
}
const weekday = value => {
  if (!value) return '-'
  return new Date(`${value}T00:00:00`).toLocaleDateString('en-US', { weekday: 'short' })
}
const totalCost = computed(() => toNumber(summary.value.materialCost) + toNumber(summary.value.laborCost) + toNumber(summary.value.wasteCost))
const dailyRows = computed(() => {
  let cumulativeProfit = 0
  const totalProfit = dailyList.value.reduce((sum, item) => sum + toNumber(item.profit), 0)
  return dailyList.value.map((item, index, rows) => {
    const profit = toNumber(item.profit)
    const previousProfit = index > 0 ? toNumber(rows[index - 1].profit) : null
    cumulativeProfit += profit
    return {
      ...item,
      weekday: weekday(item.date),
      profitShare: totalProfit ? profit / totalProfit * 100 : 0,
      previousDayChange: previousProfit == null ? null : profit - previousProfit,
      cumulativeProfit: Math.round(cumulativeProfit * 100) / 100
    }
  })
})
const filteredDailyList = computed(() => filterRows(dailyRows.value, searchText.value))
const pagedDailyList = computed(() => pageRows(filteredDailyList.value, currentPage.value, pageSize.value))
const kpiCards = computed(() => [
  { label: 'Total Sales', value: money(summary.value.totalSales), sub: `${dailyList.value.length} maintained sales days` },
  { label: 'Total Cost', value: money(totalCost.value), sub: 'Material + labor + waste' },
  { label: 'Gross Profit', value: money(summary.value.grossProfit), sub: 'Sales minus material cost' },
  { label: 'Net Profit', value: money(summary.value.netProfit), sub: 'Gross profit minus labor and waste' },
  { label: 'Net Margin', value: percent(summary.value.profitMargin), sub: 'Net profit / sales' },
  { label: 'Material Cost', value: money(summary.value.materialCost), sub: 'Sales outbound records' },
  { label: 'Labor Cost', value: money(summary.value.laborCost), sub: 'Payroll records' },
  { label: 'Waste Cost', value: money(summary.value.wasteCost), sub: 'Waste closure records' }
])

const loadReport = async () => {
  if (!month.value) return
  const data = await request.get('/profit/month', { params: { month: month.value } })
  summary.value = data.summary || {}
  dailyList.value = data.dailyList || []
  costBreakdown.value = data.costBreakdown || {}
  await nextTick()
  renderDailyChart(dailyList.value)
  renderCostChart(costBreakdown.value)
  renderMarginChart(summary.value)
}

const renderDailyChart = (dailyList) => {
  dailyChart?.dispose()
  if (!dailyChartRef.value) return
  dailyChart = echarts.init(dailyChartRef.value)
  const dates = dailyList.map(item => item.date)
  const profits = dailyList.map(item => toNumber(item.profit))
  const emptyGraphic = dailyList.length ? [] : [{
    type: 'text',
    left: 'center',
    top: 'middle',
    style: { text: 'No maintained sales records for this month', fill: '#94a3b8', fontSize: 14 }
  }]
  const cumulative = profits.reduce((result, value, index) => {
    result.push((result[index - 1] || 0) + value)
    return result
  }, [])
  dailyChart.setOption({
    color: ['#e4002b', '#2563eb'],
    graphic: emptyGraphic,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: params => {
        const title = params[0]?.axisValue || ''
        const lines = params.map(item => `${item.marker}${item.seriesName}: ¥${Number(item.value || 0).toLocaleString()}`)
        return [title, ...lines].join('<br/>')
      }
    },
    legend: { top: 0, textStyle: { color: '#475569' } },
    grid: { left: 42, right: 32, top: 48, bottom: 36, containLabel: true },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: { rotate: 25, color: '#64748b' },
      axisLine: { lineStyle: { color: '#cbd5e1' } }
    },
    yAxis: {
      type: 'value',
      name: 'Profit',
      axisLabel: { color: '#64748b', formatter: value => `¥${value}` },
      splitLine: { lineStyle: { color: '#eef2f7' } }
    },
    series: [
      {
        name: 'Daily Gross Profit',
        type: 'bar',
        barWidth: 16,
        data: profits,
        itemStyle: { borderRadius: [5, 5, 0, 0] },
        markLine: {
          symbol: 'none',
          lineStyle: { color: '#94a3b8', type: 'dashed' },
          label: { formatter: 'Avg Gross Profit' },
          data: [{ type: 'average', name: 'Average Gross Profit' }]
        }
      },
      {
        name: 'Cumulative Gross Profit',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: cumulative,
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(37, 99, 235, 0.18)' },
            { offset: 1, color: 'rgba(37, 99, 235, 0.02)' }
          ])
        }
      }
    ]
  })
}

const renderCostChart = (costBreakdown) => {
  costChart?.dispose()
  if (!costChartRef.value) return
  costChart = echarts.init(costChartRef.value)
  const data = [
    { value: toNumber(costBreakdown.materialCost), name: 'Material Cost', itemStyle: { color: '#f97316' } },
    { value: toNumber(costBreakdown.laborCost), name: 'Labor Cost', itemStyle: { color: '#2563eb' } },
    { value: toNumber(costBreakdown.wasteCost), name: 'Waste Cost', itemStyle: { color: '#e4002b' } }
  ]
  const totalCost = data.reduce((sum, item) => sum + item.value, 0)
  costChart.setOption({
    graphic: totalCost ? [] : [{
      type: 'text',
      left: 'center',
      top: 'middle',
      style: { text: 'No maintained cost records for this month', fill: '#94a3b8', fontSize: 14 }
    }],
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
        name: 'Cost',
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

const renderMarginChart = (summaryData) => {
  marginChart?.dispose()
  if (!marginChartRef.value) return
  marginChart = echarts.init(marginChartRef.value)
  const margin = Math.max(-100, Math.min(100, toNumber(summaryData.profitMargin)))
  marginChart.setOption({
    series: [
      {
        name: 'Profit Margin',
        type: 'gauge',
        radius: '90%',
        startAngle: 205,
        endAngle: -25,
        min: -100,
        max: 100,
        splitNumber: 4,
        progress: {
          show: true,
          roundCap: true,
          width: 16,
          itemStyle: { color: margin >= 0 ? '#10b981' : '#e4002b' }
        },
        axisLine: {
          roundCap: true,
          lineStyle: { width: 16, color: [[1, '#f1f5f9']] }
        },
        axisTick: { show: false },
        splitLine: { length: 8, lineStyle: { color: '#94a3b8', width: 1 } },
        axisLabel: { color: '#64748b', distance: 24 },
        pointer: { show: false },
        anchor: { show: false },
        title: { offsetCenter: [0, '32%'], color: '#64748b', fontSize: 13 },
        detail: {
          valueAnimation: true,
          formatter: '{value}%',
          color: margin >= 0 ? '#111827' : '#e4002b',
          fontSize: 28,
          fontWeight: 800,
          offsetCenter: [0, '8%']
        },
        data: [{ value: margin, name: 'Net Margin' }]
      }
    ]
  })
}

const exportExcel = () => {
  if (month.value) window.open(`/api/reports/export/profit?month=${month.value}`)
}

const initMonth = () => {
  const now = new Date()
  month.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

const resize = () => {
  dailyChart?.resize()
  costChart?.resize()
  marginChart?.resize()
}

onMounted(() => {
  initMonth()
  loadReport()
  window.addEventListener('resize', resize)
})

onUnmounted(() => {
  dailyChart?.dispose()
  costChart?.dispose()
  marginChart?.dispose()
  window.removeEventListener('resize', resize)
})
</script>

<style scoped>
.page {
  max-width: 1400px;
  margin: 0 auto;
  color: #111827;
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
  min-height: 40px;
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
  font-size: 26px;
  color: #111827;
  line-height: 1.15;
}

.summary-card small {
  color: #94a3b8;
  font-size: 12px;
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
  color: #94a3b8;
  font-size: 12px;
  font-weight: 500;
}

.list-tools .panel-title {
  margin-bottom: 0;
}

.adaptive-table {
  width: 100%;
}

:deep(.adaptive-table .cell) {
  white-space: normal;
  word-break: normal;
}

.change-value {
  font-weight: 700;
}

.change-value.positive {
  color: #059669;
}

.change-value.negative {
  color: #dc2626;
}

.change-value.neutral {
  color: #64748b;
}

.chart {
  height: 360px;
  width: 100%;
}

@media (max-width: 900px) {
  .toolbar,
  .summary-grid,
  .grid {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
