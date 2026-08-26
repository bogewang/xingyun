import type { PrintReceiveSheetBo } from '@/api/sc/purchase/receive/model/printReceiveSheetBo';
import { PRINT_TYPE } from '@/enums/biz/printType';

export type ReceiveSheetPrintLoader = (id: string) => Promise<PrintReceiveSheetBo>;

export type ReceiveSheetBrowserPrint = (
  type: number,
  data: PrintReceiveSheetBo,
) => Promise<void> | void;

/**
 * 加载采购入库打印数据并调起采购入库业务类型的浏览器打印。
 *
 * @param id 采购入库单ID
 * @param load 打印数据加载函数
 * @param browserPrint 浏览器打印函数
 */
export async function browserPrintReceiveSheet(
  id: string,
  load: ReceiveSheetPrintLoader,
  browserPrint: ReceiveSheetBrowserPrint,
): Promise<void> {
  const data = await load(id);
  await browserPrint(PRINT_TYPE.RECEIVE_SHEET.code, data);
}
