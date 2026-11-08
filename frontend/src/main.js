import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 捕获ResizeObserver错误
if (window.ResizeObserver) {
  const resizeObserverErrHandler = (e) => {
    // 检查是否是ResizeObserver循环错误
    if (e.message && e.message.includes('ResizeObserver loop')) {
      // 静默处理此错误，不影响用户体验
      return false;
    }
    // 其他错误仍然抛出
    return true;
  };
  
  // 保存原始错误处理
  const originalOnError = window.onerror;
  window.onerror = function(msg, source, lineno, colno, error) {
    // 处理ResizeObserver错误
    if (error && !resizeObserverErrHandler(error)) {
      return true; // 阻止错误传播
    }
    // 其他错误使用原始处理
    return originalOnError ? originalOnError(msg, source, lineno, colno, error) : false;
  };
}

const app = createApp(App)

// 添加全局错误处理器，捕获并处理特定的DOM错误
app.config.errorHandler = (err, instance, info) => {
  // 过滤掉 'Cannot read properties of null (reading 'parentNode')' 错误
  // 这类错误通常发生在组件卸载后消息仍尝试更新DOM的情况
  if (err.message && err.message.includes("Cannot read properties of null (reading 'parentNode')")) {
    // 静默处理这个特定错误，避免影响用户体验
    console.log('处理已知的DOM更新错误：组件已卸载但消息仍尝试更新DOM');
    return;
  }
  
  // 对于其他错误，继续默认的错误处理
  console.error('全局错误:', err);
  console.log('错误信息:', info);
}

app.use(router)
app.use(ElementPlus)
app.mount('#app')