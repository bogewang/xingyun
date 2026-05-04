import * as api from '@/api/base-data/print-template';
import { createError } from '@/hooks/web/msg';

export const printMix = {
  methods: {
    async vgPrintPreview(type, printData, options = {}) {
      const bizType = String(type);
      const result = await queryTemplateByBizType(bizType);

      const templateList = result?.datas || [];
      if (!templateList.length) {
        createError('未找到当前业务类型的打印模板！');
        return;
      }

      const templateId = templateList[0].id;
      const setting = await api.getSetting(templateId);
      const templateJson = setting?.templateJson;

      if (!templateJson) {
        createError('未找到打印模板配置！');
        return;
      }

      // @ts-ignore 运行时由外部注入 $printRuntimeApi
      const preview = (this as any).$printRuntimeApi?.preview;
      if (typeof preview !== 'function') {
        createError('打印预览组件未正确初始化！');
        return;
      }

      return preview(templateJson, printData, {
        ...options,
        bizType,
        templateId,
        enableTemplateSwitch: true,
        templateList: templateList.map((item) => ({
          id: item.id,
          name: item.name,
          bizType: item.bizType,
        })),
      });

      async function queryTemplateByBizType(bizType: string) {
        return await api.query({
          pageIndex: 1,
          pageSize: 200,
          sortField: '',
          sortOrder: '',
          name: '',
          bizType: bizType,
        });
      }
    },
  },
};
