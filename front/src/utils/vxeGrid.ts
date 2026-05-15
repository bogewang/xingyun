interface FocusVxeGridRowOptions {
  grid: any;
  row: Recordable | undefined;
  rowIndex: number;
  nextTick: () => Promise<void>;
  focus?: () => void;
  rowHeight?: number;
}

function getRowElement(grid: any, row: Recordable | undefined): HTMLElement | null {
  if (!grid || !row || !row.id) {
    return null;
  }

  const bodyElem = grid.getRefMaps?.()?.refTableBody?.value?.$el as HTMLElement | undefined;
  if (!bodyElem) {
    return null;
  }

  return bodyElem.querySelector(`[rowid="${row.id}"]`);
}

export async function focusVxeGridRow(options: FocusVxeGridRowOptions) {
  const { grid, row, rowIndex, nextTick, focus, rowHeight = 48 } = options;

  await nextTick();
  if (grid?.recalculate) {
    await grid.recalculate();
  }
  if (grid?.refreshScroll) {
    await grid.refreshScroll();
  }
  if (row && grid?.scrollToRow) {
    await grid.scrollToRow(row);
  }

  await nextTick();
  let rowElem = getRowElement(grid, row);

  if (!rowElem && grid?.scrollTo) {
    await grid.scrollTo(null, rowIndex * rowHeight);
    await nextTick();
    rowElem = getRowElement(grid, row);
  }

  if (rowElem) {
    rowElem.scrollIntoView({ block: 'center', inline: 'nearest' });
  }

  await nextTick();
  if (focus) {
    focus();
  }
}
