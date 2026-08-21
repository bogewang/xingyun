import type { App } from 'vue';
import { h } from 'vue';
import Antd, { Empty } from 'ant-design-vue';
import VXETable from 'vxe-table';
import VxeUI from 'vxe-pc-ui';
import VxeUIPluginRenderAntd from '@vxe-ui/plugin-render-antd';
import JForm from '@/components/JForm';
import JFormItem from '@/components/JFormItem';
import DialogTable from '@/components/DialogTable';
import DialogTree from '@/components/DialogTree';
import JBorder from '@/components/JBorder';
import { Icon } from '@/components/Icon';
import { PageWrapper } from '/@/components/Page';
import { TableAction } from '/@/components/Table';
import componentSetting from '/@/settings/componentSetting';
import { defHttp } from '@/utils/http/axios';
import { createConfirm } from '@/hooks/web/msg';
import PrintDesigner, { printRuntimeApi } from '@/components/PrintDesigner';
import bpmApproveInstall from '@/components/BpmApprove';

export async function registerGlobComp(app: App) {
  app
    .use(Antd)
    .use(VxeUI)
    .use(VXETable)
    .component('JForm', JForm)
    .component('JFormItem', JFormItem)
    .component('JBorder', JBorder)
    .component('DialogTable', DialogTable)
    .component('DialogTree', DialogTree)
    .component('Icon', Icon)
    .component('PageWrapper', PageWrapper)
    .component('TableAction', TableAction)
    .component('PrintDesigner', PrintDesigner)
    .use(bpmApproveInstall);

  VxeUI.use(VxeUIPluginRenderAntd);
  VXETable.setup(componentSetting.vxeTable);

  /** 刷新所有 VXE 表格分页栏中的当前页勾选条数。 */
  const refreshVxePagerSelectionCount = () => {
    document.querySelectorAll<HTMLElement>('.vxe-grid').forEach((gridElement) => {
      const pagerElement = gridElement.querySelector<HTMLElement>('.vxe-pager');
      if (!pagerElement) {
        return;
      }

      const checkboxCells = gridElement.querySelectorAll(
        '.vxe-table--body-wrapper .vxe-cell--checkbox',
      );
      const selectionCountElement = pagerElement.querySelector<HTMLElement>(
        '.vxe-pager--selection-count',
      );
      if (!checkboxCells.length) {
        selectionCountElement?.remove();
        return;
      }

      const selectedCount = gridElement.querySelectorAll(
        '.vxe-table--body-wrapper .vxe-cell--checkbox.is--checked',
      ).length;
      const countElement = selectionCountElement || document.createElement('span');
      countElement.className = 'vxe-pager--selection-count';
      countElement.textContent = `已勾选 ${selectedCount} 条`;
      if (!selectionCountElement) {
        pagerElement.prepend(countElement);
      }
    });
  };

  document.addEventListener(
    'click',
    () => {
      window.setTimeout(refreshVxePagerSelectionCount);
    },
    true,
  );
  window.setTimeout(refreshVxePagerSelectionCount, 500);
  window.setTimeout(refreshVxePagerSelectionCount, 1500);
  VXETable.renderer.add('NotData', {
    renderEmpty(renderOpts) {
      const { attrs, props } = renderOpts;
      return [
        h(Empty, {
          ...attrs,
          ...props,
        }),
      ];
    },
  });

  app.config.globalProperties.$defHttp = defHttp;
  app.config.globalProperties.$confirm = (message: string, title = '提示信息') => {
    return createConfirm(message, title)
      .then(() => true)
      .catch(() => false);
  };
  app.config.globalProperties.$vh =
    (document.documentElement.clientHeight || document.body.clientHeight) / 100;
  app.config.globalProperties.$printRuntimeApi = printRuntimeApi;
}
