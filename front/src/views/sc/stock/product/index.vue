<template>
  <div v-permission="['stock:product:query']">
    <page-wrapper content-full-height fixed-height>
      <!-- 数据列表 -->
      <vxe-grid
        id="ProductStock"
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
        height="auto"
      >
        <template #form>
          <j-border>
            <j-form
              bordered
              label-width="80px"
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
              <j-form-item label="库存数量">
                <a-space>
                  <a-input-number
                    v-model:value="searchFormData.stockNumStart"
                    :precision="2"
                    placeholder="最小值"
                    style="width: 120px"
                  />
                  <span>至</span>
                  <a-input-number
                    v-model:value="searchFormData.stockNumEnd"
                    :precision="2"
                    placeholder="最大值"
                    style="width: 120px"
                  />
                </a-space>
              </j-form-item>
            </j-form>
          </j-border>
        </template>
        <!-- 工具栏 -->
        <template #toolbar_buttons>
          <a-space>
            <a-button type="primary" :icon="h(SearchOutlined)" @click="search">查询</a-button>
            <a-button
              v-permission="['stock:product:approve']"
              type="primary"
              :icon="h(SyncOutlined)"
              @click="rebuildByReceiveSale"
              >重建库存</a-button
            >
            <a-button
              v-permission="['stock:product:export']"
              type="primary"
              :icon="h(DownloadOutlined)"
              @click="exportList"
              >导出</a-button
            >
          </a-space>
        </template>
        <template #operation_default="{ row }">
          <a-button
            v-permission="['stock:adjust:approve']"
            type="link"
            size="small"
            @click="openStockAdjust(row)"
            >调整库存</a-button
          >
        </template>
      </vxe-grid>

      <a-modal
        v-model:open="stockAdjustVisible"
        title="调整库存"
        :confirm-loading="stockAdjustLoading"
        @ok="submitStockAdjust"
      >
        <j-form bordered label-width="100px">
          <j-form-item label="商品">
            <a-input :value="stockAdjustData.productName" readonly />
          </j-form-item>
          <j-form-item label="当前库存">
            <a-input :value="stockAdjustData.curStockNum" readonly />
          </j-form-item>
          <j-form-item label="业务类型" required>
            <a-select v-model:value="stockAdjustData.bizType">
              <a-select-option
                v-for="item in STOCK_ADJUST_SHEET_BIZ_TYPE.values()"
                :key="item.code"
                :value="item.code"
                >{{ item.desc }}</a-select-option
              >
            </a-select>
          </j-form-item>
          <j-form-item label="调整数量" required>
            <a-input-number
              v-model:value="stockAdjustData.stockNum"
              :min="0"
              :precision="8"
              style="width: 100%"
            />
          </j-form-item>
          <j-form-item label="调整原因" required>
            <a-select
              v-model:value="stockAdjustData.reasonId"
              show-search
              option-filter-prop="label"
            >
              <a-select-option
                v-for="item in reasonOptions"
                :key="item.id"
                :value="item.id"
                :label="item.name"
                >{{ item.name }}</a-select-option
              >
            </a-select>
          </j-form-item>
          <j-form-item label="备注" :span="24">
            <a-textarea v-model:value.trim="stockAdjustData.description" :maxlength="200" />
          </j-form-item>
        </j-form>
      </a-modal>
    </page-wrapper>
  </div>
</template>

<script>
  import { h, defineComponent } from 'vue';
  import { SearchOutlined, DownloadOutlined, SyncOutlined } from '@ant-design/icons-vue';
  import * as api from '@/api/sc/stock/product-stock';
  import * as stockAdjustApi from '@/api/sc/stock/adjust/stock';
  import * as stockAdjustReasonApi from '@/api/sc/stock/adjust/reason';
  import {
    buildSortPageVo,
    isEmpty,
    isFloat,
    isFloatGtZero,
    isNumberPrecision,
  } from '@/utils/utils';
  import { createConfirm, createError, createSuccess } from '@/hooks/web/msg';
  import { STOCK_ADJUST_SHEET_BIZ_TYPE } from '@/enums/biz/stockAdjustSheetBizType';
  import ProductCategorySelector from '@/components/Selector/ProductCategorySelector.vue';

  export default defineComponent({
    name: 'ProductStock',
    components: {
      ProductCategorySelector,
    },
    setup() {
      return {
        h,
        SearchOutlined,
        DownloadOutlined,
        SyncOutlined,
        STOCK_ADJUST_SHEET_BIZ_TYPE,
      };
    },
    data() {
      return {
        loading: false,
        stockAdjustVisible: false,
        stockAdjustLoading: false,
        stockAdjustData: {},
        reasonOptions: [],
        // 当前行数据
        id: '',
        ids: [],
        // 查询列表的查询条件
        searchFormData: {
          scId: '',
          productCode: '',
          productName: '',
          categoryId: '',
          brandId: '',
          stockNumStart: undefined,
          stockNumEnd: undefined,
        },
        // 工具栏配置
        toolbarConfig: {
          // 自定义左侧工具栏
          slots: {
            buttons: 'toolbar_buttons',
          },
        },
        // 列表数据配置
        tableColumn: [
          { type: 'seq', width: 50 },
          { field: 'productCode', title: '商品编号', width: 120, sortable: true },
          { field: 'productName', title: '商品名称', width: 180 },
          { field: 'categoryName', title: '商品分类', width: 120 },
          { field: 'unit', title: '单位', width: 80 },
          { field: 'stockNum', title: '库存数量', align: 'right', width: 100, sortable: true },
          { field: 'taxPrice', title: '价格', align: 'right', width: 100 },
          { field: 'taxAmount', title: '金额', align: 'right', width: 100 },
          {
            field: 'operation',
            title: '操作',
            width: 100,
            fixed: 'right',
            slots: { default: 'operation_default' },
          },
        ],
        // 请求接口配置
        proxyConfig: {
          props: {
            // 响应结果列表字段
            result: 'datas',
            // 响应结果总条数字段
            total: 'totalCount',
          },
          ajax: {
            // 查询接口
            query: ({ page, sorts }) => {
              return api.query(this.buildQueryParams(page, sorts));
            },
          },
        },
      };
    },
    created() {},
    methods: {
      // 列表发生查询时的事件
      search() {
        this.$refs.grid.commitProxy('reload');
      },
      // 查询前构建查询参数结构
      buildQueryParams(page, sorts) {
        return {
          ...buildSortPageVo(page, sorts),
          ...this.buildSearchFormData(),
        };
      },
      // 查询前构建具体的查询参数
      buildSearchFormData() {
        const params = Object.assign({}, this.searchFormData, {
          scId: this.searchFormData.scId,
          categoryId: this.searchFormData.categoryId,
          brandId: this.searchFormData.brandId,
          stockNumStart: this.searchFormData.stockNumStart,
          stockNumEnd: this.searchFormData.stockNumEnd,
        });

        return params;
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
      rebuildByReceiveSale() {
        createConfirm('将按单据日期重放采购收货单和销售出库单，重新生成库存数据，是否继续？').then(
          () => {
            this.loading = true;
            api
              .rebuildByReceiveSale()
              .then(() => {
                createSuccess('库存重建成功。');
                this.search();
              })
              .finally(() => {
                this.loading = false;
              });
          },
        );
      },
      openStockAdjust(row) {
        this.stockAdjustData = {
          productId: row.productId,
          productName: `${row.productCode} ${row.productName}`,
          curStockNum: row.stockNum,
          bizType: '',
          stockNum: undefined,
          reasonId: '',
          description: '',
        };
        this.stockAdjustVisible = true;
        this.loadReasonOptions();
      },
      validStockAdjustData() {
        if (isEmpty(this.stockAdjustData.bizType)) {
          createError('请选择业务类型！');
          return false;
        }
        if (isEmpty(this.stockAdjustData.reasonId)) {
          createError('请选择调整原因！');
          return false;
        }
        if (isEmpty(this.stockAdjustData.stockNum)) {
          createError('调整数量不允许为空！');
          return false;
        }
        if (
          !isFloat(this.stockAdjustData.stockNum) ||
          !isFloatGtZero(this.stockAdjustData.stockNum)
        ) {
          createError('调整数量必须大于0！');
          return false;
        }
        if (!isNumberPrecision(this.stockAdjustData.stockNum, 8)) {
          createError('调整数量最多允许8位小数！');
          return false;
        }
        return true;
      },
      submitStockAdjust() {
        if (!this.validStockAdjustData()) {
          return;
        }
        this.stockAdjustLoading = true;
        stockAdjustApi
          .directApprovePass({
            bizType: this.stockAdjustData.bizType,
            reasonId: this.stockAdjustData.reasonId,
            description: this.stockAdjustData.description,
            products: [
              {
                productId: this.stockAdjustData.productId,
                stockNum: this.stockAdjustData.stockNum,
                description: '',
              },
            ],
          })
          .then(() => {
            createSuccess('库存调整成功！');
            this.stockAdjustVisible = false;
            this.search();
          })
          .finally(() => {
            this.stockAdjustLoading = false;
          });
      },
      loadReasonOptions() {
        stockAdjustReasonApi
          .selector({
            pageIndex: 1,
            pageSize: 500,
            code: '',
            name: '',
            available: true,
          })
          .then((res) => {
            this.reasonOptions = res.datas || [];
          });
      },
    },
  });
</script>
<style scoped></style>
