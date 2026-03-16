export interface UploadResponse {
    message: string;
    totalInserted: number;
    totalUpdated: number;
    totalProcessed: number;
    warnings?: string[];
}

export interface RowErrorResponse{
    totalErrors: number;
    errors: string[];
}

export interface FileErrorResponse{
    error: string;
}