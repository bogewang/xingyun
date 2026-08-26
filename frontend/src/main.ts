import 'uno.css';
import 'ant-design-vue/dist/reset.css';
import 'vg-print/style.css';
import '@/design/index.less';
import '@/components/VxeTable/src/css/index.scss';
// Register icon sprite
import 'virtual:svg-icons-register';
import { io } from 'socket.io-client';
import { setSocketIo, hiprint, disAutoConnect } from 'vg-print';
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

/**
 * 处理懒加载 chunk 404：新版本部署后服务器上旧 hash 的 chunk 已被删除，
 * 若浏览器仍持有旧版本的 index.html（或标签页在部署前已打开），
 * 动态导入会报 "Failed to fetch dynamically imported module"，此时自动刷新页面加载新版本。
 */
function setupChunkErrorReload() {
  const CHUNK_RELOAD_KEY = '__chunk_error_reload_ts__';
  // 30 秒内只自动刷新一次，防止刷新后依然失败导致死循环
  const RELOAD_INTERVAL = 30 * 1000;

  const reloadOnce = () => {
    const lastReload = Number(sessionStorage.getItem(CHUNK_RELOAD_KEY) || 0);
    if (Date.now() - lastReload < RELOAD_INTERVAL) {
      return;
    }
    sessionStorage.setItem(CHUNK_RELOAD_KEY, String(Date.now()));
    console.warn('[chunk] 动态导入模块失败，自动刷新页面加载新版本');
    location.reload();
  };

  // 路由懒加载失败时通过 Promise 拒绝抛出
  window.addEventListener('unhandledrejection', (event) => {
    const message = String(event.reason?.message ?? event.reason ?? '');
    // 动态导入失败的典型报错文案
    if (/dynamically imported module|importing a module script failed/i.test(message)) {
      reloadOnce();
    }
  });

  // 部分场景（如 preload 预加载）只有资源加载错误事件，没有 Promise 拒绝
  window.addEventListener(
    'error',
    (event) => {
      const target = event.target as HTMLElement | null;
      const src = (target as HTMLScriptElement | null)?.src ?? '';
      if (target?.tagName === 'SCRIPT' && src.includes('/assets/')) {
        reloadOnce();
      }
    },
    true,
  );
}

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

disAutoConnect();
setSocketIo(io); // 再注入 io（这一步不会触发连接了）

hiprint.init({
  host: 'http://127.0.0.1:17521',
  token: 'D,u.j@Xu}MN%;"3y-J"|',
});

// 在应用初始化时全局注册，确保模板解析和设计器均可使用代码编辑器插件。
hiprint.register({
  authKey: 'eyJrIjoiZ21jNTc2MDMzNyJ9',
  plugins: [pluginEleCodeEditor()],
});

// disAutoConnect() // 注入不自动链接，需要关闭自动链接
// 关键：先把 window.autoConnect 置 false

// 必须在 bootstrap 之前注册，动态导入发生在应用启动和路由跳转过程中
setupChunkErrorReload();

bootstrap();
