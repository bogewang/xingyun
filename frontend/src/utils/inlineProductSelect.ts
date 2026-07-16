import { isEmpty } from '@/utils/utils';

const ACTIVE_ROW_CLASS = 'inline-product-select-active-row';

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
