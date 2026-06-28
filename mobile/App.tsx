import React, { useEffect, useState } from 'react';
import { NavigationContainer, createNavigationContainerRef } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { registerNav, registerForPushNotifications, attachNotificationRouting } from './src/lib/push';
import Login from './src/screens/Login';
import Signup from './src/screens/Signup';
import Onboard from './src/screens/Onboard';
import Today from './src/screens/Today';
import HistoryLibrary from './src/screens/HistoryLibrary';
import Bookmarks from './src/screens/Bookmarks';
import Slots from './src/screens/Slots';
import SlotEditions from './src/screens/SlotEditions';
import EditionDetail from './src/screens/EditionDetail';
import Settings from './src/screens/Settings';
import { useSession } from './src/store/session';
import { Header } from './src/components/Header';
import { BottomTabBar } from './src/components/BottomTabBar';

const queryClient = new QueryClient();
const Stack = createNativeStackNavigator();
const Tab = createBottomTabNavigator();
const navigationRef = createNavigationContainerRef();

// 메인 하단 탭 — 오늘 / 히스토리 / 내 슬롯 (책갈피는 헤더 햄버거 메뉴). 둥근 알약형 커스텀 탭바.
// 헤더는 상위 스택이 그리므로 탭 자체 헤더는 끈다.
function MainTabs() {
  return (
    <Tab.Navigator
      tabBar={(props) => <BottomTabBar {...props} />}
      screenOptions={{ headerShown: false }}>
      <Tab.Screen name="Home" component={Today} options={{ title: '오늘' }} />
      <Tab.Screen name="History" component={HistoryLibrary} options={{ title: '히스토리' }} />
      <Tab.Screen name="Slots" component={Slots} options={{ title: '내 슬롯' }} />
    </Tab.Navigator>
  );
}

export default function App() {
  // 영속 세션(SecureStore) 복원 대기 → 복원되면 로그인 여부에 따라 초기 화면 결정.
  const [hydrated, setHydrated] = useState(useSession.persist.hasHydrated());
  useEffect(() => {
    setHydrated(useSession.persist.hasHydrated());
    return useSession.persist.onFinishHydration(() => setHydrated(true));
  }, []);
  const userId = useSession((s) => s.userId);

  // 알림 탭 → 딥링크 라우팅 리스너(앱 수명주기 1회)
  useEffect(() => attachNotificationRouting(), []);
  // 로그인 상태가 되면 푸시 권한 요청 + 디바이스 토큰 서버 등록
  useEffect(() => {
    if (userId != null) registerForPushNotifications();
  }, [userId]);

  if (!hydrated) return null; // 복원 전 한 프레임(스플래시는 expo-splash-screen으로 확장 가능)
  const initialRouteName = userId != null ? 'Main' : 'Login';

  return (
    <QueryClientProvider client={queryClient}>
      <NavigationContainer ref={navigationRef} onReady={() => registerNav(navigationRef)}>
        <Stack.Navigator
          initialRouteName={initialRouteName}
          screenOptions={{
            // 전 화면 공유 헤더(네비게이터 관리) — push 화면이면 back 화살표, 아니면 햄버거만.
            header: ({ navigation, back }) => <Header navigation={navigation} back={!!back} />,
          }}>
          {/* 인증 화면 — 헤더 없음 */}
          <Stack.Screen name="Login" component={Login} options={{ headerShown: false }} />
          <Stack.Screen name="Signup" component={Signup} options={{ headerShown: false }} />
          <Stack.Screen name="Onboard" component={Onboard} options={{ headerShown: false }} />
          <Stack.Screen name="Main" component={MainTabs} />
          {/* 탭 위에 push 되는 상세 화면들 — 공유 헤더가 back 화살표를 표시 */}
          <Stack.Screen name="SlotEditions" component={SlotEditions} />
          <Stack.Screen name="EditionDetail" component={EditionDetail} />
          <Stack.Screen name="Settings" component={Settings} />
          <Stack.Screen name="Bookmarks" component={Bookmarks} />
        </Stack.Navigator>
      </NavigationContainer>
    </QueryClientProvider>
  );
}
