import axios from 'axios';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use(
  (config) => {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('chronos_token');
      const orgId = localStorage.getItem('chronos_org_id');

      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      if (orgId) {
        config.headers['X-Organization-Id'] = orgId;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && typeof window !== 'undefined') {
      // Don't auto redirect on login page
      if (!window.location.pathname.startsWith('/login')) {
        localStorage.removeItem('chronos_token');
        localStorage.removeItem('chronos_user');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);
