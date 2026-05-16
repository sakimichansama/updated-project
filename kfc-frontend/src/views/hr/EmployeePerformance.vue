<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>Employee Performance Analysis</h2>
        <span>Score performance using work hours, payroll, attendance, and labor cost per hour</span>
      </div>
      <div class="actions">
        <el-input v-model="searchText" clearable class="search-input" placeholder="Search employees" @input="currentPage = 1" @clear="currentPage = 1" />
        <el-date-picker v-model="month" type="month" value-format="YYYY-MM" @change="loadPerformance" />
        <el-button class="red-btn" @click="exportPerformance">Export Excel</el-button>
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
      <section class="panel">
        <div class="panel-title">
          <div>Score Ranking by Employee</div>
          <small>Source: employee records, work hours, and payroll</small>
        </div>
        <div ref="scoreChartRef" class="chart"></div>
      </section>
      <section class="panel">
        <div class="panel-title">
          <div>Hours, Attendance, and Cost Efficiency</div>
          <small>Each point is one maintained employee in the selected month</small>
        </div>
        <div ref="costChartRef" class="chart"></div>
      </section>
    </div>

    <section class="panel">
      <div class="panel-title">Employee Details</div>
      <el-table :data="pagedList" stripe>
        <el-table-column prop="rank" label="Rank" width="80" />
        <el-table-column prop="employeeName" label="Employee" min-width="120" />
        <el-table-column prop="position" label="Position" min-width="120" />
        <el-table-column prop="totalHours" label="Hours" width="100" />
        <el-table-column prop="attendanceRate" label="Attendance Rate" width="100">
          <template #default="{ row }">{{ row.attendanceRate }}%</template>
        </el-table-column>
        <el-table-column prop="totalSalary" label="Salary" width="110">
          <template #default="{ row }">{{ money(row.totalSalary) }}</template>
        </el-table-column>
        <el-table-column prop="salaryPerHour" label="Cost per Hour" width="110">
          <template #default="{ row }">{{ money(row.salaryPerHour) }}</template>
        </el-table-column>
        <el-table-column prop="score" label="Score" width="100" />
        <el-table-column prop="suggestion" label="Advice" min-width="240" />
      </el-table>
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          background
          layout="total, sizes, prev, pager, next"
          :page-sizes="PAGE_SIZES"
          :total="filteredList.length"
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

const month = ref('')
const summary = ref({})
const list = ref([])
const searchText = ref('')
const currentPage = ref(1)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const scoreChartRef = ref(null)
const costChartRef = ref(null)
let scoreChart = null
let costChart = null
const filteredList = computed(() => filterRows(list.value, searchText.value))
const pagedList = computed(() => pageRows(filteredList.value, currentPage.value, pageSize.value))
const toNumber = value => Number(value || 0)
const money = value => `¥${toNumber(value).toLocaleString(undefined, { maximumFractionDigits: 2 })}`
const totalHours = computed(() => list.value.reduce((sum, item) => sum + toNumber(item.totalHours), 0))
const avgAttendance = computed(() => {
  if (!list.value.length) return 0
  return list.value.reduce((sum, item) => sum + toNumber(item.attendanceRate), 0) / list.value.length
})
const fullAttendanceCount = computed(() => list.value.filter(item => toNumber(item.totalHours) >= toNumber(summary.value.standardHours)).length)
const kpiCards = computed(() => [
  { label: 'Employees', value: Number(summary.value.employeeCount || 0).toLocaleString(), sub: `${fullAttendanceCount.value} reached standard hours` },
  { label: 'Average Score', value: toNumber(summary.value.avgScore).toLocaleString(undefined, { maximumFractionDigits: 2 }), sub: `Average attendance ${avgAttendance.value.toFixed(2)}%` },
  { label: 'Labor Cost', value: money(summary.value.totalLaborCost), sub: `${money(list.value.reduce((sum, item) => sum + toNumber(item.totalSalary), 0))} from employee rows` },
  { label: 'Total Hours', value: totalHours.value.toLocaleString(undefined, { maximumFractionDigits: 2 }), sub: `Standard ${summary.value.standardHours || 0} hours / employee` },
  { label: 'Average Cost / Hour', value: money(totalHours.value ? toNumber(summary.value.totalLaborCost) / totalHours.value : 0), sub: 'Labor cost divided by hours' },
  { label: 'Performance Records', value: list.value.length.toLocaleString(), sub: 'Employee rows returned by API' }
])

const scoreColor = score => {
  if (score >= 90) return '#10b981'
  if (score >= 75) return '#2563eb'
  if (score >= 60) return '#f97316'
  return '#e4002b'
}

const loadPerformance = async () => {
  const data = await request.get('/employee-performance/month', { params: { month: month.value } })
  summary.value = data.summary || {}
  list.value = data.list || []
  await nextTick()
  renderCharts()
}

const exportPerformance = () => {
  if (!month.value) return
  window.open(`/api/reports/export/employee-performance?month=${month.value}`)
}

const renderCharts = () => {
  const rows = [...list.value].sort((a, b) => toNumber(a.rank || 9999) - toNumber(b.rank || 9999))
  const names = rows.map(item => item.employeeName)
  const emptyGraphic = rows.length ? [] : [{
    type: 'text',
    left: 'center',
    top: 'middle',
    style: { text: 'No maintained employee performance data for this month', fill: '#94a3b8', fontSize: 14 }
  }]
  scoreChart?.dispose()
  scoreChart = echarts.init(scoreChartRef.value)
  scoreChart.setOption({
    graphic: emptyGraphic,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: params => {
        const item = rows[params[0]?.dataIndex] || {}
        return `${item.employeeName || ''}<br/>Rank: ${item.rank || '-'}<br/>Score: ${item.score || 0}<br/>Attendance: ${item.attendanceRate || 0}%`
      }
    },
    grid: { left: 96, right: 28, top: 18, bottom: 28, containLabel: true },
    xAxis: {
      type: 'value',
      max: 100,
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { color: '#eef2f7' } }
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: names,
      axisLabel: { color: '#475569' },
      axisLine: { lineStyle: { color: '#cbd5e1' } }
    },
    series: [
      {
        name: 'Score',
        type: 'bar',
        barWidth: 14,
        data: rows.map(item => ({
          value: toNumber(item.score),
          itemStyle: { color: scoreColor(toNumber(item.score)), borderRadius: [0, 5, 5, 0] }
        })),
        label: { show: true, position: 'right', color: '#334155', formatter: '{c}' },
        markLine: {
          symbol: 'none',
          lineStyle: { color: '#94a3b8', type: 'dashed' },
          label: { formatter: 'Avg Score' },
          data: [{ type: 'average', name: 'Average Score' }]
        }
      }
    ]
  })

  costChart?.dispose()
  costChart = echarts.init(costChartRef.value)
  const avgHours = rows.length ? rows.reduce((sum, item) => sum + toNumber(item.totalHours), 0) / rows.length : 0
  const avgCost = rows.length ? rows.reduce((sum, item) => sum + toNumber(item.salaryPerHour), 0) / rows.length : 0
  costChart.setOption({
    graphic: emptyGraphic,
    tooltip: {
      trigger: 'item',
      formatter: params => {
        const data = params.data || []
        return `${data[5] || ''}<br/>Hours: ${data[0]}<br/>Cost per Hour: ¥${data[1]}<br/>Score: ${data[2]}<br/>Attendance: ${data[3]}%<br/>Salary: ¥${data[4]}`
      }
    },
    grid: { left: 48, right: 26, top: 28, bottom: 42, containLabel: true },
    xAxis: {
      type: 'value',
      name: 'Hours',
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { color: '#eef2f7' } }
    },
    yAxis: {
      type: 'value',
      name: 'Cost per Hour',
      axisLabel: { color: '#64748b', formatter: value => `¥${value}` },
      splitLine: { lineStyle: { color: '#eef2f7' } }
    },
    visualMap: {
      show: false,
      min: 0,
      max: 100,
      dimension: 2,
      inRange: { color: ['#e4002b', '#f97316', '#2563eb', '#10b981'] }
    },
    series: [
      {
        name: 'Employee Efficiency',
        type: 'scatter',
        data: rows.map(item => [
          toNumber(item.totalHours),
          toNumber(item.salaryPerHour),
          toNumber(item.score),
          toNumber(item.attendanceRate),
          toNumber(item.totalSalary),
          item.employeeName
        ]),
        symbolSize: data => Math.max(12, Math.min(34, toNumber(data[2]) / 3)),
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        markLine: {
          silent: true,
          symbol: 'none',
          lineStyle: { color: '#94a3b8', type: 'dashed' },
          label: { color: '#64748b' },
          data: [
            { xAxis: Number(avgHours.toFixed(2)), name: 'Avg Hours' },
            { yAxis: Number(avgCost.toFixed(2)), name: 'Avg Cost' }
          ]
        }
      }
    ]
  })
}

const resize = () => {
  scoreChart?.resize()
  costChart?.resize()
}

onMounted(() => {
  const now = new Date()
  month.value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  loadPerformance()
  window.addEventListener('resize', resize)
})

onUnmounted(() => {
  scoreChart?.dispose()
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

.summary-grid,
.grid {
  display: grid;
  gap: 16px;
}

.summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  margin-bottom: 16px;
}

.grid {
  grid-template-columns: 1fr 1fr;
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
  line-height: 1.15;
}

.summary-card small {
  color: #94a3b8;
  font-size: 12px;
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

.pagination-bar {
  padding-top: 14px;
}

.chart {
  height: 340px;
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
