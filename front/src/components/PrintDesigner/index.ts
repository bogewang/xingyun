// 导入新的打印设计器包装组件，作为业务层统一入口。
import PrintDesigner from './src/PrintDesigner.vue';
// 导入基于 vg-print 的运行时打印工具，替换旧 lodop 能力。
import printRuntime from './src/printRuntime';

// 导出新的打印设计器组件，供全局注册与页面直接引用。
export default PrintDesigner;

// 导出运行时工具，并沿用 lodop 名称以复用现有全局挂载入口。
export { printRuntime as lodop };
