export interface QuoteUnit {
  id: string | null;
  unitName: string;
  conversionRate: string | number;
  available?: boolean;
}

const equivalentNames: Record<string, string[]> = {
  包: ['包', '袋'],
  袋: ['包', '袋'],
  千克: ['千克', '公斤', 'kg'],
  公斤: ['千克', '公斤', 'kg'],
  kg: ['千克', '公斤', 'kg'],
};

/** 为有换算率的商品单位补充等价名称；不同名称共用同一单位ID和换算率。 */
export function expandQuoteUnits(units: QuoteUnit[]): QuoteUnit[] {
  const result: QuoteUnit[] = [];
  const names = new Set<string>();
  const available = units.filter((unit) => unit.available !== false);
  for (const unit of available) {
    const name = unit.unitName.trim();
    if (!names.has(name)) {
      result.push({ ...unit, unitName: name });
      names.add(name);
    }
  }
  for (const unit of available) {
    const name = unit.unitName.trim();
    for (const alias of equivalentNames[name.toLowerCase()] || []) {
      if (!names.has(alias)) {
        result.push({ ...unit, unitName: alias });
        names.add(alias);
      }
    }
  }
  return result;
}

/** 下拉值包含快照名称，允许同一商品单位ID对应不同等价名称。 */
export function quoteUnitOptionValue(unit: Pick<QuoteUnit, 'id' | 'unitName'>): string {
  return JSON.stringify([unit.id, unit.unitName]);
}

/** 判断换算率是否为正数且最多两位小数，不对库存换算值静默取整。 */
export function isValidQuoteRate(value: unknown): boolean {
  return /^(?:\d+)(?:\.\d{1,2}0*)?$/.test(String(value)) && Number(value) > 0;
}

/** 用户明确选择单位后，统一更新单位及换算率快照。 */
export function applyQuoteUnit(row: Record<string, any>, unit: QuoteUnit): void {
  row.unitId = unit.id;
  row.unit = unit.unitName;
  row.unitName = unit.unitName;
  row.conversionRate = Number(unit.conversionRate);
}
