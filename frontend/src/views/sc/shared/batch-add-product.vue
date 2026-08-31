<template>
  <a-modal
    v-model:open="visible"
    :mask-closable="false"
    width="70%"
    title="批量添加商品"
    :style="{ top: '20px' }"
  >
    <div v-if="visible" v-permission="permissionCodes" @keydown.enter.prevent="handleEnterSearch">
      <vxe-grid
        v-if="visible"
        :id="gridId"
        ref="grid"
        resizable
        show-overflow
        highlight-hover-row
        keep-source
        :row-id="rowId"
        height="800"
        :proxy-config="proxyConfig"
        :columns="tableColumn"
        :toolbar-config="toolbarConfig"
        :custom-config="{}"
        :pager-config="{}"
        :checkbox-config="{
          trigger: 'row',
          highlight: true,
        }"
        :loading="loading"
      >
        <template #form>
          <j-border>
            <j-form bordered>
              <j-form-item label="商品">
                <a-input v-model:value="searchFormData.condition" allow-clear />
              </j-form-item>
              <j-form-item label="商品分类">
                <product-category-selector
                  v-model:value="searchFormData.categoryId"
                  :only-final="false"
                />
              </j-form-item>
              <j-form-item v-if="bizType !== 'quote'" label="商品品牌">
                <product-brand-selector
                  v-model:value="searchFormData.brandId"
                  :request-params="{ available: true }"
                />
              </j-form-item>
            </j-form>
          </j-border>
        </template>
        <template #toolbar_buttons>
          <a-space>
            <a-button type="primary" :icon="h(SearchOutlined)" @click="search">查询</a-button>
          </a-space>
        </template>
        <template #inquiryProduct_default="{ row }">
          <span :class="formatInquiryProduct(row.inquiryProduct).className">
            {{ formatInquiryProduct(row.inquiryProduct).text }}
          </span>
        </template>
      </vxe-grid>
    </div>
    <template #footer>
      <a-space>
        <a-button @click="closeDialog">取 消</a-button>
        <a-button v-permission="permissionCodes" type="primary" :loading="loading" @click="doSelect"
          >确 定</a-button
        >
      </a-space>
    </template>
  </a-modal>
</template>

<script>
  import { h, defineComponent } from 'vue';
  import { SearchOutlined } from '@ant-design/icons-vue';
  import * as saleApi from '@/api/sc/sale/order';
  import * as productApi from '@/api/base-data/product/info';
  import * as unitApi from '@/api/base-data/unit';
  import { isEmpty } from '@/utils/utils';
  import { createError } from '@/hooks/web/msg';
  import ProductBrandSelector from '@/components/Selector/ProductBrandSelector.vue';
  import ProductCategorySelector from '@/components/Selector/ProductCategorySelector.vue';
  import { formatInquiryProduct } from '@/views/sc/components/inquiryProduct';

  export default defineComponent({
    name: 'SharedBatchAddProduct',
    components: {
      ProductBrandSelector,
      ProductCategorySelector,
    },
    props: {
      bizType: {
        type: String,
        required: true,
        validator(value) {
          return ['purchase', 'sale', 'quote'].includes(value);
        },
      },
      scId: {
        type: String,
        default: '',
      },
      isReturn: {
        type: Boolean,
        default: false,
      },
      showInquiryProduct: {
        type: Boolean,
        default: false,
      },
      /** 单据日期，后端按需过滤可售商品 */
      orderDate: {
        type: String,
        default: '',
      },
      /** 报价单 ID，后端据此排除已有明细商品。 */
      quoteSheetId: {
        type: String,
        default: '',
      },
    },
    setup() {
      return {
        h,
        SearchOutlined,
        formatInquiryProduct,
      };
    },
    data() {
      return {
        visible: false,
        loading: false,
        searchFormData: {
          condition: '',
          categoryId: '',
          brandId: '',
        },
        // 报价单批量添加时缓存单位 ID 到名称的映射。
        quoteUnitNameMap: {},
        quoteUnitLoadPromise: null,
        toolbarConfig: {
          custom: true,
          refresh: false,
          slots: {
            buttons: 'toolbar_buttons',
          },
        },
        tableColumn: this.bizType === 'quote' ? [
          { type: 'checkbox', width: 45 },
          { type: 'seq', width: 50, title: '序号' },
          { field: 'code', title: '商品编号', width: 140 },
          { field: 'name', title: '商品名称', minWidth: 180 },
          { field: 'unit', title: '单位', width: 80 },
          { field: 'spec', title: '规格', width: 120 },
          { field: 'categoryName', title: '商品分类', width: 140 },
        ] : [
          { type: 'checkbox', width: 45 },
          { type: 'seq', width: 50, title: '序号' },
          { field: 'productName', title: '商品名称', minWidth: 150 },
          ...(this.showInquiryProduct
            ? [
                {
                  field: 'inquiryProduct',
                  title: '是否询价商品',
                  width: 120,
                  slots: { default: 'inquiryProduct_default' },
                },
              ]
            : []),
          { field: 'unit', title: '单位', width: 80 },
          { field: 'spec', title: '规格', width: 120 },
          { field: 'categoryName', title: '商品分类', width: 120 },
          { field: 'purchasePrice', title: '采购参考价（元）', align: 'right', width: 140 },
          {
            field: 'latestPurchasePrice',
            title: '采购最新价（元）',
            align: 'right',
            width: 140,
          },
          { field: 'salePrice', title: '销售参考价（元）', align: 'right', width: 140 },
          { field: 'latestSalePrice', title: '销售最新价（元）', align: 'right', width: 140 },
          { field: 'stockNum', title: '库存数量', align: 'right', width: 100 },
        ],
        proxyConfig: {
          props: {
            result: 'datas',
            total: 'totalCount',
          },
          ajax: {
            query: ({ page }) => {
              return this.queryProductList(this.buildQueryParams(page));
            },
          },
        },
      };
    },
    computed: {
      gridId() {
        return `ScBatchAddProduct_${this.bizType}`;
      },
      rowId() {
        return this.bizType === 'quote' ? 'id' : 'productId';
      },
      permissionCodes() {
        if (this.bizType === 'purchase') {
          return [
            'purchase:order:add',
            'purchase:order:modify',
            'purchase:receive:add',
            'purchase:receive:modify',
            'purchase:return:add',
            'purchase:return:modify',
          ];
        }

        if (this.bizType === 'quote') {
          return ['base-data:quote:modify'];
        }

        return [
          'sale:order:add',
          'sale:order:modify',
          'sale:out:add',
          'sale:out:modify',
          'sale:return:add',
          'sale:return:modify',
        ];
      },
    },
    methods: {
      handleEnterSearch(e) {
        if (e.target?.closest('.ant-modal-footer')) {
          return;
        }

        this.search();
      },
      queryProductList(params) {
        if (this.bizType === 'quote') {
          return this.loadQuoteUnitNames().then(() =>
            productApi.selector(params).then((data) => ({
              ...data,
              datas: (data.datas || []).map((product) => ({
                ...product,
                unit: this.quoteUnitNameMap[product.unit] || product.unit,
              })),
            })),
          );
        }
        return saleApi.querySaleProductList({ ...params, orderDate: this.orderDate || undefined });
      },
      // 加载报价单商品单位名称，避免批量选择表格展示单位 ID。
      loadQuoteUnitNames() {
        if (this.quoteUnitLoadPromise) {
          return this.quoteUnitLoadPromise;
        }

        this.quoteUnitLoadPromise = unitApi.query({ pageIndex: 1, pageSize: 1000 }).then((data) => {
          this.quoteUnitNameMap = (data.datas || []).reduce((result, item) => {
            result[item.id] = item.name;
            return result;
          }, {});
        });
        return this.quoteUnitLoadPromise;
      },
      search() {
        this.$refs.grid.commitProxy('reload');
      },
      buildQueryParams(page) {
        return {
          pageIndex: page.currentPage,
          pageSize: page.pageSize,
          ...this.buildSearchFormData(),
        };
      },
      buildSearchFormData() {
        return {
          condition: this.searchFormData.condition,
          categoryId: this.searchFormData.categoryId || '',
          ...(this.bizType === 'quote'
            ? { quoteSheetId: this.quoteSheetId }
            : {
                brandId: this.searchFormData.brandId || '',
                isReturn: this.isReturn,
                scId: this.scId,
              }),
        };
      },
      openDialog() {
        this.visible = true;
        this.$nextTick(() => this.open());
      },
      closeDialog() {
        this.visible = false;
        this.$emit('close');
      },
      open() {},
      doSelect() {
        const records = this.$refs.grid.getCheckboxRecords();
        if (isEmpty(records)) {
          createError('请选择商品数据！');
          return;
        }

        this.$emit('confirm', records);
        this.closeDialog();
      },
    },
  });
</script>
