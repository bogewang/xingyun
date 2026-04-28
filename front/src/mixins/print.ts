import * as api from '@/api/base-data/print-template';

export const printMix = {
  methods: {
    /**
     * 打开模板打印预览。
     *
     * 功能:
     * 根据模板类型读取后端存储的模板配置，并使用新的打印运行时打开浏览器预览。
     *
     * 参数:
     * @param {string} type - 打印模板类型或模板主键。
     * @param {unknown} printData - 当前打印任务使用的数据对象。
     *
     * 返回值:
     * {void} 无返回值。
     *
     * 异常:
     * 无显式抛出异常；接口或预览异常由调用链自行处理。
     */
    lodopPreview(type, printData) {
      // 查询指定模板的最新设置数据。
      api.getSetting(type).then((res) => {
        // 读取后端保存的新模板 JSON。
        const templateJson = res.templateJson;
        // 使用新的运行时预览能力渲染模板与打印数据。
        this.$lodop.preview(templateJson, [printData]);
      });
    },
  },
};
