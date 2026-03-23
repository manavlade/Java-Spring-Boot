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

  chartTypes: ('line' | 'bar' | 'pie' | 'salary-range')[] = ['line', 'bar', 'pie', 'salary-range'];

  activeChart: 'line' | 'bar' | 'pie' | 'salary-range' = 'line';
  ages: number[] = [];
  salaries: number[] = [];

  constructor(private readonly chartService: Chart) {
    this.chartService.getChartData().subscribe(data => {
      this.ages = data.map(d => d.age);
      this.salaries = data.map(d => d.salary);
    });
  }
}
