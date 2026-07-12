import { BaseEnum, BaseEnumItem } from '@/enums/baseEnum';

const PRODUCT_TYPE: BaseEnum<number, string> = new BaseEnum<number, string>();
PRODUCT_TYPE.set('NORMAL', new BaseEnumItem<number, string>(1, '普通商品'));

export { PRODUCT_TYPE };
