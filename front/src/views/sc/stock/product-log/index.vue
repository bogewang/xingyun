<template>
  <div v-permission="['stock:product-log:query']">
    <page-wrapper content-full-height fixed-height>
      <vxe-grid
        id="ProductStockLog"
        ref="grid"
        resizable
        show-overflow
        highlight-hover-row
        keep-source
        row-id="id"
        :proxy-config="proxyConfig"
        :columns="tableColumn"
        :toolbar-config="toolbarConfig"
        :custom-config="{}"
        :pager-config="{}"
        :loading="loading"
        :row-class-name="rowClassName"
        height="auto"
      >
        <template #form>
          <j-border>
            <j-form
              bordered
              label-width="100px"
              @collapse="$refs.grid.refreshColumn()"
              @keyup.enter="search"
            >
              <j-form-item label="商品编号">
                <a-input v-model:value="searchFormData.productCode" allow-clear />
              </j-form-item>
              <j-form-item label="商品名称">
                <a-input v-model:value="searchFormData.productName" allow-clear />
              </j-form-item>
              <j-form-item label="商品分类">
                <product-category-selector
                  v-model:value="searchFormData.categoryId"
                  :only-final="false"
                />
              </j-form-item>
              <j-form-item label="操作日期" :content-nest="false">
                <div class="date-range-container">
                  <a-date-picker
                    v-model:value="searchFormData.createStartTime"
                    placeholder=""
                    value-format="YYYY-MM-DD 00:00:00"
                  />
                  <span class="date-split">至</span>
                  <a-date-picker
                    v-model:value="searchFormData.createEndTime"
                    placeholder=""
                    value-format="YYYY-MM-DD 23:59:59"
                  />
                </div>
              </j-form-item>
              <j-form-item label="业务类型">
                <a-select v-model:value="searchFormData.bizType" placeholder="全部" allow-clear>
                  <a-select-option
                    v-for="item in PRODUCT_STOCK_BIZ_TYPE.values()"
                    :key="item.code"
                    :value="item.code"
                  >
                    {{ item.desc }}
                  </a-select-option>
                </a-select>
              </j-form-item>
            </j-form>
          </j-border>
        </template>

        <template #bizCode_default="{ row }">
          <div v-if="PRODUCT_STOCK_BIZ_TYPE.PURCHASE.equalsCode(row.bizType)">
            <a v-permission="['purchase:receive:modify']" @click="openBizModifyPage(row)">
              {{ row.bizCode }}
            </a>
            <span v-no-permission="['purchase:receive:modify']">{{ row.bizCode }}</span>
          </div>
          <div v-else-if="PRODUCT_STOCK_BIZ_TYPE.PURCHASE_RETURN.equalsCode(row.bizType)">
            <a v-permission="['purchase:return:modify']" @click="openBizModifyPage(row)">
              {{ row.bizCode }}
            </a>
            <span v-no-permission="['purchase:return:modify']">{{ row.bizCode }}</span>
          </div>
          <div v-else-if="PRODUCT_STOCK_BIZ_TYPE.SALE.equalsCode(row.bizType)">
            <a v-permission="['sale:out:modify']" @click="openBizModifyPage(row)">
              {{ row.bizCode }}
            </a>
            <span v-no-permission="['sale:out:modify']">{{ row.bizCode }}</span>
          </div>
          <div v-else-if="PRODUCT_STOCK_BIZ_TYPE.SALE_RETURN.equalsCode(row.bizType)">
            <a v-permission="['sale:return:modify']" @click="openBizModifyPage(row)">
              {{ row.bizCode }}
            </a>
            <span v-no-permission="['sale:return:modify']">{{ row.bizCode }}</span>
          </div>
          <div v-else-if="PRODUCT_STOCK_BIZ_TYPE.RETAIL.equalsCode(row.bizType)">
            <a v-permission="['retail:out:modify']" @click="openBizModifyPage(row)">
              {{ row.bizCode }}
            </a>
            <span v-no-permission="['retail:out:modify']">{{ row.bizCode }}</span>
          </div>
          <div v-else-if="PRODUCT_STOCK_BIZ_TYPE.RETAIL_RETURN.equalsCode(row.bizType)">
            <a v-permission="['retail:return:modify']" @click="openBizModifyPage(row)">
              {{ row.bizCode }}
            </a>
            <span v-no-permission="['retail:return:modify']">{{ row.bizCode }}</span>
          </div>
          <div
            v-else-if="
              PRODUCT_STOCK_BIZ_TYPE.TAKE_STOCK_IN.equalsCode(row.bizType) ||
              PRODUCT_STOCK_BIZ_TYPE.TAKE_STOCK_OUT.equalsCode(row.bizType)
            "
          >
            <a v-permission="['stock:take:sheet:modify']" @click="openBizModifyPage(row)">
              {{ row.bizCode }}
            </a>
            <span v-no-permission="['stock:take:sheet:modify']">{{ row.bizCode }}</span>
          </div>
          <div v-else-if="PRODUCT_STOCK_BIZ_TYPE.STOCK_ADJUST.equalsCode(row.bizType)">
            <a v-permission="['stock:adjust:modify']" @click="openBizModifyPage(row)">
              {{ row.bizCode }}
            </a>
            <span v-no-permission="['stock:adjust:modify']">{{ row.bizCode }}</span>
          </div>
          <div v-else-if="PRODUCT_STOCK_BIZ_TYPE.SC_TRANSFER.equalsCode(row.bizType)">
            <a v-permission="['stock:sc-transfer:modify']" @click="openBizModifyPage(row)">
              {{ row.bizCode }}
            </a>
            <span v-no-permission="['stock:sc-transfer:modify']">{{ row.bizCode }}</span>
          </div>
          <span v-else>{{ row.bizCode }}</span>
        </template>

        <template #toolbar_buttons>
          <a-space>
            <a-button type="primary" :icon="h(SearchOutlined)" @click="search">查询</a-button>
            <a-button
              v-permission="['stock:product-log:export']"
              type="primary"
              :icon="h(DownloadOutlined)"
              @click="exportList"
            >
              导出
            </a-button>
          </a-space>
        </template>
      </vxe-grid>
    </page-wrapper>
  </div>
</template>

<script>
  import { h, defineComponent } from 'vue';
  import { SearchOutlined, DownloadOutlined } from '@ant-design/icons-vue';
  import Moment from 'moment';
  import * as api from '@/api/sc/stock/product-stock-log';
  import * as purchaseReceiveApi from '@/api/sc/purchase/receive';
  import * as purchaseReturnApi from '@/api/sc/purchase/return';
  import * as saleOutApi from '@/api/sc/sale/out';
  import * as saleReturnApi from '@/api/sc/sale/return';
  import * as retailReturnApi from '@/api/sc/retail/return';
  import { multiplePageMix } from '@/mixins/multiplePageMix';
  import {
    formatDateTime,
    getDateTimeWithMinTime,
    getDateTimeWithMaxTime,
    buildSortPageVo,
    isEmpty,
  } from '@/utils/utils';
  import { createError, createSuccess } from '@/hooks/web/msg';
  import ProductBrandSelector from '@/components/Selector/ProductBrandSelector.vue';
  import ProductCategorySelector from '@/components/Selector/ProductCategorySelector.vue';
  import StoreCenterSelector from '@/components/Selector/StoreCenterSelector.vue';
  import { PRODUCT_STOCK_BIZ_TYPE } from '@/enums/biz/productStockBizType';

  export default defineComponent({
    name: 'ProductStockLog',
    components: {
      ProductBrandSelector,
      ProductCategorySelector,
      StoreCenterSelector,
    },
    mixins: [multiplePageMix],
    setup() {
      return {
        h,
        SearchOutlined,
        DownloadOutlined,
        PRODUCT_STOCK_BIZ_TYPE,
      };
    },
    data() {
      return {
        loading: false,
        searchFormData: {
          scId: '',
          productCode: '',
          productName: '',
          categoryId: '',
          brandId: '',
          createStartTime: formatDateTime(getDateTimeWithMinTime(Moment().subtract(1, 'M'))),
          createEndTime: formatDateTime(getDateTimeWithMaxTime(Moment())),
          bizType: undefined,
        },
        toolbarConfig: {
          slots: {
            buttons: 'toolbar_buttons',
          },
        },
        tableColumn: [
          { type: 'seq', width: 50 },
          {
            field: 'bizType',
            title: '业务类型',
            width: 100,
            formatter: ({ cellValue }) => {
              return PRODUCT_STOCK_BIZ_TYPE.getDesc(cellValue);
            },
            sortable: true,
          },
          { field: 'createTime', title: '操作时间', width: 150, sortable: true },
          { field: 'orderDate', title: '订单日期', width: 120 },
          {
            field: 'bizCode',
            title: '单据号',
            width: 180,
            slots: { default: 'bizCode_default' },
            sortable: true,
          },
          // { field: 'productCode', title: '商品编号', width: 120, sortable: true },
          { field: 'productName', title: '商品名称', width: 180 },
          {
            field: 'oriStockNum',
            title: '数量(前)',
            align: 'right',
            width: 100,
            sortable: true,
          },
          { field: 'stockNum', title: '变动数量', align: 'right', width: 100, sortable: true },
          {
            field: 'curStockNum',
            title: '数量(后)',
            align: 'right',
            width: 100,
            sortable: true,
          },
          {
            field: 'oriTaxPrice',
            title: '成本价(前)',
            align: 'right',
            width: 100,
            sortable: true,
          },
          {
            field: 'curTaxPrice',
            title: '成本价(后)',
            align: 'right',
            width: 100,
            sortable: true,
          },
          { field: 'taxAmount', title: '变动金额', align: 'right', width: 100, sortable: true },
          { field: 'createBy', title: '操作人', width: 150 },

        ],
        proxyConfig: {
          props: {
            result: 'datas',
            total: 'totalCount',
          },
          ajax: {
            query: ({ page, sorts }) => {
              return api.query(this.buildQueryParams(page, sorts));
            },
          },
        },
      };
    },
    methods: {
      search() {
        this.$refs.grid.commitProxy('reload');
      },
      buildQueryParams(page, sorts) {
        return {
          ...buildSortPageVo(page, sorts),
          ...this.buildSearchFormData(),
        };
      },
      buildSearchFormData() {
        return Object.assign({}, this.searchFormData, {
          scId: this.searchFormData.scId,
          categoryId: this.searchFormData.categoryId,
          brandId: this.searchFormData.brandId,
          supplierId: this.searchFormData.supplierId,
        });
      },
      exportList() {
        this.loading = true;
        api
          .exportList(this.buildQueryParams({}))
          .then(() => {
            createSuccess('创建导出任务成功，请前往“导出中心”进行下载。');
          })
          .finally(() => {
            this.loading = false;
          });
      },
      rowClassName({ row }) {
        const stockNum = Number(row.stockNum || 0);

        if (stockNum > 0) {
          return 'stock-log-row-in';
        }

        if (stockNum < 0) {
          return 'stock-log-row-out';
        }

        return '';
      },
      async openBizModifyPage(row) {
        try {
          const bizType = row.bizType;

          if (PRODUCT_STOCK_BIZ_TYPE.PURCHASE.equalsCode(bizType)) {
            const res = await purchaseReceiveApi.get(row.bizId);
            if (!isEmpty(res.purchaseOrderId)) {
              this.openChildPage('/purchase/receive/modify/require/' + row.bizId);
            } else {
              this.openChildPage('/purchase/receive/modify/un-require/' + row.bizId);
            }
            return;
          }

          if (PRODUCT_STOCK_BIZ_TYPE.PURCHASE_RETURN.equalsCode(bizType)) {
            const res = await purchaseReturnApi.get(row.bizId);
            if (!isEmpty(res.receiveSheetId)) {
              this.openChildPage('/purchase/return/modify/require/' + row.bizId);
            } else {
              this.openChildPage('/purchase/return/modify/un-require/' + row.bizId);
            }
            return;
          }

          if (PRODUCT_STOCK_BIZ_TYPE.SALE.equalsCode(bizType)) {
            const res = await saleOutApi.get(row.bizId);
            if (!isEmpty(res.saleOrderId)) {
              this.openChildPage('/sale/out/modify/require/' + row.bizId);
            } else {
              this.openChildPage('/sale/out/modify/un-require/' + row.bizId);
            }
            return;
          }

          if (PRODUCT_STOCK_BIZ_TYPE.SALE_RETURN.equalsCode(bizType)) {
            const res = await saleReturnApi.get(row.bizId);
            if (!isEmpty(res.outSheetId)) {
              this.openChildPage('/sale/return/modify/require/' + row.bizId);
            } else {
              this.openChildPage('/sale/return/modify/un-require/' + row.bizId);
            }
            return;
          }

          if (PRODUCT_STOCK_BIZ_TYPE.RETAIL.equalsCode(bizType)) {
            this.openChildPage('/retail/out/modify/' + row.bizId);
            return;
          }

          if (PRODUCT_STOCK_BIZ_TYPE.RETAIL_RETURN.equalsCode(bizType)) {
            const res = await retailReturnApi.get(row.bizId);
            if (!isEmpty(res.outSheetId)) {
              this.openChildPage('/retail/return/modify/require/' + row.bizId);
            } else {
              this.openChildPage('/retail/return/modify/un-require/' + row.bizId);
            }
            return;
          }

          if (
            PRODUCT_STOCK_BIZ_TYPE.TAKE_STOCK_IN.equalsCode(bizType) ||
            PRODUCT_STOCK_BIZ_TYPE.TAKE_STOCK_OUT.equalsCode(bizType)
          ) {
            this.openChildPage('/stock/take/sheet/modify/' + row.bizId);
            return;
          }

          if (PRODUCT_STOCK_BIZ_TYPE.STOCK_ADJUST.equalsCode(bizType)) {
            this.openChildPage('/stock/stock-adjust/modify/' + row.bizId);
            return;
          }

          if (PRODUCT_STOCK_BIZ_TYPE.SC_TRANSFER.equalsCode(bizType)) {
            this.openChildPage('/stock/stock-transfer/modify/' + row.bizId);
            return;
          }

          createError('当前单据暂不支持跳转修改页！');
        } catch (e) {
          createError('打开修改页面失败，请刷新后重试！');
        }
      },
    },
  });
</script>

<style scoped>
  :deep(.stock-log-row-in) {
    color: #389e0d;
  }

  :deep(.stock-log-row-in a) {
    color: inherit;
  }

  :deep(.stock-log-row-out) {
    color: #cf1322;
  }

  :deep(.stock-log-row-out a) {
    color: inherit;
  }
</style>
