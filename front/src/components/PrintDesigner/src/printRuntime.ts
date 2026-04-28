// 导入 vg-print 的模板创建与浏览器打印能力，替换旧 lodop 运行时。
import { createTemplate, exportImage, exportPdf, printBrowser } from 'vg-print';

/**
 * 构建运行时空模板。
 *
 * 功能:
 * 为运行时预览、打印和导出提供兜底模板结构。
 *
 * 参数:
 * 无。
 *
 * 返回值:
 * {Record<string, unknown>} 返回包含空 `panels` 的模板对象。
 *
 * 异常:
 * 无显式抛出异常。
 */
function createEmptyTemplate(): Record<string, unknown> {
  // 返回最小模板对象，避免运行时工具拿到空值后直接报错。
  return { panels: [] };
}

/**
 * 标准化运行时模板。
 *
 * 功能:
 * 仅接受 vg-print 新模板结构；不兼容旧结构时直接回退为空模板。
 *
 * 参数:
 * @param {unknown} templateJson - 后端或业务层传入的模板 JSON。
 *
 * 返回值:
 * {Record<string, unknown>} 返回可用于创建打印模板实例的对象。
 *
 * 异常:
 * 无显式抛出异常。
 */
function normalizeTemplate(templateJson: unknown): Record<string, unknown> {
  // 判断模板值是否为对象，避免读取 panels 属性时报错。
  const isObjectValue = !!templateJson && typeof templateJson === 'object';
  // 判断模板是否已经是 vg-print 的新结构。
  const hasPanels = isObjectValue && Array.isArray((templateJson as { panels?: unknown[] }).panels);
  // 返回合法模板，或统一回退到空模板。
  return hasPanels ? (templateJson as Record<string, unknown>) : createEmptyTemplate();
}

/**
 * 标准化运行时打印数据。
 *
 * 功能:
 * 保证运行时 API 始终接收数组形式的数据，和设计器行为保持一致。
 *
 * 参数:
 * @param {unknown} data - 打印数据，可以是对象、数组或空值。
 *
 * 返回值:
 * {unknown[]} 返回标准化后的数据数组。
 *
 * 异常:
 * 无显式抛出异常。
 */
function normalizeData(data: unknown): unknown[] {
  // 如果已经是数组，则直接返回。
  if (Array.isArray(data)) {
    // 返回原数组以保留调用方提供的数据结构。
    return data;
  }

  // 如果传入的是单条对象，则包装成数组。
  if (data && typeof data === 'object') {
    // 返回单元素数组，满足 vg-print 的批量打印接口要求。
    return [data];
  }

  // 空值场景统一返回空数组。
  return [];
}

/**
 * 创建 vg-print 模板实例。
 *
 * 功能:
 * 根据模板 JSON 构建可用于预览、打印和导出的模板实例。
 *
 * 参数:
 * @param {unknown} templateJson - 模板 JSON 数据。
 *
 * 返回值:
 * {unknown} 返回 `createTemplate` 生成的模板实例。
 *
 * 异常:
 * 可能抛出 `createTemplate` 在模板格式非法时触发的运行时异常。
 */
function buildTemplate(templateJson: unknown) {
  // 使用标准化后的模板对象创建运行时模板实例。
  return createTemplate(normalizeTemplate(templateJson));
}

/**
 * 浏览器预览打印内容。
 *
 * 功能:
 * 使用 vg-print 的浏览器打印能力展示当前模板与数据。
 *
 * 参数:
 * @param {unknown} templateJson - 模板 JSON 数据。
 * @param {unknown} data - 打印数据。
 *
 * 返回值:
 * {unknown} 返回 `printBrowser` 的执行结果。
 *
 * 异常:
 * 可能抛出模板创建或浏览器打印阶段的运行时异常。
 */
function preview(templateJson: unknown, data: unknown) {
  // 基于模板 JSON 生成运行时模板实例。
  const template = buildTemplate(templateJson);
  // 使用标准化后的数组数据打开浏览器打印预览。
  return printBrowser(template, normalizeData(data));
}

/**
 * 浏览器直接打印内容。
 *
 * 功能:
 * 当前项目仅接入浏览器打印，因此直接复用预览打印能力。
 *
 * 参数:
 * @param {unknown} templateJson - 模板 JSON 数据。
 * @param {unknown} data - 打印数据。
 *
 * 返回值:
 * {unknown} 返回浏览器打印执行结果。
 *
 * 异常:
 * 可能抛出模板创建或浏览器打印阶段的运行时异常。
 */
function print(templateJson: unknown, data: unknown) {
  // 直接复用浏览器打印能力，统一运行时入口。
  return preview(templateJson, data);
}

/**
 * 导出 PDF。
 *
 * 功能:
 * 将模板与数据渲染为 PDF 文件。
 *
 * 参数:
 * @param {unknown} templateJson - 模板 JSON 数据。
 * @param {unknown} data - 打印数据。
 * @param {string} [filename='print-document'] - 导出文件名。
 * @param {Record<string, unknown>} [options={}] - 导出附加选项。
 *
 * 返回值:
 * {unknown} 返回导出任务结果。
 *
 * 异常:
 * 可能抛出模板创建或 PDF 导出阶段的运行时异常。
 */
function toPdf(
  templateJson: unknown,
  data: unknown,
  filename = 'print-document',
  options: Record<string, unknown> = {},
) {
  // 基于模板 JSON 生成运行时模板实例。
  const template = buildTemplate(templateJson);
  // 调用官方导出能力，生成 PDF 文件。
  return exportPdf(template, normalizeData(data), filename, options);
}

/**
 * 导出图片。
 *
 * 功能:
 * 将模板与数据渲染为图片。
 *
 * 参数:
 * @param {unknown} templateJson - 模板 JSON 数据。
 * @param {unknown} data - 打印数据。
 * @param {Record<string, unknown>} [options={}] - 导出附加选项。
 *
 * 返回值:
 * {unknown} 返回导出任务结果。
 *
 * 异常:
 * 可能抛出模板创建或图片导出阶段的运行时异常。
 */
function toImage(
  templateJson: unknown,
  data: unknown,
  options: Record<string, unknown> = {},
) {
  // 基于模板 JSON 生成运行时模板实例。
  const template = buildTemplate(templateJson);
  // 调用官方导出能力，生成图片结果。
  return exportImage(template, normalizeData(data), options);
}

// 导出统一运行时工具对象，便于挂到全局属性中供业务复用。
export default {
  preview,
  print,
  toImage,
  toPdf,
};
