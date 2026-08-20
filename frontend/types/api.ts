export interface ApiResponse<T> {
  data: T;
  message?: string;
  timestamp?: string;
}

export interface ApiError {
  message: string;
  status: number;
  timestamp?: string;
  errors?: Record<string, string>;
}
