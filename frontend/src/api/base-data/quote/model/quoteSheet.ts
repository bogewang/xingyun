import { PageVo } from '@/api/model/pageVo';

/** 报价单查询参数。 */
export interface QueryQuoteSheetVo extends PageVo {
  projectId?: string;
  name?: string;
  status?: 'ENABLED' | 'DISABLED';
  startDate?: string;
  endDate?: string;
  idList?: string[];
}

/** 报价单商品明细查询参数。 */
export interface QueryQuoteSheetDetailVo extends PageVo {
  projectId?: string;
  quoteSheetName?: string;
  status?: 'ENABLED' | 'DISABLED';
  inquiryProduct?: boolean;
  startDate?: string;
  endDate?: string;
  productKeyword?: string;
}

/** 报价单商品明细。 */
export interface QuoteSheetDetailBo {
  quoteSheetId: string;
  quoteSheetName: string;
  startDate: string;
  endDate: string;
  status: 'ENABLED' | 'DISABLED';
  detailId: string;
  productId: string;
  productCode?: string;
  productName?: string;
  spec?: string;
  unit?: string;
  salePrice: string | number;
  inquiryProduct?: boolean;
}

/** 报价单商品。 */
export interface QuoteSheetProductVo {
  productId: string;
  productName?: string;
  spec?: string | null;
  unitId?: string | null;
  unitName?: string | null;
  conversionRate?: string | number | null;
  salePrice: string | number;
  inquiryProduct?: boolean;
}

/** 报价单保存参数。 */
export interface QuoteSheetVo {
  id?: string;
  projectId?: string;
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
