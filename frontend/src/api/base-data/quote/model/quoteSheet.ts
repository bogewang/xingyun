import { PageVo } from '@/api/model/pageVo';

/** 报价单查询参数。 */
export interface QueryQuoteSheetVo extends PageVo {
  code?: string;
  name?: string;
  status?: 'ENABLED' | 'DISABLED';
  startDate?: string;
  endDate?: string;
}

/** 报价单商品。 */
export interface QuoteSheetProductVo {
  productId: string;
  salePrice: string | number;
}

/** 报价单保存参数。 */
export interface QuoteSheetVo {
  id?: string;
  code: string;
  name: string;
  startDate: string;
  endDate: string;
  description?: string;
  products: QuoteSheetProductVo[];
}

/** 报价单列表数据。 */
export interface QuoteSheetBo extends QuoteSheetVo {
  status: 'ENABLED' | 'DISABLED';
  createBy?: string;
  createTime?: string;
}
