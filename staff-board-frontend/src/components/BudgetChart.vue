<template>
  <div ref="chartRef" :style="{ width: '100%', height: height + 'px' }"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  type: { type: String, default: 'bar' },  // bar / pie
  data: { type: Object, default: () => ({}) },
  height: { type: Number, default: 350 }
})

const chartRef = ref(null)
let chart = null

function renderChart() {
  if (!chartRef.value || !props.data) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  if (props.type === 'pie') {
    chart.setOption({
      title: { text: props.data.title || '', left: 'center' },
      tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: props.data.items || [],
        emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } }
      }]
    })
  } else {
    chart.setOption({
      title: { text: props.data.title || '' },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: props.data.categories || [] },
      yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
      series: [
        { name: '实际成本', type: 'bar', data: props.data.actual || [], itemStyle: { color: '#409EFF' } },
        { name: '预算额度', type: 'bar', data: props.data.budget || [], itemStyle: { color: '#67C23A' } }
      ]
    })
  }
}

onMounted(() => {
  renderChart()
  window.addEventListener('resize', () => chart?.resize())
})

onUnmounted(() => {
  chart?.dispose()
  window.removeEventListener('resize', () => chart?.resize())
})

watch(() => props.data, renderChart, { deep: true })
</script>
