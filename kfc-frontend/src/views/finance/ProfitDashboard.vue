<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>Profit Dashboard</h2>
        <span>Identify business issues across revenue, cost, profit, and waste</span>
      </div>
      <div class="actions">
        <el-date-picker v-model="month" type="month" value-format="YYYY-MM" @change="loadData" />
        <el-button class="red-btn" @click="exportProfit">Export Excel</el-button>
      </div>
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <span>Sales</span>
        <strong>¥{{ summary.totalSales || 0 }}</strong>
      </div>
      <div class="summary-card">
        <span>Gross Profit</span>
        <strong>¥{{ summary.grossProfit || 0 }}</strong>
      </div>
      <div class="summary-card">
        <span>Net Profit</span>
        <strong>¥{{ summary.netProfit || 0 }}</strong>
      </div>
      <div class="summary-card">
        <span>Net Margin</span>
        <strong>{{ summary.profitMargin || 0 }}%</strong>
      </div>
    </div>

    <div class="grid">
      <section class="panel">
        <div class="panel-title">Daily Profit</div>
        <div ref="profitChartRef" class="chart"></div>
      </section>
      <section class="panel">
        <div class="panel-title">Cost Mix</div>
        <div ref="costChartRef" class="chart"></div>
      </section>
    </div>

    <section class="panel">
      <div class="panel-title">Cost Source Summary</div>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="Material Cost">¥{{ summary.materialCost || 0 }}</el-descriptions-item>
        <el-descriptions-item label="Labor Cost">¥{{ summary.laborCost || 0 }}</el-descriptions-item>
        <el-descriptions-item label="Waste Cost">¥{{ summary.wasteCost || 0 }}</el-descriptions-item>
      </el-descriptions>
    </section>
  </div>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

const month = ref('')
const summary = ref({})
const dailyList = ref([])
const costBreakdown = ref({})
const profitChartRef = ref(null)
const costChartRef = ref(null)
let profitChart = null
let costChart = null

const loadData = async () => {
  const data = await request.get('/profit/month', { params: { month: month.value } })
  summary.value = data.summary || {}
  dailyList.value = data.dailyList || []
  costBreakdown.value = data.costBreakdown || {}
  await nextTick()
  renderCharts()
}

const exportProfit = () => {
  if (!month.value) return
  window.open(`/api/reports/export/profit?month=${month.value}`)
}

const renderCharts = () => {
  profitChart?.dispose()
  profitChart = echarts.init(profitChartRef.value)
  profitChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 44, right: 24, top: 32, bottom: 40, containLabel: true },
    xAxis: { type: 'category', data: dailyList.value.map(item => item.date), axisLabel: { rotate: 25 } },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, data: dailyList.value.map(item => item.profit), itemStyle: { color: '#e4002b' }, areaStyle: { color: 'rgba(228,0,43,0.12)' } }]
  })

  costChart?.dispose()
  costChart = echarts.init(costChartRef.value)
  costChart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['45%', '72%'],
      data: [
        { name: 'Material Cost', value: costBreakdown.value.materialCost || 0, itemStyle: { color: '#f97316' } },
        { name: 'Labor Cost', value: costBreakdown.value.laborCost || 0, itemStyle: { color: '#2563eb' } },
        { name: 'Waste Cost', value: costBreakdown.value.wasteCost || 0, itemStyle: { color: '#e4002b' } }
      ]
    }]
  })
}

const resize = () => {
  profitChart?.resize()
  costChart?.resize()
}

onMounted(() => {
  const now = new Date()
  month.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  loadData()
  window.addEventListener('resize', resize)
})

onUnmounted(() => {
  profitChart?.dispose()
  costChart?.dispose()
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

.toolbar h2 {
  color: #111827;
  letter-spacing: 0;
}

.toolbar span {
  color: #64748b;
  font-size: 14px;
}

.summary-grid,
.grid {
  display: grid;
  gap: 16px;
}

.summary-grid {
  grid-template-columns: repeat(4, minmax(160px, 1fr));
  margin-bottom: 16px;
}

.grid {
  grid-template-columns: 1.4fr 1fr;
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
  font-size: 26px;
  color: #111827;
}

.panel {
  padding: 16px;
}

.panel-title {
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 12px;
}

.chart {
  height: 360px;
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
