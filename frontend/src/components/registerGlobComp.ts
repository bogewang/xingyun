import type { App } from 'vue';
import 'vue-print-designer';
import 'vue-print-designer/style.css';
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
