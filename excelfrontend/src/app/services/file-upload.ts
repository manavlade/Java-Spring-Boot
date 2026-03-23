import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { UploadResponse } from '../models/upload-response.model';

@Injectable({
  providedIn: 'root',
})

export class FileUpload {

  private readonly apiURL = `${environment.apiUrl}/upload/report`;
  private readonly saveURL = `${environment.apiUrl}/upload`;

  constructor(private readonly http: HttpClient) { }

  uploadFile(file: File): Observable<Blob> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post(this.apiURL, formData, {
      responseType: 'blob'
    }).pipe(
      catchError(this.handleError)
    );
  }
    saveFile(file: File): Observable<UploadResponse> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<UploadResponse>(this.saveURL, formData).pipe(
      catchError(this.handleError)
    );
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    return throwError(() => error);
  }
}
