import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SignUpDTO {
  email: string;
  password: string;
}

@Injectable({
  providedIn: 'root',
})
export class Signup {
  
  private readonly apiURL = `${environment.apiUrl}/api/users`

  constructor(private readonly http: HttpClient) {}

  signUpEmployee(data: SignUpDTO): Observable<any>{
    const headers = { 'Content-Type': 'application/json' };
    return this.http.post(`${this.apiURL}`, data, { headers });
  }
}
