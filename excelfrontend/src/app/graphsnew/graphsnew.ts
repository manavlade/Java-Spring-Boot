import { Component, Input } from '@angular/core';
import { ChartService, SalaryChartResponseDTO } from '../services/chart';

@Component({
  selector: 'app-graphsnew',
  imports: [],
  templateUrl: './graphsnew.html',
  styleUrl: './graphsnew.css',
})
export class Graphsnew {
  constructor(private dataService: ChartService) { }

  @Input() salaryData?: SalaryChartResponseDTO;

  option = {

    xAxis: {
      type: 'sal category'
    }
  }

  
}
