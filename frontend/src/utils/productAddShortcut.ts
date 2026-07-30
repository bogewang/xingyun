const OVERLAY_SELECTOR = '.ant-modal, .ant-drawer, .vxe-modal--wrapper';
const FORM_CONTROL_SELECTOR =
  'input, textarea, button, a, [contenteditable="true"], .ant-select, .ant-picker';

/**
 * 判断回车是否应触发商品行新增。
 *
 * 表格中的普通行内输入框允许触发；下拉、日期、弹窗及页面表单控件不触发。
 */
export function shouldAddProductByEnter(event: KeyboardEvent) {
  if (event.key !== 'Enter' || event.defaultPrevented || event.isComposing || event.repeat) {
    return false;
  }

  const target = event.target;
  if (!(target instanceof HTMLElement)) {
    return true;
  }

  if (target.closest(OVERLAY_SELECTOR)) {
    return false;
  }

  if (
    target.closest('.sheet-editor-grid') &&
    target.matches('input:not([readonly]), textarea:not([readonly])') &&
    !target.closest('.ant-select, .ant-picker')
  ) {
    return true;
  }

  return !target.closest(FORM_CONTROL_SELECTOR);
}

/**
 * 阻止数量输入框的小键盘删除/小数点键冒泡到 VXE 表格。
 *
 * 小键盘小数点在不同 NumLock、浏览器组合下可能上报为 Delete、Decimal 或 NumpadDecimal，
 * 冒泡后会误触发表格删除行为。这里只停止冒泡，不阻止输入框录入小数点。
 */
export function stopGridDeleteFromInput(event: KeyboardEvent) {
  if (event.key === 'Delete' || event.key === 'Decimal' || event.code === 'NumpadDecimal') {
    event.stopPropagation();
  }
}
