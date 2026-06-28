import { registerRootComponent } from 'expo';

import App from './App';

// registerRootComponent → AppRegistry.registerComponent('main', () => App).
// Expo Go·네이티브 빌드 양쪽에서 동일하게 동작하도록 보장한다.
registerRootComponent(App);
