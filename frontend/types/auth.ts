export interface User {
  id: string;
  email: string;
  fullName?: string;
  organizationId: string;
  roles?: string[];
}

export interface AuthResponse {
  token: string;
  refreshToken?: string;
  user: User;
}

export interface LoginRequest {
  email: string;
  password?: string;
}

export interface RegisterRequest {
  email: string;
  password?: string;
  fullName: string;
  orgName: string;
}

export interface ApiKey {
  id: string;
  name: string;
  keyPrefix: string;
  organizationId: string;
  createdAt: string;
  expiresAt?: string;
  lastUsedAt?: string;
}

export interface CreateApiKeyRequest {
  name: string;
  expiresAt?: string;
}

export interface CreateApiKeyResponse {
  id: string;
  name: string;
  apiKey: string;
  createdAt: string;
}
