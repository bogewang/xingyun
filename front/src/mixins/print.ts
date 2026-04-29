import * as api from '@/api/base-data/print-template';
import { createError } from '@/hooks/web/msg';

export const printMix = {
  methods: {
    async vgPrintPreview(type, printData, options = {}) {
      const setting = await api.getSetting(String(type));
      const templateJson = setting?.templateJson;

      if (!templateJson) {
        createError('未找到打印模板配置！');
        return;
      }

      const preview = this.$printRuntimeApi?.preview;
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
