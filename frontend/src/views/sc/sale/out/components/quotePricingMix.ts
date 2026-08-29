import * as saleOutApi from '@/api/sc/sale/out';
import type { QuoteProductBo } from '@/api/sc/sale/out/model/quoteProductBo';
import * as sysParameterApi from '@/api/system/parameter';
import { useUserStoreWithOut } from '/@/store/modules/user';
import { createError } from '@/hooks/web/msg';
import {
  applyQuoteProducts,
  hasInvalidQuoteProducts,
  normalizeQuoteProducts,
} from './quoteProductPricing';

/** 销售出库唯一报价模式的页面共享行为。 */
export const quotePricingMix = {
  data() {
    return {
      saleOutPriceUseUniquePrice: false,
      quoteProducts: [] as QuoteProductBo[],
    };
  },
  watch: {
    'formData.orderDate'() {
      this.refreshQuoteProducts();
    },
  },
  methods: {
    /** 加载唯一报价开关并获取当前日期可销售商品。 */
    async loadQuotePricingSetting() {
      const tenantId = (await useUserStoreWithOut().getTenantRequire())?.tenantId;
      if (!tenantId) {
        this.saleOutPriceUseUniquePrice = false;
        this.quoteProducts = [];
        return;
      }

      const res = await sysParameterApi.query({
        pageIndex: 1,
        pageSize: 1,
        tenantId,
        pmKey: 'sale_out_price_use_unique_price',
        createTimeStart: '',
        createTimeEnd: '',
      });
      this.saleOutPriceUseUniquePrice = res.datas?.[0]?.pmValue === 'true';
      await this.refreshQuoteProducts();
    },
    /** 根据单据日期刷新有效报价商品，并更新仍可售商品的价格。 */
    async refreshQuoteProducts() {
      if (!this.saleOutPriceUseUniquePrice || !this.formData?.orderDate) {
        this.quoteProducts = [];
        if (this.tableData) {
          this.tableData = this.tableData.map((row) => ({ ...row, quoteInvalid: false }));
        }
        return;
      }

      const products = await saleOutApi.queryQuoteProducts({
        orderDate: this.formData.orderDate,
      });
      this.quoteProducts = normalizeQuoteProducts(products || []);
      this.applyCurrentQuoteProducts();
    },
    /** 对当前已录入商品应用报价价格。 */
    applyCurrentQuoteProducts() {
      if (!this.saleOutPriceUseUniquePrice || !this.tableData) {
        return;
      }
      this.tableData = applyQuoteProducts(this.tableData, this.quoteProducts);
      this.tableData.forEach((row) => this.taxPriceInput?.(row, row.taxPrice));
      this.calcSum?.();
    },
    /** 选择商品时取得报价价，关闭开关则保持原先最新售价逻辑。 */
    getSelectedProductPrice(product) {
      return this.saleOutPriceUseUniquePrice ? product.salePrice : product.latestSalePrice;
    },
    /** 保存前阻止包含失效报价商品的单据。 */
    validQuoteProducts() {
      if (this.saleOutPriceUseUniquePrice && hasInvalidQuoteProducts(this.tableData || [])) {
        createError('存在不在当前报价单内的商品，请删除或重新选择商品后再保存！');
        return false;
      }
      return true;
    },
  },
};
