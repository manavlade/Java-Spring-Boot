import { Component } from '@angular/core';
import { HlmTabsImports } from "@spartan-ng/helm/tabs";
import { Chart, DataPoint } from '../../services/chart';

import * as eCharts from "echarts";


@Component({
  selector: 'app-dashboard',
  imports: [HlmTabsImports],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {

  constructor( private readonly chartService: Chart) {}

  ngAfterViewInit(): void {
    const charDom = document.getElementById('chart');
    const myChart = eCharts.init(charDom);

    this.chartService.getChartData().subscribe((response: DataPoint[]) =>{
      const ages = response.map(d => d.age);
      const salaries = response.map(d => d.salary);

      const option = {
        title: {
          text: 'Age vs Salary'
        },
        tooltip: {
          trigger: 'axis'
        },
        xAxis: {
          type: 'category',
          name: 'Age',
          data: ages
        },
        yAxis: {
          type: 'value',
          name: 'Salary'
        },
        series: [{
          data: salaries,
          type: 'line',
          smooth: true
        }]
      };

      myChart.setOption(option);
    })
  }
}
