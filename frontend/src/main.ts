import 'uno.css';
import 'ant-design-vue/dist/reset.css';
import 'vg-print/style.css';
import '@/design/index.less';
import '@/components/VxeTable/src/css/index.scss';
// Register icon sprite
import 'virtual:svg-icons-register';
import { io } from 'socket.io-client';
import { setSocketIo, hiprint } from 'vg-print';
import pluginEleCodeEditor from '@vg-print/plugin-code-editor';

import { createApp } from 'vue';

import { registerGlobComp } from '@/components/registerGlobComp';
import { setupGlobDirectives } from '@/directives';
import { setupI18n } from '@/locales/setupI18n';
import { initAppConfigStore } from '@/logics/initAppConfig';
import { router, setupRouter } from '@/router';
import { setupRouterGuard } from '@/router/guard';
import { setupStore } from '@/store';

import App from './App.vue';

async function bootstrap() {
  const app = createApp(App);

  // Configure store
  // 配置 store
  setupStore(app);

  // Initialize internal system configuration
  // 初始化内部系统配置
  initAppConfigStore();

  // Register global components
  // 注册全局组件
  await registerGlobComp(app);

  // Multilingual configuration
  // 多语言配置
  // Asynchronous case: language files may be obtained from the server side
  // 异步案例：语言文件可能从服务器端获取
  await setupI18n(app);

  // Configure routing
  // 配置路由
  setupRouter(app);

  // router-guard
  // 路由守卫
  setupRouterGuard(router);

  // Register global directive
  // 注册全局指令
  setupGlobDirectives(app);

  // https://next.router.vuejs.org/api/#isready
  // await router.isReady();

  app.mount('#app');
}

hiprint.init({
  host: 'http://localhost:17521',
  token: 'eyJrIjoiZ21jNTc2MDMzNyJ9',
});

// 在应用初始化时全局注册，确保模板解析和设计器均可使用代码编辑器插件。
hiprint.register({
  authKey: 'eyJrIjoiZ21jNTc2MDMzNyJ9',
  plugins: [pluginEleCodeEditor()],
});

// disAutoConnect() // 注入不自动链接，需要关闭自动链接
// 关键：先把 window.autoConnect 置 false
setSocketIo(io); // 再注入 io（这一步不会触发连接了）

bootstrap();
