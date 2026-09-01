import { computed, nextTick, ref, unref, type Ref } from 'vue';
import { isEmpty } from '@/utils/utils';

const ACTIVE_ROW_CLASS = 'inline-product-select-active-row';

type ProductRowSource = Recordable | Ref<Recordable>;

/**
 * 表格内联商品选择的公共逻辑，供各业务的内联选品下拉组件复用。
 *
 * @param row 行数据（响应式对象引用，允许变异）
 * @param searchProducts 商品搜索接口，返回商品列表
 * @param emits 事件发射函数
 */
export function useInlineProductSelect(
  rowSource: ProductRowSource,
  searchProducts: (keyword: string) => Promise<Recordable[]>,
  emits: (event: 'select' | 'addProduct', ...args: unknown[]) => void,
) {
  const autoCompleteRef = ref(null);
  const getRow = () => unref(rowSource);

  // 是否显示auto-complete输入框
  const showAutoComplete = computed(() => {
    const row = getRow();
    return isEmpty(row.productId) || row.editingProduct;
  });

  // 搜索商品
  const queryProduct = (queryString: string, currentRow: Recordable) => {
    if (isEmpty(queryString)) {
      currentRow.products = [];
      currentRow.productOptions = [];
      resetInlineProductSelect(currentRow);
      return;
    }

    searchProducts(queryString).then((res) => {
      setInlineProductSelectProducts(currentRow, res);
      currentRow.productOptions = res.map((item) => ({
        value: item.productId || item.id,
        label: (item.productCode || item.code || '') + ' ' + (item.productName || item.name || ''),
      }));
    });
  };

  // 输入框聚焦时触发搜索，fallbackName为已选商品名称等兜底关键字
  const handleProductInputFocus = (fallbackName?: string) => {
    const row = getRow();
    const keyword = row.productQuery || fallbackName;
    if (isEmpty(keyword)) return;

    queryProduct(keyword, row);
  };

  // 键盘导航
  const handleProductSelectKeydown = (
    event: KeyboardEvent,
    currentRow: Recordable,
    currentRowIndex: number,
  ) => {
    if (handleEmptyProductInputEnter(event, currentRow, () => emits('addProduct'))) {
      return;
    }

    handleInlineProductSelectKeydown(
      event,
      currentRow,
      currentRowIndex,
      (idx, product) => emits('select', idx, product),
      () => nextTick(),
    );
  };

  // 行高亮CSS类
  const getProductSelectRowClass = (currentRow: Recordable, product: Recordable) => {
    return getInlineProductSelectRowClass(currentRow, product);
  };

  // 启用编辑模式（row为对象引用，变异是设计行为）
  const handleEnableProductEdit = (enabled = true) => {
    if (!enabled) return;

    const row = getRow();
    row.editingProduct = true;
    row.productQuery = '';
    row.products = [];
    row.productOptions = [];
    resetInlineProductSelect(row);
    nextTick(() => {
      autoCompleteRef.value?.focus();
    });
  };

  // 聚焦输入框
  const focus = () => {
    nextTick(() => {
      autoCompleteRef.value?.focus();
    });
  };

  return {
    autoCompleteRef,
    showAutoComplete,
    queryProduct,
    handleProductInputFocus,
    handleProductSelectKeydown,
    getProductSelectRowClass,
    handleEnableProductEdit,
    focus,
  };
}

export function resetInlineProductSelect(row: Recordable) {
  row.activeProductIndex = -1;
}

export function setInlineProductSelectProducts(row: Recordable, products: Recordable[] = []) {
  row.products = products;
  row.activeProductIndex = isEmpty(products) ? -1 : 0;
}

export function getInlineProductSelectRowClass(row: Recordable, product: Recordable) {
  if (!row || !product || isEmpty(row.products) || row.activeProductIndex < 0) {
    return '';
  }

  return row.products[row.activeProductIndex] === product ? ACTIVE_ROW_CLASS : '';
}

export function handleInlineProductSelectKeydown(
  event: KeyboardEvent,
  row: Recordable,
  rowIndex: number,
  selectProduct: (rowIndex: number, product: Recordable) => void,
  nextTick: () => Promise<void>,
) {
  if (!row || isEmpty(row.products)) {
    return;
  }

  const key = event.key;
  if (!['ArrowDown', 'ArrowUp', 'Enter'].includes(key)) {
    return;
  }

  event.preventDefault();
  event.stopPropagation();

  if (key === 'Enter') {
    const product = row.products[row.activeProductIndex >= 0 ? row.activeProductIndex : 0];
    if (product) {
      selectProduct(rowIndex, product);
    }
    return;
  }

  const offset = key === 'ArrowDown' ? 1 : -1;
  const maxIndex = row.products.length - 1;
  const currentIndex = row.activeProductIndex >= 0 ? row.activeProductIndex : 0;
  row.activeProductIndex = Math.min(maxIndex, Math.max(0, currentIndex + offset));
  scrollActiveInlineProductIntoView(nextTick);
}

/**
 * 商品输入框处于待输入状态时，处理回车新增商品行。
 *
 * @return 是否已处理该回车事件
 */
export function handleEmptyProductInputEnter(
  event: KeyboardEvent,
  row: Recordable,
  addProduct: () => void,
) {
  if (
    event.key !== 'Enter' ||
    event.isComposing ||
    event.repeat ||
    !isEmpty(row.productId) ||
    !isEmpty(row.productQuery)
  ) {
    return false;
  }

  event.preventDefault();
  event.stopPropagation();
  addProduct();
  return true;
}

function scrollActiveInlineProductIntoView(nextTick: () => Promise<void>) {
  nextTick().then(() => {
    const activeRow = getVisibleActiveRow();
    if (!activeRow) {
      return;
    }

    const table = activeRow.closest('.vxe-table');
    const bodyWrapper = table?.querySelector('.vxe-table--body-wrapper.body--wrapper') as
      | HTMLElement
      | null
      | undefined;

    if (!bodyWrapper) {
      activeRow.scrollIntoView({ block: 'nearest', inline: 'nearest' });
      return;
    }

    const rowRect = activeRow.getBoundingClientRect();
    const wrapperRect = bodyWrapper.getBoundingClientRect();

    if (rowRect.bottom > wrapperRect.bottom) {
      bodyWrapper.scrollTop += rowRect.bottom - wrapperRect.bottom;
    } else if (rowRect.top < wrapperRect.top) {
      bodyWrapper.scrollTop -= wrapperRect.top - rowRect.top;
    }
  });
}

function getVisibleActiveRow() {
  const rows = Array.from(document.querySelectorAll(`.${ACTIVE_ROW_CLASS}`)) as HTMLElement[];
  return rows.find((row) => {
    const rect = row.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0;
  });
}
