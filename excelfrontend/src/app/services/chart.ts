import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DataPoint {
  age: number;
  salary: number;
}

@Injectable({
  providedIn: 'root',
})
export class Chart {

  private readonly apiURL = `${environment.apiUrl}/ChartData`

  constructor(private readonly http: HttpClient) { }

  getChartData(): Observable<DataPoint[]> {
    return this.http.get<DataPoint[]>(this.apiURL);
  }
}
