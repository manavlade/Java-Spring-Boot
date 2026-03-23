import { Component, ElementRef, Input, OnChanges, SimpleChanges, ViewChild } from '@angular/core';
import * as eCharts from "echarts";


@Component({
  selector: 'app-echarts-graph',
  standalone: true,
  imports: [],
  templateUrl: './echarts-graph.html',
  styleUrl: './echarts-graph.css',
})


export class EchartsGraph implements OnChanges {
  @Input({ required: true }) type: 'line' | 'bar' | 'pie' | 'salary-range' = 'line';
  @Input() ages: number[] = [];
  @Input() salaries: number[] = [];

  @ViewChild('chartContainer', { static: true }) chartContainer!: ElementRef;
  private chartInstance?: eCharts.ECharts;

  ngOnChanges(changes: SimpleChanges) {
    if (this.chartInstance) {
      this.renderChart();
    }
  }

  ngAfterViewInit() {
    this.chartInstance = eCharts.init(this.chartContainer.nativeElement);
    this.renderChart();
    window.addEventListener('resize', () => this.chartInstance?.resize());
  }

  private renderChart() {
    let option: any;

    if (this.type === 'pie') {
      const low = this.salaries.filter(s => s < 30000).length;
      const medium = this.salaries.filter(s => s >= 30000 && s < 70000).length;
      const high = this.salaries.filter(s => s >= 70000).length;

      option = {
        title: { text: 'Salary Distribution', left: 'center' },
        series: [{
          type: 'pie', data: [
            { value: low, name: 'Low' },
            { value: medium, name: 'Medium' },
            { value: high, name: 'High' }
          ]
        }]
      };
    }
    else if (this.type === 'salary-range') {
      option = this.buildSalaryRangeOption();

    }
    else {
      option = {
        title: { text: `Age vs Salary (${this.type.toUpperCase()})` },
        xAxis: { type: 'category', data: this.ages },
        yAxis: { type: 'value' },
        series: [{ data: this.salaries, type: this.type, smooth: true }]
      };
    }
    this.chartInstance?.setOption(option);
  }

  private buildSalaryRangeOption(): any {
    const RANGES = [
      { label: '40k – 60k', min: 40000, max: 60000 },
      { label: '60k – 80k', min: 60000, max: 80000 },
      { label: '80k – Max', min: 80000, max: Infinity },
    ];

    const buckets: Record<number, Record<number, number>> = { 0: {}, 1: {}, 2: {} };
    this.salaries.forEach(s => {
      RANGES.forEach((r, i) => {
        if (s >= r.min && s < r.max) {
          buckets[i][s] = (buckets[i][s] || 0) + 1;
        }
      });
    });

    const uniqueSalaries = [...new Set(this.salaries)].sort((a, b) => a - b);

    const shades: Record<number, string[]> = {
      0: ['#B5D4F4', '#85B7EB', '#378ADD', '#185FA5', '#0C447C'],
      1: ['#9FE1CB', '#5DCAA5', '#1D9E75', '#0F6E56', '#085041'],
      2: ['#FAC775', '#EF9F27', '#BA7517', '#854F0B', '#633806'],
    };
    const shadeCount: Record<number, number> = { 0: 0, 1: 0, 2: 0 };

    const series = uniqueSalaries.map(salary => {
      const ri = RANGES.findIndex(r => salary >= r.min && salary < r.max);
      const color = ri >= 0
        ? shades[ri][Math.min(shadeCount[ri]++, shades[ri].length - 1)]
        : '#D3D1C7';

      return {
        name: this.fmtSalary(salary),
        type: 'bar',
        stack: 'total',
        itemStyle: { color },
        emphasis: { focus: 'series' },
        data: RANGES.map((_, i) => buckets[i][salary] || 0),
      };
    });

    return {
      title: { text: 'Salary Range Breakdown', left: 'center' },
      tooltip: {
        trigger: 'item',
        formatter: (params: any) =>
          `<b>${params.seriesName}</b><br/>
           Range: ${RANGES[params.dataIndex].label}<br/>
           Employees: ${params.value}`
      },
      legend: { show: false },
      grid: { top: 50, bottom: 60, left: 60, right: 20 },
      xAxis: {
        type: 'category',
        data: RANGES.map(r => r.label),
        axisTick: { show: false },
      },
      yAxis: {
        type: 'value',
        name: 'Employees',
        minInterval: 1,
      },
      series,
    };
  }

  private fmtSalary(n: number): string {
    if (n >= 100_000) return '₹' + (n / 100_000).toFixed(1) + 'L';
    return '₹' + (n / 1_000).toFixed(0) + 'k';
  }

}