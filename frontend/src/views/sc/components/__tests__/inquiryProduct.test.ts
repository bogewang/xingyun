import { describe, expect, it } from 'vitest';
import { formatInquiryProduct } from '../inquiryProduct';

describe('formatInquiryProduct', () => {
  it('将空值和假值显示为红色否', () => {
    expect(formatInquiryProduct(null)).toEqual({
      text: '否',
      className: 'inquiry-product-no',
    });
    expect(formatInquiryProduct(undefined)).toEqual({
      text: '否',
      className: 'inquiry-product-no',
    });
    expect(formatInquiryProduct(false)).toEqual({
      text: '否',
      className: 'inquiry-product-no',
    });
  });

  it('将真值显示为绿色是', () => {
    expect(formatInquiryProduct(true)).toEqual({ text: '是', className: 'inquiry-product-yes' });
  });
});
