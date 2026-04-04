import { Component, OnInit } from '@angular/core';
import * as echarts from 'echarts';

@Component({
  selector: 'app-grapht',
  imports: [],
  templateUrl: './grapht.html',
  styleUrl: './grapht.css',
})
export class Grapht implements OnInit {
  chartInstance!: echarts.ECharts;

  months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'];

  seriesData = [
    { name: 'Sales', data: [120, 200, 150, 80, 70, 110], color: '#5470c6', prefix: '' },
    { name: 'Revenue', data: [90, 180, 130, 100, 60, 90], color: '#91cc75', prefix: '₹' },
  ];

  getTotal(data: number[]) {
    return data.reduce((a, b) => a + b, 0);
  }

  getAvg(data: number[]) {
    return Math.round(this.getTotal(data) / data.length);
  }

  getPeakIndex(data: number[]) {
    return data.indexOf(Math.max(...data));
  }

  isLow(value: number, data: number[]) {
    return value === Math.min(...data);
  }

  isPeak(value: number, data: number[]) {
    return value === Math.max(...data);
  }

  chartOptions: echarts.EChartsOption = {
    tooltip: { trigger: 'axis' },
    legend: { top: 'top', left: 'center' },
    xAxis: { type: 'category', data: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'], boundaryGap: false },
    yAxis: { type: 'value' },
    series: [
      { name: 'Sales', type: 'line', data: [120, 200, 150, 80, 70, 110], smooth: true, itemStyle: { color: '#5470c6' } },
      { name: 'Revenue', type: 'line', data: [90, 180, 130, 100, 60, 90], smooth: true, itemStyle: { color: '#91cc75' } },
    ]
  };

  ngOnInit(): void {
    const chartDom = document.getElementById('mainChart')!;
    this.chartInstance = echarts.init(chartDom);
    this.chartInstance.setOption(this.chartOptions);
    window.addEventListener('resize', () => this.chartInstance.resize());
  }
}