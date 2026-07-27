import { describe, expect, it } from 'vitest';
import { getActiveMenuPath } from './menuHelper';

describe('菜单激活路径', () => {
  it('只返回当前叶子菜单，父级菜单仅用于展开', () => {
    const menus = [
      {
        path: '/settle',
        children: [{ path: '/settle/supplier/sheet' }, { path: '/settle/customer/sheet' }],
      },
    ];

    expect(getActiveMenuPath(menus, '/settle/supplier/sheet')).toBe('/settle/supplier/sheet');
  });
});
