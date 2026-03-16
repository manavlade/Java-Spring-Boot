import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';
import { UploadResponse } from '../models/upload-response.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class FileUpload {

  private readonly apiURL = `${environment.apiUrl}/upload`;

  constructor (private readonly http: HttpClient) { }

  uploadFile(file: File): Observable<UploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<UploadResponse>(this.apiURL, formData).pipe(
      catchError(this.handleError)
    )
  };

  private handleError(error: HttpErrorResponse): Observable<never>{
    return throwError(() => error);
  }
}
