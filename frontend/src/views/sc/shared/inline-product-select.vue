<template>
  <!-- eslint-disable vue/no-mutating-props row prop 由父组件传入，变异是设计行为 -->
  <div class="inline-product-select">
    <a-auto-complete
      v-if="showAutoComplete"
      ref="autoCompleteRef"
      v-model:value="row.productQuery"
      placeholder="请输入商品编号/名称/SKU编号/简码"
      :options="row.productOptions"
      :dropdown-match-select-width="false"
      :dropdown-style="{ width: dropdownWidth }"
      placement="bottomLeft"
      @focus="handleProductInputFocus"
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
            <!-- 商品名称（始终显示，含热度星标） -->
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
            <!-- 是否询价商品（始终显示） -->
            <vxe-column field="inquiryProduct" title="是否询价商品" width="70">
              <template #default="{ row: product }">
                <span :class="formatInquiryProduct(product.inquiryProduct).className">
                  {{ formatInquiryProduct(product.inquiryProduct).text }}
                </span>
              </template>
            </vxe-column>
            <!-- 规格（始终显示） -->
            <vxe-column field="spec" title="规格" width="120" />
            <!-- 单位（始终显示） -->
            <vxe-column field="unit" title="单位" width="60" />
            <!-- 库存数量（始终显示） -->
            <vxe-column field="stockNum" title="库存数量" width="60" align="right" />
            <!-- 价格列（始终显示） -->
            <vxe-column field="purchasePrice" title="采购价（元）" width="80" align="right" />
            <vxe-column
              field="latestPurchasePrice"
              title="最新采购价（元）"
              width="80"
              align="right"
            />
            <vxe-column field="salePrice" title="销售价（元）" width="80" align="right" />
            <vxe-column
              field="latestSalePrice"
              title="最新销售价（元）"
              width="80"
              align="right"
            />
            <!-- 备注（始终显示并固定在最后一列） -->
            <vxe-column field="remark" title="备注" min-width="180" />
          </vxe-table>
          <div
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
    <span v-else :style="clickableStyle" @click="handleEnableProductEdit">{{
      row.productName
    }}</span>
  </div>
</template>

<script>
  import { computed, defineComponent, nextTick, ref } from 'vue';
  import { StarTwoTone } from '@ant-design/icons-vue';
  import { formatInquiryProduct } from '@/views/sc/components/inquiryProduct';
  import * as saleApi from '@/api/sc/sale/order';
  import { isEmpty } from '@/utils/utils';
  import {
    getInlineProductSelectRowClass,
    handleEmptyProductInputEnter,
    handleInlineProductSelectKeydown,
    resetInlineProductSelect,
    setInlineProductSelectProducts,
  } from '@/utils/inlineProductSelect';

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
      /** 业务类型：purchase=采购入库, sale=销售出库 */
      bizType: { type: String, required: true, validator: (v) => ['purchase', 'sale'].includes(v) },
      /** 模式：require=有订单, unrequire=无订单 */
      mode: {
        type: String,
        required: true,
        validator: (v) => ['require', 'unrequire'].includes(v),
      },
      /** 仓库ID */
      scId: { type: String, required: true },
      /** 行是否固定（有订单模式下来自订单的行不可编辑） */
      isFixed: { type: Boolean, default: false },
      /** 下拉宽度 */
      dropdownWidth: { type: String, default: '1260px' },
    },
    emits: ['select', 'addProduct', 'openAddProductPage'],
    setup(props, { emit, expose }) {
      const autoCompleteRef = ref(null);

      // 是否显示auto-complete输入框
      const showAutoComplete = computed(() => {
        if (props.mode === 'require' && props.isFixed) {
          return false;
        }
        return isEmpty(props.row.productId) || props.row.editingProduct;
      });

      // 可点击样式（有订单模式的固定行不可点击）
      const clickableStyle = computed(() => {
        if (props.mode === 'require' && props.isFixed) {
          return '';
        }
        return 'color: #1677ff; cursor: pointer';
      });

      // 搜索商品
      const queryProduct = (queryString, row) => {
        if (isEmpty(queryString)) {
          row.products = [];
          row.productOptions = [];
          resetInlineProductSelect(row);
          return;
        }

        saleApi.searchSaleProducts(props.scId, queryString).then((res) => {
          setInlineProductSelectProducts(row, res);
          row.productOptions = res.map((item) => ({
            value: item.productId,
            label: item.productCode + ' ' + item.productName,
          }));
        });
      };

      // 输入框聚焦时触发搜索
      const handleProductInputFocus = () => {
        const keyword = props.row.productQuery || props.row.productName;
        if (isEmpty(keyword)) return;

        queryProduct(keyword, props.row);
      };

      // 键盘导航
      const handleProductSelectKeydown = (event, row, rowIndex) => {
        if (handleEmptyProductInputEnter(event, row, () => emit('addProduct'))) {
          return;
        }

        handleInlineProductSelectKeydown(
          event,
          row,
          rowIndex,
          (idx, product) => emit('select', idx, product),
          () => nextTick(),
        );
      };

      // 行高亮CSS类
      const getProductSelectRowClass = (row, product) => {
        return getInlineProductSelectRowClass(row, product);
      };

      // 启用编辑模式（row为对象引用，变异是设计行为）
      /* eslint-disable vue/no-mutating-props */
      const handleEnableProductEdit = () => {
        if (props.mode === 'require' && props.isFixed) {
          return;
        }
        props.row.editingProduct = true;
        props.row.productQuery = '';
        props.row.products = [];
        props.row.productOptions = [];
        resetInlineProductSelect(props.row);
        nextTick(() => {
          autoCompleteRef.value?.focus();
        });
      };
      /* eslint-enable vue/no-mutating-props */

      // 暴露focus方法给父组件
      const focus = () => {
        nextTick(() => {
          autoCompleteRef.value?.focus();
        });
      };
      expose({ focus });

      return {
        autoCompleteRef,
        showAutoComplete,
        clickableStyle,
        queryProduct,
        handleProductInputFocus,
        handleProductSelectKeydown,
        getProductSelectRowClass,
        handleEnableProductEdit,
        focus,
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
