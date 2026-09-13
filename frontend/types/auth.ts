export interface User {
  id: string;
  email: string;
  firstName?: string;
  lastName?: string;
  fullName?: string;
  organizationId: string;
  organizationName?: string;
  role?: string;
  roles?: string[];
}

export interface AuthResponse {
  accessToken: string;
  token?: string;
  refreshToken?: string;
  tokenType?: string;
  expiresIn?: number;
  user: User;
}

export interface LoginRequest {
  email: string;
  password?: string;
}

export interface RegisterRequest {
  email: string;
  password?: string;
  firstName: string;
  lastName: string;
  organizationName: string;
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
