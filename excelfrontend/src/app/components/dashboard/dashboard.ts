import { Component } from '@angular/core';
import { HlmTabsImports } from "@spartan-ng/helm/tabs";
import { Chart } from '../../services/chart';
import { EchartsGraph } from '../echarts-graph/echarts-graph';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [HlmTabsImports, EchartsGraph],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {

  chartTypes: ('line' | 'bar' | 'pie')[] = ['line', 'bar', 'pie'];

  activeChart: 'line' | 'bar' | 'pie' = 'line';
  ages: number[] = [];
  salaries: number[] = [];

  constructor(private readonly chartService: Chart) {
    this.chartService.getChartData().subscribe(data => {
      this.ages = data.map(d => d.age);
      this.salaries = data.map(d => d.salary);
    });
  }
}


// export class Dashboard {

//   activeChart: 'line' | 'bar' | 'pie' = 'line';

//   constructor(private readonly chartService: Chart) { }

//   setChart(type: 'line' | 'bar' | 'pie') {
//     this.activeChart = type;
//   }

//   ngAfterViewInit(): void {
//     const lineDom = document.getElementById('lineChart');
//     const barDom = document.getElementById('barChart');

//     const lineChart = eCharts.init(lineDom!);
//     const barChart = eCharts.init(barDom!);

//     const pieDom = document.getElementById('pieChart');
//     const pieChart = eCharts.init(pieDom!);
//     this.chartService.getChartData().subscribe((response: DataPoint[]) => {

//       const ages = response.map(d => d.age);
//       const salaries = response.map(d => d.salary);

//       let low = 0, medium = 0, high = 0;

//       salaries.forEach(s => {
//         if (s < 30000) low++;
//         else if (s < 70000) medium++;
//         else high++;
//       });

//       const lineOption = {
//         title: {
//           text: 'Age vs Salary (Line Chart)'
//         },
//         tooltip: {
//           trigger: 'axis'
//         },
//         xAxis: {
//           type: 'category',
//           name: 'Age',
//           data: ages
//         },
//         yAxis: {
//           type: 'value',
//           name: 'Salary'
//         },
//         series: [
//           {
//             data: salaries,
//             type: 'line',
//             smooth: true
//           }
//         ]
//       };

//       // 🔹 BAR CHART OPTION
//       const barOption = {
//         title: {
//           text: 'Age vs Salary (Bar Chart)'
//         },
//         tooltip: {
//           trigger: 'axis'
//         },
//         xAxis: {
//           type: 'category',
//           name: 'Age',
//           data: ages
//         },
//         yAxis: {
//           type: 'value',
//           name: 'Salary'
//         },
//         series: [
//           {
//             data: salaries,
//             type: 'bar',
//           }
//         ]
//       };

//       const pieOption = {
//         title: {
//           text: 'Salary Distribution',
//           left: 'center'
//         },
//         tooltip: {
//           trigger: 'item'
//         },
//         series: [
//           {
//             type: 'pie',
//             radius: '50%',
//             data: [
//               { value: low, name: 'Low' },
//               { value: medium, name: 'Medium' },
//               { value: high, name: 'High' }
//             ]
//           }
//         ]
//       };

//       pieChart.setOption(pieOption);

//       lineChart.setOption(lineOption);
//       barChart.setOption(barOption);

//       window.addEventListener('resize', () => {
//         lineChart.resize();
//         barChart.resize();
//         pieChart.resize();
//       });
//     });
//   }
// }