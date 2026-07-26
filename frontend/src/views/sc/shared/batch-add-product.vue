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
        row-id="productId"
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
              <j-form-item label="商品品牌">
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
          return ['purchase', 'sale'].includes(value);
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
        toolbarConfig: {
          custom: true,
          refresh: false,
          slots: {
            buttons: 'toolbar_buttons',
          },
        },
        tableColumn: [
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
        return saleApi.querySaleProductList(params);
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
          isReturn: this.isReturn,
          scId: this.scId,
          condition: this.searchFormData.condition,
          categoryId: this.searchFormData.categoryId || '',
          brandId: this.searchFormData.brandId || '',
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
