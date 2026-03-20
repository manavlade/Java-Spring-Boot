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
  @Input({ required: true }) type: 'line' | 'bar' | 'pie' = 'line';
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
        series: [{ type: 'pie', data: [
          { value: low, name: 'Low' },
          { value: medium, name: 'Medium' },
          { value: high, name: 'High' }
        ]}]
      };
    } else {
      option = {
        title: { text: `Age vs Salary (${this.type.toUpperCase()})` },
        xAxis: { type: 'category', data: this.ages },
        yAxis: { type: 'value' },
        series: [{ data: this.salaries, type: this.type, smooth: true }]
      };
    }
    this.chartInstance?.setOption(option);
  }
}

