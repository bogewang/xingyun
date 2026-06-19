interface FocusVxeGridRowOptions {
  grid: any;
  row: Recordable | undefined;
  rowIndex: number;
  nextTick: () => Promise<void>;
  focus?: () => void;
  rowHeight?: number;
}

interface MoveTableInputOptions {
  vm: {
    $refs: Record<string, any>;
    $nextTick: () => Promise<void>;
  };
  rowIndex: number;
  refName: string;
  direction: 'left' | 'right' | 'up' | 'down';
  refOrder: string[];
  appendRow?: () => void;
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

export async function focusTableInput(
  vm: {
    $refs: Record<string, any>;
    $nextTick: () => Promise<void>;
  },
  refName: string,
  index: number,
) {
  await vm.$nextTick();
  const inputRef = vm.$refs[refName + index];
  const target = Array.isArray(inputRef) ? inputRef[0] : inputRef;
  if (target?.focus) {
    target.focus();
    return true;
  }
  return false;
}

export async function moveTableInput(options: MoveTableInputOptions) {
  const { vm, rowIndex, refName, direction, refOrder, appendRow } = options;
  const colIndex = refOrder.indexOf(refName);
  if (colIndex < 0) {
    return;
  }

  if (direction === 'left' || direction === 'right') {
    const step = direction === 'left' ? -1 : 1;
    for (let i = colIndex + step; i >= 0 && i < refOrder.length; i += step) {
      if (await focusTableInput(vm, refOrder[i], rowIndex)) {
        return;
      }
    }
    return;
  }

  const step = direction === 'up' ? -1 : 1;
  let targetRow = rowIndex + step;
  let appended = false;

  while (targetRow >= 0) {
    if (direction === 'down' && appendRow && !appended) {
      const focused = await focusTableInput(vm, refName, targetRow);
      if (focused) {
        return;
      }
      appendRow();
      appended = true;
      continue;
    }

    if (await focusTableInput(vm, refName, targetRow)) {
      return;
    }

    if (direction === 'up' && targetRow === 0) {
      return;
    }

    targetRow += step;
  }
}
