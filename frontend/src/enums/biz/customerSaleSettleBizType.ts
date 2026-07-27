import { BaseEnum, BaseEnumItem } from '@/enums/baseEnum';

const CUSTOMER_SALE_SETTLE_BIZ_TYPE: BaseEnum<number, string> = new BaseEnum<number, string>();
CUSTOMER_SALE_SETTLE_BIZ_TYPE.set(
  'OUT_SHEET',
  new BaseEnumItem<number, string>(1, '销售出库单'),
);
CUSTOMER_SALE_SETTLE_BIZ_TYPE.set(
  'SALE_RETURN',
  new BaseEnumItem<number, string>(2, '销售退单'),
);

export { CUSTOMER_SALE_SETTLE_BIZ_TYPE };
