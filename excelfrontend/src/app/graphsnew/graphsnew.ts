import { AfterViewInit, Component } from '@angular/core';
import { ChartService, SalaryChartResponseDTO } from '../services/chart';

import * as echarts from 'echarts/core';
import { GridComponent, GridComponentOption } from 'echarts/components';
import { BarChart, BarSeriesOption } from 'echarts/charts';
import { CanvasRenderer } from 'echarts/renderers';
import { LabelLayout } from 'echarts/features';

echarts.use([GridComponent, BarChart, CanvasRenderer]);

type EChartsOption = echarts.ComposeOption<GridComponentOption | BarSeriesOption>;

@Component({
  selector: 'app-graphsnew',
  imports: [],
  templateUrl: './graphsnew.html',
  styleUrl: './graphsnew.css',
  standalone: true
})

export class Graphsnew implements AfterViewInit {

  constructor(private readonly dataService: ChartService) { }

  salaryData?: SalaryChartResponseDTO;

  ngAfterViewInit(): void {
    this.dataService.getChartData().subscribe((data) => {
      this.salaryData = data;
      this.initChart();
    })
  }
  get statCards() {
    return [
      {
        label: 'Total Employees',
        value: this.salaryData?.totalEmployees ?? 0,
        sub: 'Active workforce'
      },
      {
        label: 'Avg. Salary',
        value: this.salaryData?.overallAverageSalary ?? 'N/A',
        sub: 'Company wide'
      },
      {
        label: 'Highest Density',
        value: this.salaryData?.highestDensityBucket ?? 'N/A',
        sub: 'Most common range'
      },
      {
        label: 'Largest Range',
        value: this.salaryData?.largestRange ?? 'N/A',
        sub: 'Salary spread'
      }
    ];
  }


  initChart() {
    if(!this.salaryData) return;
    const chartDom = document.getElementById('main')!;
    const myChart = echarts.init(chartDom);

    type BarData = {
      value: number;
      percentage: number;
      mainRange: string;
      itemStyle?: { color: string };
    };


    const xLables: string[] = [];
    const dynamicSeries: BarData[][] = [];
    const seriesA: BarData[] = []; // employee count karne ke liye array abhi hardcoded hai
    const seriesB: BarData[] = [];
    const seriesC: BarData[] = [];

    const COLOR_SETS = [
      ['#0d3b6e', '#1a4f8a', '#2270c4', '#4e9ae8'],
      ['#073d2a', '#107253', '#2da87e', '#5bbf9a'],
      ['#4a2700', '#7a4400', '#c06e00', '#e8a94a'],
      ['#3d1a78', '#5b2db8', '#7a4be8', '#a78bfa'],
    ];

    this.salaryData?.ranges?.forEach((range, i) => {
      xLables.push(range.rangeLabel)

      const colorSet = COLOR_SETS[i % COLOR_SETS.length];

      const bucket = range.buckets;

      seriesA.push({
        value: bucket[0]?.employeeCount ?? 0,
        percentage: bucket[0]?.percentage ?? 0,
        mainRange: bucket[0]?.bucketLabel ?? '',
        itemStyle: { color: colorSet[0] }
      });

      seriesB.push({
        value: bucket[1]?.employeeCount ?? 0,
        percentage: bucket[1]?.percentage ?? 0,
        mainRange: bucket[1]?.bucketLabel ?? '',
        itemStyle: { color: colorSet[1] }
      });

      seriesC.push({
        value: bucket[2]?.employeeCount ?? 0,
        percentage: bucket[2]?.percentage ?? 0,
        mainRange: bucket[2]?.bucketLabel ?? '',
        itemStyle: { color: colorSet[2] }
      });
    });

    let series: any[] = [];

    series = [
      {
        data: seriesA,
        type: 'bar',
        stack: 'a',
        name: 'a',
        label: {
          show: true,
          position: 'inside',
          formatter: (params: any) => {
            const data = params.data as BarData;
            return `Employee Count:${data.value} \n Salary:${data.mainRange}`;
          }
        }
      },
      {
        data: seriesB,
        type: 'bar',
        stack: 'a',
        name: 'b',
        label: {
          show: true,
          position: 'inside',
          formatter: (params: any) => {
            const data = params.data as BarData;
            return ` Employee Count ${data.value} \n Salary ${data.mainRange}`;
          }
        }
      },
      {
        data: seriesC,
        type: 'bar',
        stack: 'a',
        name: 'c',
        label: {
          show: true,
          position: 'inside',
          formatter: (params: any) => {
            const data = params.data as BarData;
            return `Employee Count ${data.value} \n Salary ${data.mainRange}`;
          }
        }
      },
    ];
    const option: EChartsOption = {
      title: {
        left: 'center',
        text: "Employee Salary Insights Dashboard"
      },
      axisLabel: {
        color: "#040404"
      },
      legend: {
        data: ['20k-60k', '60k-100k', '100k+']
      },
      xAxis: {
        type: 'category',
        data: xLables,
        name: "Salary Ranges",
        nameTextStyle: {
          color: "#0f0e0e"
        },
        tooltip: {

        },
        axisLabel: {
          color: "#040404",
          formatter: (value, index) => {
            const percentage = this.salaryData?.ranges[index]?.percentageOfTotal ?? 0;
            return `${value}\n${percentage}% of Total`;
          },
          lineHeight: 20,
          rich: {
            percentage: {
              fontSize: 10,
              color: '#0f0e0e',
              align: 'center'
            }
          }
        }
      },
      yAxis: {
        type: 'value',
        name: "employee count",
        nameTextStyle: {
          color: "#040404"
        },
        axisLabel: {
          color: "#040404"
        }
      },
      series
    };
    myChart.setOption(option);

  }
}
