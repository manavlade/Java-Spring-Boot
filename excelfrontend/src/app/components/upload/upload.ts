import { Component, ElementRef, ViewChild, } from '@angular/core';
import { FileErrorResponse, RowErrorResponse, UploadResponse } from '../../models/upload-response.model';
import { FileUpload } from '../../services/file-upload';
import { HttpErrorResponse } from '@angular/common/http';


type UploadState = `idle` | 'uploading' | 'success' | 'rowErrors' | 'FileError'| 'downloaded';

@Component({
  selector: 'app-upload',
  imports: [],
  templateUrl: './upload.html',
  styleUrl: './upload.css',
})
export class Upload {

  @ViewChild('fileInput') fileInputRef !: ElementRef<HTMLInputElement>;

  state: UploadState = 'idle';
  selectedFile: File | null = null;
  isDragOver = false;
  ClientError: string | null = null;
  successResponse: UploadResponse | null = null;
  rowErrorResponse: RowErrorResponse | null = null;
  fileErrorResponse: string | null = null;
  downloadedFileName: string | null = null;

  private readonly MAX_FILE_SIZE = 5 * 1024 * 1024;
  private readonly VALID_EXTENSIONS = ['.xlsx', 'xls'];
  private readonly VALID_TYPES = [
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  ];

  constructor(private readonly uploadService: FileUpload) { }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = true;
  }

  onDragLeave(): void {
    this.isDragOver = false;
  }
  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = false;
    const file = event.dataTransfer?.files[0];

    if (file) this.handleFileSelect(file);
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (file) this.handleFileSelect(file);
  }

  private handleFileSelect(file: File): void {
    const error = this.validateClientSideUpload(file);

    if (error) {
      this.ClientError = error;
      this.selectedFile = null;
      return;
    }
    this.ClientError = null;
    this.selectedFile = file;
  }

  private validateClientSideUpload(file: File): string | null {
    const name = file.name.toLowerCase();
    const hasValidExtension = this.VALID_EXTENSIONS.some(ext => name.endsWith(ext));
    const hasValidType = this.VALID_TYPES.includes(file.type);

    if (!hasValidExtension && !hasValidType) {
      return 'Invalid file type. Only .xlsx and .xls files are accepted.'
    }

    if (file.size === 0) {
      return "File is empty. Please upload a valid Excel file with data.";
    }
    if (file.size > this.MAX_FILE_SIZE) {
      return `Uploaded file exceeds the limited size of ${this.MAX_FILE_SIZE}`;
    }
    return null;
  }

  removeFile(): void {
    this.selectedFile = null;
    this.ClientError = null;

    if (this.fileInputRef) {
      this.fileInputRef.nativeElement.value = '';
    }
  }

    upload(): void {
    if (!this.selectedFile) return;
    this.state = 'uploading';

    this.uploadService.uploadFile(this.selectedFile).subscribe({
      next: (blob: Blob) => {
        const originalName = this.selectedFile!.name.replace(/\.(xlsx|xls)$/i, '');
        const reportName = `${originalName}_report.xlsx`;
        this.triggerDownload(blob, reportName);
        this.downloadedFileName = reportName;
        this.state = 'downloaded';
      },
      error: (err: HttpErrorResponse) => {
        if (err.error instanceof Blob) {
          const reader = new FileReader();
          reader.onload = () => {
            try {
              const json = JSON.parse(reader.result as string);
              this.fileErrorResponse = json.error ?? 'Upload failed. Please check your file.';
            } catch {
              this.fileErrorResponse = 'Upload failed. Please check your file.';
            }
            this.state = 'FileError';
          };
          reader.readAsText(err.error);
        } else if (err.status === 413) {
          this.fileErrorResponse = 'File too large. Max allowed size is 5MB.';
          this.state = 'FileError';
        } else if (err.status === 0) {
          this.fileErrorResponse = 'Cannot reach the server. Make sure Spring Boot is running on port 8080.';
          this.state = 'FileError';
        } else {
          this.fileErrorResponse = 'Something went wrong. Please try again.';
          this.state = 'FileError';
        }
      }
    });
  }


  private triggerDownload(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    window.URL.revokeObjectURL(url);
  }

  reset(): void {
    this.state = 'idle';
    this.selectedFile = null;
    this.ClientError = null;
    this.successResponse = null;
    this.rowErrorResponse = null;
    this.fileErrorResponse = null;

    if(this.fileInputRef){
      this.fileInputRef.nativeElement.value = '';
    }
  }

    formatBytes(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  }

}
