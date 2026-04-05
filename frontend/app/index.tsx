import { View, Text, TouchableOpacity, SafeAreaView } from 'react-native';
import { useRouter } from 'expo-router';
// Import the global CSS file to register NativeWind styles
import '../global.css';

export default function LoginScreen() {
  const router = useRouter();

  const handleLogin = () => {
    // Stub OAuth implementation resolving immediately to JWT persistence 
    // SecureStore.setItemAsync('access_token', 'mock_jwt_token_here');
    router.replace('/(dashboard)/home');
  };

  return (
    <SafeAreaView className="flex-1 bg-white items-center justify-center px-6">
      
      {/* Brand & Sparkle */}
      <View className="mb-12 items-center">
        <Text className="text-4xl text-primary font-bold tracking-tight">✦ SimplyViral</Text>
      </View>

      {/* Hero Headings */}
      <View className="items-center mb-16">
        <Text className="text-4xl text-dark font-extrabold text-center mb-4 leading-tight">
          Don't create content. {"\n"} Let it create itself.
        </Text>
        <Text className="text-lg text-gray-500 text-center px-4">
          AI that knows what goes viral — before you do.
        </Text>
      </View>

      {/* Action Buttons */}
      <View className="w-full space-y-4">
        
        {/* Main CTA bounds as secondary entry here for visual parity with Figma*/}
        <TouchableOpacity 
          onPress={handleLogin}
          className="w-full bg-primary py-4 rounded-full flex-row justify-center items-center shadow-md shadow-blue-500/30"
        >
          <Text className="text-white text-lg font-bold">Continue with Google</Text>
        </TouchableOpacity>

        {/* Demo Button */}
        <TouchableOpacity 
          className="w-full bg-white border border-gray-300 py-4 rounded-full flex-row justify-center items-center"
        >
          <Text className="text-dark text-lg font-medium">Watch Demo</Text>
        </TouchableOpacity>
        
      </View>

      {/* Footer */}
      <View className="absolute bottom-8 items-center">
         <Text className="text-xs text-gray-400">© 2026 SimplyViral. Start Creating Without Thinking.</Text>
      </View>

    </SafeAreaView>
  );
}
