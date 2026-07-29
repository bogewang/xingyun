// @vitest-environment jsdom

import { mount } from '@vue/test-utils';
import { beforeAll, describe, expect, it, vi } from 'vitest';

import PrintDesigner from './PrintDesigner.vue';

vi.mock('@/api/base-data/print-template', () => ({
  getFieldDesc: vi.fn(),
}));

const templateData = { data: { pages: [] } };
const testData = { orderNo: 'SO-001' };

class TestPrintDesignerElement extends HTMLElement {
  loadTemplateData = vi.fn();
  getTemplateData = vi.fn(() => templateData);
  setTestData = vi.fn();
  getTestData = vi.fn(() => testData);
  setVariables = vi.fn();
  print = vi.fn();
}

beforeAll(() => {
  if (!customElements.get('print-designer')) {
    customElements.define('print-designer', TestPrintDesignerElement);
  }
});

describe('PrintDesigner', () => {
  it('从 PrintDot 元素读取模板和测试数据并发出既有保存契约', async () => {
    const wrapper = mount(PrintDesigner, {
      props: {
        tempValue: templateData,
        demoData: testData,
      },
      global: {
        stubs: {
          'a-alert': { template: '<div><slot /></div>' },
          'a-button': { template: '<button><slot /></button>' },
          'a-modal': { template: '<div><slot /></div>' },
          'a-table': { template: '<div><slot /></div>' },
        },
      },
    });

    await wrapper.vm.$nextTick();

    expect(wrapper.find('print-designer').exists()).toBe(true);

    (wrapper.vm as unknown as { saveTemp: () => void }).saveTemp();

    expect(wrapper.emitted('save')).toEqual([[templateData, testData]]);
  });
});
