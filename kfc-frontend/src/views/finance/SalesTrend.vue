<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>Sales Trend Analysis</h2>
        <span>Track sales, orders, average ticket, and period growth</span>
      </div>
      <div class="actions">
        <input v-model="startDate" type="date" class="date-input" />
        <input v-model="endDate" type="date" class="date-input" />
        <el-button class="red-btn" @click="loadTrend">Analyze</el-button>
        <el-button class="outline-btn" @click="exportSales">Export Excel</el-button>
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
          <div>Sales, Orders, and Average Ticket</div>
          <small>Source: order entry and sales daily records</small>
        </div>
        <div ref="trendChartRef" class="chart"></div>
      </section>

      <section class="panel chart-panel">
        <div class="panel-title">
          <div>Order Volume and Ticket Relationship</div>
          <small>Each point is one maintained sales day</small>
        </div>
        <div ref="ticketChartRef" class="chart"></div>
      </section>
    </div>

    <section class="panel">
      <div class="list-tools">
        <div class="panel-title">Details</div>
        <el-input v-model="searchText" clearable class="search-input" placeholder="Search details" @input="currentPage = 1" @clear="currentPage = 1" />
      </div>
      <el-table :data="pagedDailyList" stripe class="adaptive-table">
        <el-table-column prop="date" label="Date" min-width="130" />
        <el-table-column prop="weekday" label="Weekday" min-width="100" />
        <el-table-column prop="totalAmount" label="Sales" min-width="130">
          <template #default="{ row }">{{ money(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="orderCount" label="Orders" min-width="100" />
        <el-table-column prop="avgTicket" label="Average Ticket" min-width="130">
          <template #default="{ row }">{{ money(row.avgTicket) }}</template>
        </el-table-column>
        <el-table-column prop="salesShare" label="Sales Share" min-width="120">
          <template #default="{ row }">{{ percent(row.salesShare) }}</template>
        </el-table-column>
        <el-table-column prop="previousDayChange" label="Vs Previous Day" min-width="150">
          <template #default="{ row }">
            <span :class="['change-value', changeClass(row.previousDayChange)]">{{ signedMoney(row.previousDayChange) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="cumulativeSales" label="Cumulative Sales" min-width="150">
          <template #default="{ row }">{{ money(row.cumulativeSales) }}</template>
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
import * as echarts from 'echarts'
import request from '@/utils/request'
import { DEFAULT_PAGE_SIZE, PAGE_SIZES, filterRows, pageRows } from '@/utils/listTools'

const startDate = ref('')
const endDate = ref('')
const summary = ref({})
const dailyList = ref([])
const searchText = ref('')
const currentPage = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const trendChartRef = ref(null)
const ticketChartRef = ref(null)
let trendChart = null
let ticketChart = null
const dailyRows = computed(() => {
  let cumulativeSales = 0
  const totalSales = Number(summary.value.totalSales || 0)
  return dailyList.value.map((item, index, rows) => {
    const totalAmount = Number(item.totalAmount || 0)
    const previousAmount = index > 0 ? Number(rows[index - 1].totalAmount || 0) : null
    cumulativeSales += totalAmount
    return {
      ...item,
      weekday: weekday(item.date),
      salesShare: totalSales ? totalAmount / totalSales * 100 : 0,
      previousDayChange: previousAmount == null ? null : totalAmount - previousAmount,
      cumulativeSales: Math.round(cumulativeSales * 100) / 100
    }
  })
})
const filteredDailyList = computed(() => filterRows(dailyRows.value, searchText.value))
const pagedDailyList = computed(() => pageRows(filteredDailyList.value, currentPage.value, pageSize.value))
const money = value => `¥${Number(value || 0).toLocaleString(undefined, { maximumFractionDigits: 2 })}`
const percent = value => `${Number(value || 0).toLocaleString(undefined, { maximumFractionDigits: 2 })}%`
const displayValue = value => value || '-'
const signedMoney = value => {
  if (value == null) return '-'
  const amount = Number(value || 0)
  const prefix = amount > 0 ? '+' : amount < 0 ? '-' : ''
  return `${prefix}${money(Math.abs(amount))}`
}
const changeClass = value => {
  const amount = Number(value || 0)
  if (amount > 0) return 'positive'
  if (amount < 0) return 'negative'
  return 'neutral'
}
const weekday = value => {
  if (!value) return '-'
  return new Date(`${value}T00:00:00`).toLocaleDateString('en-US', { weekday: 'short' })
}
const kpiCards = computed(() => [
  { label: 'Total Sales', value: money(summary.value.totalSales), sub: `Previous period ${money(summary.value.previousSales)}` },
  { label: 'Orders', value: Number(summary.value.totalOrders || 0).toLocaleString(), sub: `${dailyList.value.length} sales days` },
  { label: 'Average Ticket', value: money(summary.value.avgTicket), sub: `Daily average ${money(summary.value.avgDailySales)}` },
  { label: 'Period Growth', value: percent(summary.value.growthRate), sub: 'Compared with previous equal period' },
  { label: 'Best Sales Day', value: displayValue(summary.value.bestDay), sub: 'Highest maintained daily sales' },
  { label: 'Lowest Sales Day', value: displayValue(summary.value.worstDay), sub: 'Lowest maintained daily sales' }
])

const loadTrend = async () => {
  const data = await request.get('/sales-analysis/trend', { params: { start: startDate.value, end: endDate.value } })
  summary.value = data.summary || {}
  dailyList.value = data.dailyList || []
  await nextTick()
  renderChart()
}

const exportSales = () => {
  const params = new URLSearchParams({ start: startDate.value, end: endDate.value })
  window.open(`/api/reports/export/sales?${params.toString()}`)
}

const renderChart = () => {
  trendChart?.dispose()
  ticketChart?.dispose()
  if (!trendChartRef.value || !ticketChartRef.value) return
  const rows = dailyList.value
  const dates = rows.map(item => item.date)
  const salesValues = rows.map(item => Number(item.totalAmount || 0))
  const orderValues = rows.map(item => Number(item.orderCount || 0))
  const ticketValues = rows.map(item => Number(item.avgTicket || 0))
  const emptyGraphic = rows.length ? [] : [{
    type: 'text',
    left: 'center',
    top: 'middle',
    style: { text: 'No maintained sales records in this range', fill: '#94a3b8', fontSize: 14 }
  }]
  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    color: ['#e4002b', '#2563eb', '#10b981'],
    graphic: emptyGraphic,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: params => {
        const title = params[0]?.axisValue || ''
        const lines = params.map(item => {
          const isOrder = item.seriesName === 'Orders'
          const value = isOrder ? item.value : `¥${Number(item.value || 0).toLocaleString()}`
          return `${item.marker}${item.seriesName}: ${value}`
        })
        return [title, ...lines].join('<br/>')
      }
    },
    legend: { top: 0, textStyle: { color: '#475569' } },
    grid: { left: 42, right: 78, top: 48, bottom: 36, containLabel: true },
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
        position: 'right',
        axisLabel: { color: '#64748b' },
        splitLine: { show: false }
      },
      {
        type: 'value',
        name: 'Avg Ticket',
        position: 'right',
        offset: 54,
        axisLabel: { color: '#64748b', formatter: value => `¥${value}` },
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
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(228, 0, 43, 0.22)' },
            { offset: 1, color: 'rgba(228, 0, 43, 0.02)' }
          ])
        },
        lineStyle: { width: 3 },
        markLine: {
          symbol: 'none',
          lineStyle: { color: '#94a3b8', type: 'dashed' },
          label: { formatter: 'Avg Sales' },
          data: [{ type: 'average', name: 'Average Sales' }]
        }
      },
      {
        name: 'Orders',
        type: 'bar',
        yAxisIndex: 1,
        barWidth: 14,
        data: orderValues,
        itemStyle: { borderRadius: [5, 5, 0, 0] }
      },
      {
        name: 'Average Ticket',
        type: 'line',
        yAxisIndex: 2,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: ticketValues,
        lineStyle: { width: 2, type: 'dashed' }
      }
    ]
  })

  ticketChart = echarts.init(ticketChartRef.value)
  ticketChart.setOption({
    color: ['#e4002b'],
    graphic: emptyGraphic,
    tooltip: {
      trigger: 'item',
      formatter: params => {
        const [orders, ticket, sales, date] = params.data
        return `${date}<br/>Orders: ${orders}<br/>Average Ticket: ¥${ticket}<br/>Sales: ¥${Number(sales || 0).toLocaleString()}`
      }
    },
    grid: { left: 44, right: 24, top: 28, bottom: 40, containLabel: true },
    xAxis: {
      type: 'value',
      name: 'Orders',
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { color: '#eef2f7' } }
    },
    yAxis: {
      type: 'value',
      name: 'Avg Ticket',
      axisLabel: { color: '#64748b', formatter: value => `¥${value}` },
      splitLine: { lineStyle: { color: '#eef2f7' } }
    },
    series: [
      {
        name: 'Daily Order Value',
        type: 'scatter',
        data: rows.map(item => [
          Number(item.orderCount || 0),
          Number(item.avgTicket || 0),
          Number(item.totalAmount || 0),
          item.date
        ]),
        symbolSize: data => Math.max(10, Math.min(32, Math.sqrt(Number(data[2] || 0)) / 10)),
        itemStyle: {
          color: 'rgba(228, 0, 43, 0.72)',
          borderColor: '#fff',
          borderWidth: 2
        },
        emphasis: {
          itemStyle: { color: '#c80025' }
        }
      }
    ]
  })
}

const initDates = () => {
  const end = new Date()
  const start = new Date(end.getTime() - 29 * 24 * 3600 * 1000)
  startDate.value = start.toISOString().slice(0, 10)
  endDate.value = end.toISOString().slice(0, 10)
}

const resize = () => {
  trendChart?.resize()
  ticketChart?.resize()
}

onMounted(() => {
  initDates()
  loadTrend()
  window.addEventListener('resize', resize)
})

onUnmounted(() => {
  trendChart?.dispose()
  ticketChart?.dispose()
  window.removeEventListener('resize', resize)
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

.actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.toolbar h2 {
  color: #111827;
  letter-spacing: 0;
}

.toolbar span {
  color: #64748b;
  font-size: 14px;
}

.date-input {
  height: 32px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 0 10px;
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
  grid-template-columns: 1.45fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.panel {
  padding: 16px;
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
}

.wide .chart {
  min-height: 380px;
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
