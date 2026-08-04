import type { ComponentPublicInstance } from 'vue';
import * as api from '@/api/base-data/print-template';
import type { QueryPrintTemplateBo } from '@/api/base-data/print-template/model/queryPrintTemplateBo';
import { createError } from '@/hooks/web/msg';
import type {
  PrintRuntimeApi,
  PrintRuntimePreviewOptions,
} from '@/components/PrintDesigner/src/printRuntime';
import type { PrintTemplateOption } from '@/components/PrintDialog';

type PrintBizType = string | number;

export interface PrintTemplateSelection {
  templateId: string;
  templateList: PrintTemplateOption[];
}

interface PrintMixinInstance extends ComponentPublicInstance {
  $printRuntimeApi?: PrintRuntimeApi;
}

const TEMPLATE_QUERY_PAGE_SIZE = 200;

/**
 * 将业务类型统一转换为字符串，匹配打印模板接口的查询参数格式。
 */
function toBizType(type: PrintBizType) {
  return String(type);
}

/**
 * 将后端模板列表转换为预览弹窗可直接使用的下拉选项。
 */
function toTemplateOptions(templateList: QueryPrintTemplateBo[]): PrintTemplateOption[] {
  return templateList.map(({ id, name, bizType }) => ({
    id,
    name,
    bizType,
  }));
}

/**
 * 按业务类型查询可用于订单打印预览的模板列表。
 */
async function queryTemplateByBizType(bizType: string) {
  const result = await api.query({
    pageIndex: 1,
    pageSize: TEMPLATE_QUERY_PAGE_SIZE,
    sortField: '',
    sortOrder: '',
    name: '',
    bizType,
  });

  return result?.datas || [];
}

/**
 * 读取指定打印模板的设计器 JSON 配置。
 */
async function getTemplateJson(templateId: string) {
  const setting = await api.getSetting(templateId);
  return setting?.templateJson;
}

/**
 * 加载指定业务类型的打印模板选项，并返回默认选中的模板。
 */
export async function getPrintTemplateSelection(
  type: PrintBizType,
): Promise<PrintTemplateSelection | undefined> {
  const templateList = await queryTemplateByBizType(toBizType(type));

  if (!templateList.length) {
    createError('未找到当前业务类型的打印模板！');
    return undefined;
  }

  const defaultTemplate = templateList.find((item) => item.isDefault);
  return {
    templateId: (defaultTemplate || templateList[0]).id,
    templateList: toTemplateOptions(templateList),
  };
}

/**
 * 订单打印预览入口。
 *
 * 根据业务类型查找模板，加载首个模板配置，并打开运行时预览弹窗；
 * 同时把同业务类型模板列表传入弹窗，支持预览时切换模板。
 */
export async function vgPrintPreview(
  this: PrintMixinInstance,
  type: PrintBizType,
  printData: unknown,
  options: PrintRuntimePreviewOptions = {},
) {
  const bizType = toBizType(type);
  const templateList = await queryTemplateByBizType(bizType);

  if (!templateList.length) {
    createError('未找到当前业务类型的打印模板！');
    return;
  }

  // 优先选择默认模板，否则选择第一个
  const defaultTemplate = templateList.find((item) => item.isDefault);
  const templateId = (defaultTemplate || templateList[0]).id;
  const templateJson = await getTemplateJson(templateId);

  if (!templateJson) {
    createError('未找到打印模板配置！');
    return;
  }

  const preview = this.$printRuntimeApi?.preview;
  if (typeof preview !== 'function') {
    createError('打印预览组件未正确初始化！');
    return;
  }

  preview(templateJson, printData, {
    ...options,
    bizType,
    templateId,
    enableTemplateSwitch: true,
    templateList: toTemplateOptions(templateList),
  });
}

/**
 * 浏览器打印入口。
 *
 * 按用户选中的模板加载配置，然后直接调起浏览器的系统打印对话框。
 */
export async function vgBrowserPrint(
  this: PrintMixinInstance,
  printData: unknown,
  templateId: string,
) {
  if (!templateId) {
    createError('请选择打印模板！');
    return;
  }

  const templateJson = await getTemplateJson(templateId);
  if (!templateJson) {
    createError('未找到打印模板配置！');
    return;
  }

  const browserPrint = this.$printRuntimeApi?.browserPrint;
  if (typeof browserPrint !== 'function') {
    createError('浏览器打印组件未正确初始化！');
    return;
  }

  browserPrint(templateJson, printData);
}

export const printMix = {
  methods: {
    getPrintTemplateSelection,
    vgBrowserPrint,
    vgPrintPreview,
  },
};
