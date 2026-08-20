import { apiClient } from '@/lib/axios';
import { AuthResponse, LoginRequest, RegisterRequest, ApiKey, CreateApiKeyRequest, CreateApiKeyResponse } from '@/types/auth';

export const authService = {
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const res = await apiClient.post<AuthResponse>('/api/v1/auth/login', data);
    return res.data;
  },

  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const res = await apiClient.post<AuthResponse>('/api/v1/auth/register', data);
    return res.data;
  },

  refreshToken: async (refreshToken: string): Promise<AuthResponse> => {
    const res = await apiClient.post<AuthResponse>('/api/v1/auth/refresh', { refreshToken });
    return res.data;
  },

  listApiKeys: async (): Promise<ApiKey[]> => {
    const res = await apiClient.get<ApiKey[]>('/api/v1/api-keys');
    return res.data;
  },

  createApiKey: async (data: CreateApiKeyRequest): Promise<CreateApiKeyResponse> => {
    const res = await apiClient.post<CreateApiKeyResponse>('/api/v1/api-keys', data);
    return res.data;
  },

  revokeApiKey: async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/api-keys/${id}`);
  },
};
