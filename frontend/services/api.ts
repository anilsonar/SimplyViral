import axios from 'axios';

// During local dev in Android emulator, localhost is usually 10.0.2.2.
// iOS simulator can use localhost.
const BASE_URL = 'http://localhost:8080/api/v1';

export const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Future: Add interceptors for JWT token attachment
apiClient.interceptors.request.use(
  async (config) => {
    // const token = await AsyncStorage.getItem('access_token');
    // if (token) {
    //   config.headers.Authorization = `Bearer ${token}`;
    // }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);
