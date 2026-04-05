import { apiClient } from './api';

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: string;
  errorCode?: string;
}

export const AuthService = {
  checkStatus: async (): Promise<ApiResponse<string>> => {
    const response = await apiClient.get<ApiResponse<string>>('/auth/status');
    return response.data;
  },

  // Future integration points:
  // loginWithGoogle: async (idToken: string) => {...}
  // requestOtp: async (phone: string) => {...}
};
