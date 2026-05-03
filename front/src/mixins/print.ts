import * as api from '@/api/base-data/print-template';
import { createError } from '@/hooks/web/msg';

export const printMix = {
  methods: {
    async vgPrintPreview(type, printData, options = {}) {
      // 获取打印模板配置,这里假设 type 是一个字符串，代表不同的业务类型
      // todo 一种类型可能对应多个模板，这里先简单处理成一对一的关系
      const setting = await api.getSetting(String(type));
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

      return preview(templateJson, printData, options);
    },

    lodopPreview(type, printData, options = {}) {
      return this.vgPrintPreview(type, printData, options);
    },
  },
};
