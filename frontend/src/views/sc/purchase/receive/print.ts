import type { PrintReceiveSheetBo } from '@/api/sc/purchase/receive/model/printReceiveSheetBo';
import { PRINT_TYPE } from '@/enums/biz/printType';

export type ReceiveSheetPrintLoader = (id: string) => Promise<PrintReceiveSheetBo>;

export type ReceiveSheetPrintPreview = (
  type: number,
  data: PrintReceiveSheetBo,
) => Promise<void> | void;

/**
 * 加载采购入库打印数据并打开采购入库业务类型的打印预览。
 *
 * @param id 采购入库单ID
 * @param load 打印数据加载函数
 * @param preview 打印预览函数
 */
export async function previewReceiveSheetPrint(
  id: string,
  load: ReceiveSheetPrintLoader,
  preview: ReceiveSheetPrintPreview,
): Promise<void> {
  const data = await load(id);
  await preview(PRINT_TYPE.RECEIVE_SHEET.code, data);
}
