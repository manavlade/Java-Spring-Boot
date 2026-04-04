import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment.development';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LoginDTO {
  email: string;
  password: string;
}

@Injectable({
  providedIn: 'root',
})
export class LoginService {

  private readonly apiURL = `${environment.apiUrl}/api/users`

  constructor(private readonly http: HttpClient) { }

  loginEmployee(data: LoginDTO): Observable<any> {
    const headers = { 'Content-Type': 'application/json' };
    return this.http.post(`${this.apiURL}`, data, { headers });
  }
}
