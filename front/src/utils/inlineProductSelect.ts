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

function scrollActiveInlineProductIntoView(nextTick: () => Promise<void>) {
  nextTick().then(() => {
    document.querySelector(`.${ACTIVE_ROW_CLASS}`)?.scrollIntoView({
      block: 'nearest',
      inline: 'nearest',
    });
  });
}
