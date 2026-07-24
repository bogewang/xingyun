import './inquiryProduct.less';

/**
 * 格式化询价商品标识，供采购收货相关表格统一展示。
 */
export function formatInquiryProduct(value: boolean | null | undefined) {
  return value
    ? { text: '是' as const, className: 'inquiry-product-yes' as const }
    : { text: '否' as const, className: 'inquiry-product-no' as const };
}
