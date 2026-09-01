import { ref } from 'vue';
import { describe, expect, it } from 'vitest';
import { useInlineProductSelect } from '../inlineProductSelect';

describe('useInlineProductSelect', () => {
  it('表格复用选品组件并切换到插入的空行后，应显示商品输入框', () => {
    const currentRow = ref({ productId: 'selected-product', editingProduct: false });
    const { showAutoComplete } = useInlineProductSelect(
      currentRow,
      () => Promise.resolve([]),
      () => undefined,
    );

    expect(showAutoComplete.value).toBe(false);

    currentRow.value = { productId: '', editingProduct: false };

    expect(showAutoComplete.value).toBe(true);
  });
});
