<template>
  <!-- eslint-disable vue/no-mutating-props row prop 由父组件传入，变异是设计行为 -->
  <div class="inline-product-select">
    <a-auto-complete
      v-if="showAutoCompleteForRow"
      ref="autoCompleteRef"
      v-model:value="row.productQuery"
      :placeholder="isQuote ? '请输入商品编号/名称/SKU编号' : '请输入商品编号/名称/SKU编号/简码'"
      :options="row.productOptions"
      :dropdown-match-select-width="false"
      :dropdown-style="{ width: dropdownWidth }"
      placement="bottomLeft"
      @focus="onProductInputFocus"
      @search="(e) => queryProduct(e, row)"
      @keydown="(e) => handleProductSelectKeydown(e, row, rowIndex)"
    >
      <!-- 自定义下拉框内容 -->
      <template #dropdownRender>
        <div v-if="!isEmpty(row.products)" @mousedown.prevent @click.stop>
          <vxe-table
            :data="row.products"
            max-height="360"
            class="cursor-pointer"
            highlight-hover-row
            show-overflow
            :row-config="{ isHover: true }"
            :row-class-name="({ row: product }) => getProductSelectRowClass(row, product)"
            @cell-click="({ row: product }) => emit('select', rowIndex, product)"
          >
            <!-- 序号 -->
            <vxe-column type="seq" title="序号" width="60" />
            <!-- 报价和进销模式共用商品名称，始终展示热度星标 -->
            <vxe-column field="productName" title="商品名称" min-width="250">
              <template #default="{ row: product }">
                <span>{{ product.productName }}</span>
                <span v-if="product.hotLevel" class="inline-product-hot-stars">
                  <StarTwoTone
                    v-for="star in product.hotLevel"
                    :key="star"
                    two-tone-color="#faad14"
                  />
                </span>
              </template>
            </vxe-column>
            <!-- 是否询价商品（进销模式显示） -->
            <vxe-column v-if="!isQuote" field="inquiryProduct" title="是否询价商品" width="70">
              <template #default="{ row: product }">
                <span :class="formatInquiryProduct(product.inquiryProduct).className">
                  {{ formatInquiryProduct(product.inquiryProduct).text }}
                </span>
              </template>
            </vxe-column>
            <!-- 报价和进销模式共用字段 -->
            <vxe-column field="spec" title="规格" width="120" />
            <vxe-column field="unit" title="单位" width="70">
              <template #default="{ row: product }">
                {{ getUnitName(product.unit) }}
              </template>
            </vxe-column>
            <!-- 库存数量（进销模式显示） -->
            <vxe-column
              v-if="!isQuote"
              field="stockNum"
              title="库存数量"
              width="80"
              align="right"
            />
            <!-- 价格列（进销模式显示） -->
            <vxe-column
              v-if="!isQuote"
              field="purchasePrice"
              title="采购价（元）"
              width="80"
              align="right"
            />
            <vxe-column
              v-if="!isQuote"
              field="latestPurchasePrice"
              title="最新采购价（元）"
              width="80"
              align="right"
            />
            <vxe-column
              v-if="!isQuote"
              field="salePrice"
              title="销售价（元）"
              width="80"
              align="right"
            />
            <vxe-column
              v-if="!isQuote"
              field="latestSalePrice"
              title="最新销售价（元）"
              width="80"
              align="right"
            />
            <!-- 备注（进销模式显示并固定在最后一列） -->
            <vxe-column v-if="!isQuote" field="remark" title="备注" min-width="180" />
          </vxe-table>
          <div
            v-if="!isQuote"
            class="inline-product-select-add"
            @mousedown.prevent
            @click.stop="emit('openAddProductPage')"
          >
            + 新增商品
          </div>
        </div>
      </template>
    </a-auto-complete>
    <!-- 已选商品展示 -->
    <span v-else :style="clickableStyle" @click="onEnableProductEdit">{{
      isQuote ? row.name : row.productName
    }}</span>
  </div>
</template>

<script>
  import { computed, defineComponent, toRef } from 'vue';
  import { StarTwoTone } from '@ant-design/icons-vue';
  import { formatInquiryProduct } from '@/views/sc/components/inquiryProduct';
  import * as saleApi from '@/api/sc/sale/order';
  import * as productApi from '@/api/base-data/product/info';
  import { isEmpty } from '@/utils/utils';
  import { useInlineProductSelect } from '@/utils/inlineProductSelect';

  export default defineComponent({
    name: 'InlineProductSelect',
    components: {
      StarTwoTone,
    },
    props: {
      /** 行数据 */
      row: { type: Object, required: true },
      /** 行索引 */
      rowIndex: { type: Number, required: true },
      /** 业务类型：purchase=采购入库, sale=销售出库, quote=报价单 */
      bizType: {
        type: String,
        required: true,
        validator: (v) => ['purchase', 'sale', 'quote'].includes(v),
      },
      /** 模式：require=有订单, unrequire=无订单 */
      mode: {
        type: String,
        required: true,
        validator: (v) => ['require', 'unrequire'].includes(v),
      },
      /** 仓库ID（报价单模式不需要） */
      scId: { type: String, default: '' },
      /** 行是否固定（有订单模式下来自订单的行不可编辑） */
      isFixed: { type: Boolean, default: false },
      /** 下拉宽度 */
      dropdownWidth: { type: String, default: '1260px' },
      /** 单据日期，后端按需过滤可售商品 */
      orderDate: { type: String, default: '' },
      /** 计量单位 ID 到名称的映射，仅报价单模式使用 */
      unitNameMap: { type: Object, default: () => ({}) },
    },
    emits: ['select', 'addProduct', 'openAddProductPage'],
    setup(props, { emit, expose }) {
      // 是否报价单模式
      const isQuote = computed(() => props.bizType === 'quote');

      // 将报价单商品接口中的单位 ID 转为用户可读的单位名称。
      const getUnitName = (unit) => props.unitNameMap[unit] || unit;

      // 商品搜索：报价单模式走商品中心查询所有非停用基础商品，进销模式走后端可售商品搜索
      const searchProducts = (keyword) => {
        if (isQuote.value) {
          return productApi
            .selector({
              pageIndex: 1,
              pageSize: 100,
              condition: keyword,
            })
            .then((data) =>
              (data.datas || []).map((product) => ({
                ...product,
                productName: product.name,
              })),
            );
        }
        return saleApi.searchSaleProducts(props.scId, keyword, props.orderDate);
      };

      const {
        autoCompleteRef,
        showAutoComplete,
        queryProduct,
        handleProductInputFocus,
        handleProductSelectKeydown,
        getProductSelectRowClass,
        handleEnableProductEdit,
        focus,
      } = useInlineProductSelect(toRef(props, 'row'), searchProducts, emit);

      // 有订单模式的固定行不可编辑
      const rowEditable = !(props.mode === 'require' && props.isFixed);

      // 是否显示auto-complete输入框（固定行不显示）
      const showAutoCompleteForRow = computed(() => {
        return rowEditable && showAutoComplete.value;
      });

      // 可点击样式（有订单模式的固定行不可点击）
      const clickableStyle = computed(() => {
        return rowEditable ? 'color: #1677ff; cursor: pointer' : '';
      });

      // 聚焦时用已选商品名称作为兜底关键字
      const onProductInputFocus = () =>
        handleProductInputFocus(isQuote.value ? props.row.name : props.row.productName);

      // 启用编辑模式
      const onEnableProductEdit = () => handleEnableProductEdit(rowEditable);

      expose({ focus });

      return {
        autoCompleteRef,
        showAutoCompleteForRow,
        clickableStyle,
        queryProduct,
        onProductInputFocus,
        handleProductSelectKeydown,
        getProductSelectRowClass,
        onEnableProductEdit,
        focus,
        isQuote,
        getUnitName,
        isEmpty,
        formatInquiryProduct,
        StarTwoTone,
        emit,
      };
    },
  });
</script>

<style scoped>
  .inline-product-select {
    width: 100%;
  }

  /* 与表格内其他输入框保持相同的尺寸和边框，避免自动聚焦时出现突兀的高亮。 */
  .inline-product-select :deep(.ant-select) {
    display: block;
    width: 100%;
  }

  .inline-product-select :deep(.ant-select-selector),
  .inline-product-select :deep(.ant-select-focused .ant-select-selector) {
    border-color: #d9d9d9 !important;
    box-shadow: none !important;
  }
</style>
