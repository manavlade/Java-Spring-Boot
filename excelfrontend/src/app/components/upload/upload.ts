import { Component, ElementRef, ViewChild, } from '@angular/core';
import { RowErrorResponse, UploadResponse } from '../../models/upload-response.model';
import { FileUpload } from '../../services/file-upload';
import { HttpErrorResponse } from '@angular/common/http';
import { HotToastService } from '@ngxpert/hot-toast';
import { CommonModule } from '@angular/common';


type UploadState = `idle` | 'uploading' | 'success' | 'rowErrors' | 'FileError' | 'downloaded' | 'saving';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule],
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
  reportBlob: Blob | null = null;

  private readonly MAX_FILE_SIZE = 5 * 1024 * 1024;
  private readonly VALID_EXTENSIONS = ['.xlsx', '.xls'];
  private readonly VALID_TYPES = [
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  ];

  constructor(
    private readonly uploadService: FileUpload,
    private readonly toast: HotToastService
  ) { }

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
      this.toast.error(error);
      return;
    }
    this.ClientError = null;
    this.selectedFile = file;
  }

  private validateClientSideUpload(file: File): string | null {
    const name = file.name.toLowerCase();
    const hasValidExtension = this.VALID_EXTENSIONS.some(ext => name.endsWith(ext));
    const hasValidType = this.VALID_TYPES.includes(file.type);

    if (!hasValidExtension || !hasValidType) {
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
    this.toast.info('File removed. You can select a new file to upload.');

    if (this.fileInputRef) {
      this.fileInputRef.nativeElement.value = '';
    }
  }

   upload(): void {
    if (!this.selectedFile) return;
    this.state = 'uploading';

    this.toast.loading('Generating report...', { id: 'upload-toast' });

    this.uploadService.uploadFile(this.selectedFile).subscribe({
      next: (blob: Blob) => {
        const originalName = this.selectedFile!.name.replace(/\.(xlsx|xls)$/i, '');
        this.reportBlob = blob;
        this.downloadedFileName = `${originalName}_report.xlsx`;
        this.state = 'downloaded';
        this.toast.close('upload-toast');
        this.toast.success('Report ready! Review it and confirm to save.');
      },
      error: (err: HttpErrorResponse) => this.handleUploadError(err)
    });
  }

  // ── Step 2: calls /upload → saves to DB ────────────────────────────────────
  confirmAndSave(): void {
    if (!this.selectedFile) return;
    this.state = 'saving';

    this.toast.loading('Saving to database...', { id: 'save-toast' });

    this.uploadService.saveFile(this.selectedFile).subscribe({
      next: (response: UploadResponse) => {
        this.successResponse = response;
        this.state = 'success';
        this.toast.close('save-toast');
        this.toast.success(
          `Saved! ${response.totalInserted} inserted, ${response.totalUpdated} updated.`
        );
      },
      error: (err: HttpErrorResponse) => {
        this.state = 'downloaded'; // go back to downloaded state so user can retry
        this.toast.close('save-toast');
        this.toast.error(err.error?.message ?? 'Save failed. Please try again.');
      }
    });
  }

  downloadReport(): void {
    if (!this.reportBlob || !this.downloadedFileName) return;
    this.triggerDownload(this.reportBlob, this.downloadedFileName);
    this.toast.success('Download started!', { id: 'download-toast' });
  }

  private triggerDownload(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    window.URL.revokeObjectURL(url);
  }

  private handleUploadError(err: HttpErrorResponse): void {
    if (err.error instanceof Blob) {
      const reader = new FileReader();
      reader.onload = () => {
        try {
          const json = JSON.parse(reader.result as string);
          this.fileErrorResponse = json.error ?? 'Upload failed. Please check your file.';
        } catch {
          this.fileErrorResponse = 'Upload failed. Please check your file.';
        }
        this.toast.error(this.fileErrorResponse!, { id: 'upload-toast' });
        this.state = 'FileError';
      };
      reader.readAsText(err.error);
    } else if (err.status === 413) {
      this.fileErrorResponse = 'File too large. Max allowed size is 5MB.';
      this.toast.error(this.fileErrorResponse, { id: 'upload-toast' });
      this.state = 'FileError';
    } else if (err.status === 0) {
      this.fileErrorResponse = 'Cannot reach the server. Make sure Spring Boot is running on port 8080.';
      this.toast.error(this.fileErrorResponse, { id: 'upload-toast' });
      this.state = 'FileError';
    } else {
      this.fileErrorResponse = 'Something went wrong. Please try again.';
      this.toast.error(this.fileErrorResponse, { id: 'upload-toast' });
      this.state = 'FileError';
    }
  }

  reset(): void {
    this.state = 'idle';
    this.selectedFile = null;
    this.ClientError = null;
    this.successResponse = null;
    this.rowErrorResponse = null;
    this.fileErrorResponse = null;
    this.reportBlob = null;
    this.downloadedFileName = null;
    if (this.fileInputRef) {
      this.fileInputRef.nativeElement.value = '';
    }
  }

  formatBytes(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  }

}
