import {
  Component,
  ElementRef,
  Input,
  OnChanges,
  SimpleChanges,
  ViewChild,
  AfterViewInit
} from '@angular/core';

import * as eCharts from "echarts";
import { SalaryChartResponseDTO } from '../../services/chart';

@Component({
  selector: 'app-echarts-graph',
  standalone: true,
  imports: [],
  templateUrl: './echarts-graph.html',
  styleUrl: './echarts-graph.css',
})
export class EchartsGraph implements OnChanges, AfterViewInit {

  @Input({ required: true }) type: 'line' | 'bar' | 'pie' | 'salary-range' = 'line';

  @Input() salaryData?: SalaryChartResponseDTO;

  private get derivedSalaries(): number[] {
    if (!this.salaryData) return [];
    const out: number[] = [];
    for (const range of this.salaryData.ranges)
      for (const b of range.buckets)
        for (let i = 0; i < b.employeeCount; i++) out.push(b.bucketStart);
    return out;
  }

  private get derivedAges(): number[] {
    if (!this.salaryData) return [];
    const out: number[] = [];
    for (const range of this.salaryData.ranges)
      for (const b of range.buckets)
        for (let i = 0; i < b.employeeCount; i++) out.push(Math.round(b.averageAge));
    return out;
  }

  @ViewChild('chartContainer', { static: true }) chartContainer!: ElementRef;

  private chartInstance?: eCharts.ECharts;

  get statCards(): { label: string; value: string; sub: string }[] {
    if (!this.salaryData) return [];
    const d = this.salaryData;

    const largestRange = d.ranges.find(r => r.rangeLabel === d.largestRange);

    let topBucketCount = 0;
    for (const range of d.ranges) {
      const match = range.buckets.find(b => b.bucketLabel === d.highestDensityBucket);
      if (match) { topBucketCount = match.employeeCount; break; }
    }

    return [
      {
        label: 'Total employees',
        value: String(d.totalEmployees),
        sub: 'across all ranges',
      },
      {
        label: 'Overall avg salary',
        value: this.fmtCurrency(d.overallAverageSalary),
        sub: `range ${this.fmtK(d.minSalary)} – ${this.fmtK(d.maxSalary)}`,
      },
      {
        label: 'Largest range',
        value: d.largestRange,
        sub: `${largestRange?.totalEmployees ?? '—'} employees`,
      },
      {
        label: 'Top density bucket',
        value: d.highestDensityBucket,
        sub: `${topBucketCount} employees`,
      },
    ];
  }

  fmtCurrency(n: number): string {
    return '$' + Math.round(n).toLocaleString();
  }

  fmtK(n: number): string {
    return '$' + (n >= 1000 ? (n / 1000).toFixed(1).replace(/\.0$/, '') + 'k' : n);
  }

  ngAfterViewInit() {
    this.chartInstance = eCharts.init(this.chartContainer.nativeElement);
    this.renderChart();
    window.addEventListener('resize', () => this.chartInstance?.resize());
  }

    ngOnChanges(changes: SimpleChanges) {
      if (this.chartInstance) this.renderChart();
    }

  private renderChart() {
    let option: any;

    if (this.type === 'pie') {
      const salaries = this.derivedSalaries;
      const low = salaries.filter(s => s < 30000).length;
      const medium = salaries.filter(s => s >= 30000 && s < 70000).length;
      const high = salaries.filter(s => s >= 70000).length;
      option = {
        title: { text: 'Salary Distribution', left: 'center' },
        series: [{
          type: 'pie', data: [
            { value: low, name: 'Low' },
            { value: medium, name: 'Medium' },
            { value: high, name: 'High' },
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
        xAxis: { type: 'category', data: this.derivedAges },
        yAxis: { type: 'value' },
        series: [{ data: this.derivedSalaries, type: this.type, smooth: true }]
      };
    }

    this.chartInstance?.setOption(option, true);
  }

  private buildSalaryRangeOption(): any {
    if (!this.salaryData) return {};

    const ranges = this.salaryData.ranges;
    const xLabels = ranges.map(r => r.rangeLabel);
    const NORMALIZED_MAX = 100;

    const CHART_DRAW_H = 400 - 50 - 75;

    const LINE_H_SALARY = 18;
    const LINE_H_COUNT = 15;
    const LINE_H_AGE = 13;
    const LINE_H_SMALL = 14;
    const LABEL_PADDING = 6;

    const THRESH_3_LINE = LINE_H_SALARY + LINE_H_COUNT + LINE_H_AGE + LABEL_PADDING;
    const THRESH_2_LINE = LINE_H_SALARY + LINE_H_COUNT + LABEL_PADDING;
    const THRESH_1_LINE = LINE_H_SMALL + LABEL_PADDING;

    const maxEmployees = Math.max(...ranges.map(r => r.totalEmployees));

    const series: any[] = [];

    ranges.forEach((range, ri) => {
      range.buckets.forEach((bucket, bi) => {
        const segH = (bucket.percentage / 100) * CHART_DRAW_H;

        const labelFormatter = (): string => {
          if (bucket.employeeCount < 1) return '';
          if (segH >= THRESH_3_LINE) {
            return [
              `{salary|${bucket.bucketLabel}}`,
              `{count|${bucket.employeeCount} emp · ${bucket.percentage}%}`,
              `{age|avg age ${Math.round(bucket.averageAge)} yrs}`,
            ].join('\n');
          }
          if (segH >= THRESH_2_LINE) {
            return [
              `{salary|${bucket.bucketLabel}}`,
              `{count|${bucket.employeeCount} emp · ${bucket.percentage}%}`,
            ].join('\n');
          }
          if (segH >= THRESH_1_LINE) {
            return `{small|${bucket.bucketLabel}: ${bucket.employeeCount}}`;
          }
          return '';
        };

        const data: (number | null)[] = ranges.map((_, i) =>
          i === ri ? bucket.percentage : null
        );

        series.push({
          name: `${range.rangeLabel}__${bucket.bucketLabel}`,
          type: 'bar',
          stack: `stack_${ri}`,
          barWidth: "50%",
          barGap: '0%',
          barCategoryGap: '0%',
          data,
          itemStyle: { color: this.getColor(ri, bi) },

          label: {
            show: true,
            position: 'inside',
            formatter: labelFormatter,
            rich: {
              salary: { fontSize: 12, fontWeight: 'bold', color: 'rgba(255,255,255,0.96)', lineHeight: 18 },
              count: { fontSize: 10, fontWeight: 'normal', color: 'rgba(255,255,255,0.80)', lineHeight: 15 },
              age: { fontSize: 9, fontWeight: 'normal', color: 'rgba(255,255,255,0.58)', lineHeight: 13 },
              small: { fontSize: 10, fontWeight: 'bold', color: 'rgba(255,255,255,0.92)', lineHeight: 14 },
            },
          },

          emphasis: { focus: 'series' },
        });
      });
    });

    return {
      title: {
        text: 'Salary Range Breakdown',
        left: 'center',
        textStyle: { fontSize: 14, fontWeight: 'bold', color: '#222' },
      },

      tooltip: {
        trigger: 'item',
        formatter: (params: any) => {
          if (!this.salaryData) return '';
          const [rangeLabel, bucketLabel] = params.seriesName.split('__');
          const range = this.salaryData.ranges.find(r => r.rangeLabel === rangeLabel);
          const bucket = range?.buckets.find(b => b.bucketLabel === bucketLabel);
          if (!bucket) return '';
          return `
            <div style="font-size:13px;font-weight:600;margin-bottom:4px">${bucket.bucketLabel} bucket</div>
            <div style="font-size:11px;color:#888;margin-bottom:6px">Range: ${rangeLabel}</div>
            <table style="font-size:12px;border-collapse:collapse">
              <tr><td style="color:#999;padding:2px 10px 2px 0">Employees</td><td><b>${bucket.employeeCount}</b></td></tr>
              <tr><td style="color:#999;padding:2px 10px 2px 0">% of range</td><td><b>${bucket.percentage}%</b></td></tr>
              <tr><td style="color:#999;padding:2px 10px 2px 0">Salary window</td>
                  <td><b>${this.fmtSalary(bucket.bucketStart)} – ${this.fmtSalary(bucket.bucketEnd)}</b></td></tr>
              <tr><td style="color:#999;padding:2px 10px 2px 0">Avg age</td><td><b>${Math.round(bucket.averageAge)} yrs</b></td></tr>
            </table>`;
        },
        backgroundColor: '#fff',
        borderColor: '#e0e0e0',
        borderWidth: 1,
        padding: 12,
        extraCssText: 'box-shadow:0 4px 16px rgba(0,0,0,0.12);border-radius:8px;',
      },

      legend: { show: false },

      grid: { top: 45, bottom: 65, left: 50, right: 20, containLabel: true },

      xAxis: {
        type: 'category',
        boundaryGap: true,
        data: xLabels,
        axisTick: { show: false },
        axisLine: { lineStyle: { color: '#e0e0e0' } },
        axisLabel: {
          interval: 0,
          formatter: (value: string) => {
            const range = ranges.find(r => r.rangeLabel === value);
            const pct = range ? Math.round(range.percentageOfTotal) : 0;
            return `{label|${value}}\n{pct|${pct}% of total}`;
          },
          rich: {
            label: {
              fontSize: 12,
              fontWeight: 'bold',
              color: '#555',
              lineHeight: 20,
            },
            pct: {
              fontSize: 10,
              fontWeight: 'normal',
              color: '#aaa',
              lineHeight: 16,
            },
          },
        },
      },

      yAxis: {
        type: 'value',
        max: NORMALIZED_MAX,
        axisLabel: {
          color: '#000000',
          fontSize: 11,
          formatter: (value: number) => {
            if (value === 0) return '0';
            const approxCount = Math.round((value / 100) * maxEmployees);
            return String(approxCount);
          },
        },
        splitLine: { lineStyle: { color: '#f0f0f0' } },
        axisTick: { show: false },
        name: 'Employees',
        nameTextStyle: { color: '#aaa', fontSize: 11 },
      },

      series,
    };
  }

  private getColor(rangeIndex: number, bucketIndex: number): string {
    const COLOR_SETS = [
      ['#0d3b6e', '#1a4f8a', '#2270c4', '#4e9ae8'],
      ['#073d2a', '#107253', '#2da87e', '#5bbf9a'],
      ['#4a2700', '#7a4400', '#c06e00', '#e8a94a'],
      ['#3d1a78', '#5b2db8', '#7a4be8', '#a78bfa'],
    ];
    const palette = COLOR_SETS[rangeIndex % COLOR_SETS.length];
    return palette[bucketIndex % palette.length];
  }

  private fmtSalary(n: number): string {
    if (n >= 100_000) return '₹' + (n / 100_000).toFixed(1) + 'L';
    return '₹' + (n / 1_000).toFixed(0) + 'k';
  }
}