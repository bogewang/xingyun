export interface SaleOutTableRow {
  id?: string;
  productId?: string;
  unitId?: string;
  oriPrice?: string | number;
  taxPrice?: string | number;
  outNum?: string | number;
  description?: string;
  isFixed?: boolean;
}

const hasProductId = (row: SaleOutTableRow): row is SaleOutTableRow & { productId: string } =>
  !!row.productId;

export function buildUnrequiredSaleOutProducts(rows: SaleOutTableRow[]) {
  return rows.filter(hasProductId).map((row) => ({
    productId: row.productId,
    unitId: row.unitId,
    oriPrice: row.oriPrice,
    taxPrice: row.taxPrice,
    orderNum: row.outNum,
    description: row.description,
  }));
}

export function buildRequiredSaleOutProducts(rows: SaleOutTableRow[]) {
  return rows.filter(hasProductId).map((row) => {
    const product = {
      productId: row.productId,
      orderNum: row.outNum,
      description: row.description,
      oriPrice: row.oriPrice,
      taxPrice: row.taxPrice,
    };

    return row.isFixed ? { ...product, saleOrderDetailId: row.id } : product;
  });
}
