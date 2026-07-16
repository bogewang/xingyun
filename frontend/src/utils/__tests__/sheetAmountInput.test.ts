import { describe, expect, it } from 'vitest';

import {
  applyManualSheetAmount,
  clearManualSheetAmount,
  getSheetLineAmount,
} from '../sheetAmountInput';

describe('单据明细金额输入', () => {
  it('输入金额后反算六位小数单价且保留手工金额', () => {
    const row = { receiveNum: '3', purchasePrice: '', taxAmount: '' };

    applyManualSheetAmount(row, '80', 'receiveNum', 'purchasePrice');

    expect(row.taxAmount).toBe('80');
    expect(row.purchasePrice).toBe(26.666667);
    expect(row.manualTaxAmount).toBe(true);
    expect(getSheetLineAmount(row, 'receiveNum', 'purchasePrice')).toBe(80);
  });

  it('清除手工金额后回填数量乘单价的自动金额', () => {
    const row = { outNum: '3', taxPrice: 26.6, taxAmount: '80', manualTaxAmount: true };

    clearManualSheetAmount(row, 'outNum', 'taxPrice');

    expect(row.taxAmount).toBe(79.8);
    expect(getSheetLineAmount(row, 'outNum', 'taxPrice')).toBe(79.8);
  });

  it('非法输入保留上一次金额且零数量不反算单价', () => {
    const row = { outNum: '0', taxPrice: '12', taxAmount: '8.5', manualTaxAmount: true };

    applyManualSheetAmount(row, '1e3', 'outNum', 'taxPrice');

    expect(row.taxAmount).toBe('8.5');
    expect(row.taxPrice).toBe('12');
    expect(row.manualTaxAmount).toBe(true);
  });

  it('v-model 先覆盖非法金额时回退到上一次合法金额', () => {
    ['1e3', 'abc', '-1'].forEach((invalidAmount) => {
      const row = {
        receiveNum: '3',
        purchasePrice: 26.666667,
        taxAmount: '80',
        lastValidTaxAmount: '80',
        manualTaxAmount: true,
      };

      row.taxAmount = invalidAmount;
      applyManualSheetAmount(row, invalidAmount, 'receiveNum', 'purchasePrice');

      expect(row.taxAmount).toBe('80');
      expect(row.purchasePrice).toBe(26.666667);
    });
  });

  it('初始合法金额首次输入非法值时回退到自动缓存金额', () => {
    ['1e3', 'abc', '-1'].forEach((invalidAmount) => {
      const row = { receiveNum: '3', purchasePrice: 26.666667, taxAmount: '80' };

      getSheetLineAmount(row, 'receiveNum', 'purchasePrice');
      row.taxAmount = invalidAmount;
      applyManualSheetAmount(row, invalidAmount, 'receiveNum', 'purchasePrice');

      expect(row.taxAmount).toBe('80');
    });
  });

  it('加载行未初始化缓存时首次输入非法值回退到自动金额', () => {
    ['1e3', 'abc', '-1'].forEach((invalidAmount) => {
      const row = { receiveNum: '3', purchasePrice: 26.666667, taxAmount: invalidAmount };

      applyManualSheetAmount(row, invalidAmount, 'receiveNum', 'purchasePrice');

      expect(row.taxAmount).toBe('80');
    });
  });
});
