import { Component } from '@angular/core';
import { ChartService, SalaryChartResponseDTO } from '../../services/chart';
import { EchartsGraph } from '../echarts-graph/echarts-graph';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [ EchartsGraph],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {

  chartTypes: ('line' | 'bar' | 'pie' | 'salary-range')[] = ['line', 'bar', 'pie', 'salary-range'];

  activeChart: 'line' | 'bar' | 'pie' | 'salary-range' = 'line';

  dashboardData?: SalaryChartResponseDTO;

  constructor(private readonly chartService: ChartService) {
    this.chartService.getChartData().subscribe(data => {
      this.dashboardData = data;
    });
  }
}
