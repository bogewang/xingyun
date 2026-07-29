// @vitest-environment jsdom

import { flushPromises, mount } from '@vue/test-utils';
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';

import PrintDialog from './PrintDialog.vue';
import { closePrintDialog, openPrintDialog } from './printDialog';

const { createError, getSetting } = vi.hoisted(() => ({
  createError: vi.fn(),
  getSetting: vi.fn(),
}));

vi.mock('@/api/base-data/print-template', () => ({
  getSetting,
}));

vi.mock('@/hooks/web/msg', () => ({
  createError,
}));

const templateData = { data: { pages: [] } };
const printData = { orderNo: 'SO-001' };

class TestPrintDesignerElement extends HTMLElement {
  loadTemplateData = vi.fn();
  getTemplateData = vi.fn(() => templateData);
  setTestData = vi.fn();
  getTestData = vi.fn(() => ({}));
  setVariables = vi.fn();
  print = vi.fn(() => Promise.resolve());
}

beforeAll(() => {
  if (!customElements.get('print-designer')) {
    customElements.define('print-designer', TestPrintDesignerElement);
  }
});

beforeEach(() => {
  closePrintDialog();
  createError.mockReset();
  getSetting.mockReset();
});

function mountDialog() {
  return mount(PrintDialog, {
    global: {
      stubs: {
        'a-button': { template: '<button><slot /></button>' },
        'a-alert': { template: '<div><slot /></div>' },
        'a-modal': { template: '<div><slot /></div>' },
        'a-select': { template: '<select><slot /></select>' },
        'a-select-option': { template: '<option><slot /></option>' },
      },
    },
  });
}

describe('PrintDialog', () => {
  it('有效模板加载变量，并只在点击打印后通过浏览器通道打印', async () => {
    const wrapper = mountDialog();
    openPrintDialog({ templateJson: templateData, printData });

    await flushPromises();
    await wrapper.vm.$nextTick();

    const designer = wrapper.find('print-designer').element as TestPrintDesignerElement;
    expect(designer.loadTemplateData).toHaveBeenCalledWith(templateData);
    expect(designer.setVariables).toHaveBeenCalledWith(printData);
    expect(designer.print).not.toHaveBeenCalled();

    await (wrapper.vm as unknown as { handlePrint: () => Promise<void> }).handlePrint();

    expect(designer.print).toHaveBeenCalledWith({ mode: 'browser' });
  });

  it('阻止加载旧版 panels 模板并提示先迁移', async () => {
    const wrapper = mountDialog();
    openPrintDialog({ templateJson: { panels: [] }, printData });

    await flushPromises();
    await wrapper.vm.$nextTick();

    expect(wrapper.find('print-designer').exists()).toBe(false);
    expect(createError).toHaveBeenCalledWith(expect.stringContaining('请先迁移模板'));
  });

  it('切换到旧模板失败时保留当前有效模板和下拉选择', async () => {
    const wrapper = mountDialog();
    openPrintDialog({
      templateJson: templateData,
      printData,
      templateId: 'current',
      templateList: [
        { id: 'current', name: '当前模板' },
        { id: 'legacy', name: '旧模板' },
      ],
      enableTemplateSwitch: true,
    });
    await flushPromises();
    await wrapper.vm.$nextTick();

    const designer = wrapper.find('print-designer').element as TestPrintDesignerElement;
    getSetting.mockResolvedValue({ templateJson: { panels: [] } });

    await (
      wrapper.vm as unknown as { handleTemplateChange: (id: string) => Promise<void> }
    ).handleTemplateChange('legacy');

    expect((wrapper.vm as unknown as { selectedTemplateId: string }).selectedTemplateId).toBe(
      'current',
    );
    expect(designer.loadTemplateData).toHaveBeenCalledTimes(1);
    expect(createError).toHaveBeenCalledWith(expect.stringContaining('请先迁移模板'));
  });

  it('切换到加载异常的模板时保留当前有效预览', async () => {
    const wrapper = mountDialog();
    openPrintDialog({
      templateJson: templateData,
      printData,
      templateId: 'current',
      templateList: [
        { id: 'current', name: '当前模板' },
        { id: 'failed', name: '异常模板' },
      ],
      enableTemplateSwitch: true,
    });
    await flushPromises();
    await wrapper.vm.$nextTick();

    const designer = wrapper.find('print-designer').element as TestPrintDesignerElement;
    designer.loadTemplateData.mockImplementationOnce(() => {
      throw new Error('load failed');
    });
    getSetting.mockResolvedValue({ templateJson: { data: { pages: [{ id: 'new' }] } } });

    await (
      wrapper.vm as unknown as { handleTemplateChange: (id: string) => Promise<void> }
    ).handleTemplateChange('failed');

    expect(wrapper.find('print-designer').exists()).toBe(true);
    expect((wrapper.vm as unknown as { selectedTemplateId: string }).selectedTemplateId).toBe(
      'current',
    );
    expect(createError).toHaveBeenCalledWith('加载打印模板失败！');
  });
});
