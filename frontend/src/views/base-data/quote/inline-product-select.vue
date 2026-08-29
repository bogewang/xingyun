<template>
  <!-- eslint-disable vue/no-mutating-props row prop 由父组件传入，变异是设计行为 -->
  <div class="inline-product-select">
    <a-auto-complete
      v-if="showAutoComplete"
      ref="autoCompleteRef"
      v-model:value="row.productQuery"
      placeholder="请输入商品编号/名称/SKU编号"
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
            <!-- 商品编号 -->
            <vxe-column field="code" title="商品编号" width="140" />
            <!-- 商品名称 -->
            <vxe-column field="name" title="商品名称" min-width="200" />
            <!-- SKU编号 -->
            <vxe-column field="skuCode" title="SKU编号" width="120" />
            <!-- 规格 -->
            <vxe-column field="spec" title="规格" width="120" />
            <!-- 单位 -->
            <vxe-column field="unit" title="单位" width="70" />
          </vxe-table>
        </div>
      </template>
    </a-auto-complete>
    <!-- 已选商品展示 -->
    <span v-else class="quote-product-name" @click="handleEnableProductEdit">{{ row.name }}</span>
  </div>
</template>

<script>
  import { computed, defineComponent, nextTick, ref } from 'vue';
  import * as productApi from '@/api/base-data/product/info';
  import { isEmpty } from '@/utils/utils';
  import {
    getInlineProductSelectRowClass,
    handleEmptyProductInputEnter,
    handleInlineProductSelectKeydown,
    resetInlineProductSelect,
    setInlineProductSelectProducts,
  } from '@/utils/inlineProductSelect';

  export default defineComponent({
    name: 'QuoteInlineProductSelect',
    props: {
      /** 行数据 */
      row: { type: Object, required: true },
      /** 行索引 */
      rowIndex: { type: Number, required: true },
      /** 下拉宽度 */
      dropdownWidth: { type: String, default: '900px' },
    },
    emits: ['select', 'addProduct'],
    setup(props, { emit, expose }) {
      const autoCompleteRef = ref(null);

      // 是否显示auto-complete输入框
      const showAutoComplete = computed(() => {
        return isEmpty(props.row.productId) || props.row.editingProduct;
      });

      // 搜索商品
      const queryProduct = (queryString, row) => {
        if (isEmpty(queryString)) {
          row.products = [];
          row.productOptions = [];
          resetInlineProductSelect(row);
          return;
        }

        productApi
          .selector({
            pageIndex: 1,
            pageSize: 100,
            code: queryString,
            name: queryString,
            skuCode: '',
            shortName: '',
            brandId: '',
            categoryId: '',
            startTime: '',
            endTime: '',
            productType: undefined,
            available: true,
          })
          .then((data) => {
            const res = data.datas || [];
            setInlineProductSelectProducts(row, res);
            row.productOptions = res.map((item) => ({
              value: item.id,
              label: item.code + ' ' + item.name,
            }));
          });
      };

      // 输入框聚焦时触发搜索
      const handleProductInputFocus = () => {
        const keyword = props.row.productQuery || props.row.name;
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
        queryProduct,
        handleProductInputFocus,
        handleProductSelectKeydown,
        getProductSelectRowClass,
        handleEnableProductEdit,
        focus,
        isEmpty,
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

  /* 已选商品名称的可点击样式 */
  .quote-product-name {
    color: #1677ff;
    cursor: pointer;
  }
</style>
