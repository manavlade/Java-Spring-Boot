import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SalaryBucketDTO {
  bucketLabel: string;
  bucketStart: number;
  bucketEnd: number;
  employeeCount: number;
  percentage: number
  mainRange: string
  averageAge: number;
}

export interface SalaryRangeDTO {
  rangeLabel: string;
  totalEmployees: number;
  averageSalary: number;
  percentageOfTotal: number;
  buckets: SalaryBucketDTO[];
}

export interface SalaryChartResponseDTO {
  totalEmployees: number;
  overallAverageSalary: number;
  minSalary: number;
  maxSalary: number;
  highestDensityBucket: string;
  largestRange: string;
  ranges: SalaryRangeDTO[];
  averageAge: number;
  minAge: number;
  maxAge: number;
}

@Injectable({
  providedIn: 'root',
})
export class ChartService {

  private readonly apiURL = `${environment.apiUrl}/ChartData`

  constructor(private readonly http: HttpClient) { }

  getChartData(): Observable<SalaryChartResponseDTO> {
    return this.http.get<SalaryChartResponseDTO>(this.apiURL);
  }
}
