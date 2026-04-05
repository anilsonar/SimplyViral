import { View, Text, TouchableOpacity, ScrollView, SafeAreaView, ActivityIndicator } from 'react-native';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { JobService } from '../../services/job.service';

export default function DashboardScreen() {
  const router = useRouter();
  const [isGenerating, setIsGenerating] = useState(false);
  const [isPremium, setIsPremium] = useState(true); // Stub implementation determining plan boundaries

  const checkPlanAndTriggerGeneration = async () => {
    setIsGenerating(true);
    const planType = isPremium ? "PREMIUM_SHORT" : "STANDARD_SHORT";
    
    try {
        const result = await JobService.generateJob(planType);
        if (result.success) {
            // Usually we extract the jobId here and redirect to the polling screen
            // router.push(`/(dashboard)/status/${jobId}`);
            alert('Job successfully deployed to workflow engine!');
        } else {
            console.error(result.error);
        }
    } catch (err) {
        console.error("Failed to bind to local engine", err);
    } finally {
        setIsGenerating(false);
    }
  };

  return (
    <SafeAreaView className="flex-1 bg-white">
      {/* Header */}
      <View className="flex-row justify-between items-center px-6 py-4 border-b border-gray-100">
        <Text className="text-xl font-bold text-dark">SimplyViral</Text>
        
        {/* Toggle Stub for presentation */}
        <TouchableOpacity onPress={() => setIsPremium(!isPremium)} className="bg-gray-100 px-3 py-1 rounded-full">
            <Text className="text-xs font-semibold text-gray-500">Plan: {isPremium ? 'PREMIUM' : 'STANDARD'}</Text>
        </TouchableOpacity>
      </View>

      <ScrollView className="flex-1 px-6">
        {/* Main Banner CTA */}
        <View className="bg-grayLight rounded-2xl p-8 mt-6 items-center border border-gray-200">
          <Text className="text-2xl font-bold text-dark mb-2 text-center">Let AI create your next viral moment</Text>
          <Text className="text-gray-500 mb-6 text-center">Our predictive algorithm binds raw topics to high-retention formats effortlessly.</Text>
          
          <TouchableOpacity 
            onPress={checkPlanAndTriggerGeneration}
            disabled={isGenerating}
            className={`flex-row items-center justify-center px-8 py-4 rounded-full shadow-lg ${isGenerating ? 'bg-gray-400' : 'bg-primary shadow-blue-500/40'}`}
          >
            {isGenerating ? (
                <ActivityIndicator color="white" />
            ) : (
                <Text className="text-white text-lg font-bold">✦ Generate {isPremium ? 'Premium' : ''} Video</Text>
            )}
          </TouchableOpacity>
        </View>

        {/* Recent Videos Grid Skeleton */}
        <Text className="text-xl font-bold text-dark mt-10 mb-4">Recent Videos</Text>
        
        <View className="flex-row justify-between flex-wrap">
            {[1,2,3,4].map((item) => (
                <View key={item} className="w-[48%] mb-4">
                    <View className="w-full h-40 bg-grayLight rounded-xl relative overflow-hidden">
                        {/* Status Badge */}
                        <View className="absolute top-2 right-2 bg-success px-2 py-1 rounded-full">
                            <Text className="text-white text-xs font-bold">Published</Text>
                        </View>
                    </View>
                    <Text className="font-bold text-dark mt-2">Space Core {item}</Text>
                    <Text className="text-xs text-gray-500">Educational • 250k Views</Text>
                </View>
            ))}
        </View>

      </ScrollView>
    </SafeAreaView>
  );
}
