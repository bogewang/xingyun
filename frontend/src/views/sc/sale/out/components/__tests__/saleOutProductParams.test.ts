import { describe, expect, it } from 'vitest';

import {
  buildRequiredSaleOutProducts,
  buildUnrequiredSaleOutProducts,
} from '../saleOutProductParams';

describe('销售出库明细请求组装', () => {
  it('非订单出库保留有商品编号但数量为空和为零的行', () => {
    expect(
      buildUnrequiredSaleOutProducts([
        {
          productId: 'p-empty',
          unitId: 'u-1',
          outNum: '',
          oriPrice: '10',
          taxPrice: '10',
          description: '',
        },
        {
          productId: 'p-zero',
          unitId: 'u-2',
          outNum: 0,
          oriPrice: '20',
          taxPrice: '20',
          description: '零数量',
        },
        { productId: '', outNum: 3 },
      ]),
    ).toEqual([
      {
        productId: 'p-empty',
        unitId: 'u-1',
        orderNum: '',
        oriPrice: '10',
        taxPrice: '10',
        description: '',
        confirmNum: 0,
      },
      {
        productId: 'p-zero',
        unitId: 'u-2',
        orderNum: 0,
        oriPrice: '20',
        taxPrice: '20',
        description: '零数量',
        confirmNum: 0,
      },
    ]);
  });

  it('需订单出库保留有商品编号的零数量行及订单明细关联', () => {
    expect(
      buildRequiredSaleOutProducts([
        {
          id: 'detail-1',
          isFixed: true,
          productId: 'p-1',
          outNum: 0,
          oriPrice: '5',
          taxPrice: '5',
          description: '',
        },
        { productId: '', outNum: 2 },
      ]),
    ).toEqual([
      {
        productId: 'p-1',
        orderNum: 0,
        description: '',
        oriPrice: '5',
        taxPrice: '5',
        confirmNum: 0,
        saleOrderDetailId: 'detail-1',
      },
    ]);
  });

  it('非订单出库提交前将验收数量转换为 number', () => {
    const [product] = buildUnrequiredSaleOutProducts([
      {
        productId: 'product-1',
        unitId: 'unit-1',
        oriPrice: 65.79,
        taxPrice: 65.75,
        outNum: 8,
        confirmNum: '8',
      },
    ]);

    expect(product.confirmNum).toBe(8);
    expect(typeof product.confirmNum).toBe('number');
  });

  it('保留手工填写的计划日期', () => {
    const [product] = buildUnrequiredSaleOutProducts([
      {
        productId: 'product-1',
        outNum: 1,
        planDate: '2026-08-13',
      },
    ]);

    expect(product.planDate).toBe('2026-08-13');
  });

  it('需订单出库提交前将小数验收数量转换为 number', () => {
    const [product] = buildRequiredSaleOutProducts([
      {
        id: 'detail-1',
        productId: 'product-1',
        outNum: 5,
        confirmNum: '5.1',
        taxPrice: 71.71,
      },
    ]);

    expect(product.confirmNum).toBe(5.1);
    expect(typeof product.confirmNum).toBe('number');
  });
});
