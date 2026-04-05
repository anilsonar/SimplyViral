import { apiClient, ApiResponse } from './api';

export const JobService = {
  generateJob: async (planType: string): Promise<ApiResponse<string>> => {
    // Uses URL params as defined in our primitive backend controller, can be updated to JSON body later.
    const response = await apiClient.post<ApiResponse<string>>(`/jobs/generate?planType=${planType}`);
    return response.data;
  },

  getJobStatus: async (jobId: string): Promise<ApiResponse<string>> => {
    const response = await apiClient.get<ApiResponse<string>>(`/jobs/${jobId}/status`);
    return response.data;
  },
};
