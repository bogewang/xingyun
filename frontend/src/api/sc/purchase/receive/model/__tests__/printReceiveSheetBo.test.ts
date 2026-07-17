import { describe, expect, it } from 'vitest';

import type { PrintReceiveSheetBo } from '../printReceiveSheetBo';

const printData = {
  id: 'receive-1',
  code: 'RK202607050001',
  scId: 'sc-1',
  scCode: 'SC001',
  scName: '示例仓库',
  supplierId: 'supplier-1',
  supplierCode: 'SUP001',
  supplierName: '示例供应商',
  purchaserId: 'user-1',
  purchaserName: '张三',
  orderDate: '2026-07-05',
  paymentDate: '2026-07-06',
  receiveDate: '2026-07-07',
  purchaseOrderId: 'purchase-order-1',
  purchaseOrderCode: 'CG202607050001',
  totalNum: 10,
  totalGiftNum: 1,
  totalAmount: 120,
  paidAmount: 100,
  unpaidAmount: 20,
  description: '示例备注',
  createBy: '张三',
  createTime: '2026-07-05 10:00:00',
  updateBy: '李四',
  updateTime: '2026-07-05 11:00:00',
  approveBy: '王五',
  approveTime: '2026-07-05 12:00:00',
  status: 2,
  refuseReason: '',
  settleStatus: 1,
  details: [
    {
      id: 'receive-detail-1',
      productId: 'product-1',
      productCode: 'P0001',
      productName: '示例商品',
      skuCode: 'SKU001',
      externalCode: 'SP001',
      orderNum: 10,
      receiveNum: 10,
      unitId: 'unit-1',
      unitName: '袋',
      conversionRate: 1,
      businessNum: 10,
      taxPrice: 12,
      purchasePrice: 12,
      taxAmount: 120,
      receiveAmount: 120,
      isGift: false,
      taxRate: 13,
      description: '明细备注',
      orderNo: 1,
      purchaseOrderDetailId: 'purchase-order-detail-1',
      productionDate: '2026-07-01',
    },
  ],
} satisfies PrintReceiveSheetBo;

describe('采购入库打印响应模型', () => {
  it('覆盖完整采购入库字段和明细字段', () => {
    expect(printData.purchaseOrderId).toBe('purchase-order-1');
    expect(printData.totalGiftNum).toBe(1);
    expect(printData.settleStatus).toBe(1);
    expect(printData.details[0].productionDate).toBe('2026-07-01');
    expect(printData.details[0].receiveNum).toBe(10);
  });
});
