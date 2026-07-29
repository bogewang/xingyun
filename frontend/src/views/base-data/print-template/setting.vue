<template>
  <a-modal
    v-model:open="visible"
    :mask-closable="false"
    width="100%"
    title="设置"
    :keyboard="false"
    :style="{ top: '5px' }"
    :footer="null"
  >
    <div
      v-if="visible && inited"
      v-permission="['base-data:print-template:query']"
      v-loading="loading"
    >
      <print-designer
        ref="designer"
        :temp-value="value"
        :demo-data="demoData"
        :biz-type="formData.bizType"
        @save="submit"
      />
    </div>
  </a-modal>
</template>

<script>
  // 导入 Vue 组件定义方法。
  import { defineComponent } from 'vue';
  // 导入打印模板相关接口。
  import * as api from '@/api/base-data/print-template';
  // 导入统一成功提示方法。
  import { createSuccess } from '@/hooks/web/msg';
  import { normalizeDemoData } from '@/components/PrintDesigner/src/printUtils';
  import PrintDesigner from '@/components/PrintDesigner';

  export default defineComponent({
    // 定义组件名称，便于调试与组件树识别。
    name: 'PrintTemplateSetting',
    components: { PrintDesigner },
    props: {
      // 接收当前模板主键。
      id: {
        type: String,
        required: true,
      },
    },
    /**
     * 构建页面响应式数据。
     *
     * 功能:
     * 初始化弹窗显示状态、表单数据、模板数据与示例数据。
     *
     * 参数:
     * 无。
     *
     * 返回值:
     * {Object} 返回页面使用的响应式数据对象。
     *
     * 异常:
     * 无显式抛出异常。
     */
    data() {
      // 返回页面需要的全部响应式状态。
      return {
        // 控制设置弹窗显示状态。
        visible: false,
        // 控制弹窗内部加载状态。
        loading: false,
        // 控制设计器何时挂载，避免拿到旧数据。
        inited: false,
        // 保存接口回填与提交时复用的表单数据。
        formData: {},
        // 预留规则字段，保持页面数据结构完整。
        rules: {
          name: [{ required: true, message: '请输入名称' }],
        },
        // 保存当前模板数据，供新设计器直接加载。
        value: null,
        // 保留组件配置列表字段，便于后续扩展接口返回值。
        widgets: [],
        // 保存设计器预览用的示例打印数据。
        demoData: {},
      };
    },
    /**
     * 组件创建完成后的初始化逻辑。
     *
     * 功能:
     * 首次创建组件时先重置页面表单数据。
     *
     * 参数:
     * 无。
     *
     * 返回值:
     * {void} 无返回值。
     *
     * 异常:
     * 无显式抛出异常。
     */
    created() {
      // 首次进入页面时先重置内部状态。
      this.initFormData();
    },
    methods: {
      /**
       * 打开设置弹窗。
       *
       * 功能:
       * 显示弹窗，并在弹窗内容挂载后加载模板配置数据。
       *
       * 参数:
       * 无。
       *
       * 返回值:
       * {void} 无返回值。
       *
       * 异常:
       * 无显式抛出异常。
       */
      openDialog() {
        // 打开当前设置弹窗。
        this.visible = true;

        // 等待弹窗节点挂载完成后再初始化设计器。
        this.$nextTick(() => this.open());
      },

      /**
       * 关闭设置弹窗。
       *
       * 功能:
       * 隐藏当前弹窗，并向父组件发送关闭事件。
       *
       * 参数:
       * 无。
       *
       * 返回值:
       * {void} 无返回值。
       *
       * 异常:
       * 无显式抛出异常。
       */
      closeDialog() {
        // 关闭弹窗显示状态。
        this.visible = false;
        // 通知父组件执行关闭后的后续逻辑。
        this.$emit('close');
      },

      /**
       * 初始化表单数据。
       *
       * 功能:
       * 清空上一次打开弹窗时残留的模板数据与挂载状态。
       *
       * 参数:
       * 无。
       *
       * 返回值:
       * {void} 无返回值。
       *
       * 异常:
       * 无显式抛出异常。
       */
      initFormData() {
        // 重置表单基础字段，为新一轮加载做准备。
        this.formData = {
          id: '',
          templateJson: '',
          bizType: '',
        };
        // 回退到空模板，避免显示上一条记录的数据。
        this.value = null;
        // 先卸载设计器，等待新数据准备完成后再重新渲染。
        this.inited = false;
      },

      /**
       * 提交模板设置。
       *
       * 功能:
       * 将新设计器输出的模板对象序列化后提交到后端保存。
       *
       * 参数:
       * @param {Object} templateJson - 新设计器保存事件返回的模板对象。
       *
       * 返回值:
       * {void} 无返回值。
       *
       * 异常:
       * 无显式抛出异常；接口异常由 Promise 链自行处理。
       * @param {Object|Array} data - 设计器保存事件返回的示例数据。
       */
      submit(templateJson, data) {
        // 开启加载状态，防止重复提交。
        this.loading = true;
        const demoData = normalizeDemoData(data);
        api
          .updateSetting({
            // 传入模板主键，定位需要更新的记录。
            id: this.formData.id,
            // 传入序列化后的模板 JSON 内容。
            templateJson: JSON.stringify(templateJson || {}),
            demoData: JSON.stringify(demoData),
          })
          .then(() => {
            // 提示用户模板保存成功。
            createSuccess('保存成功');
            // 通知父组件执行刷新等业务动作。
            this.$emit('confirm');
            // 保存成功后关闭弹窗。
            this.visible = false;
          })
          .finally(() => {
            // 无论请求结果如何，都需要结束加载状态。
            this.loading = false;
          });
      },

      /**
       * 初始化弹窗内容。
       *
       * 功能:
       * 重置页面状态后，再发起模板与组件配置数据加载。
       *
       * 参数:
       * 无。
       *
       * 返回值:
       * {void} 无返回值。
       *
       * 异常:
       * 无显式抛出异常。
       */
      open() {
        // 先重置内部状态，避免带入上一次打开的数据。
        this.initFormData();
        // 加载模板设置与示例数据。
        this.loadFormData();
      },

      /**
       * 加载模板配置数据。
       *
       * 功能:
       * 并行查询模板组件列表和模板设置详情，并将结果回填给新设计器。
       *
       * 参数:
       * 无。
       *
       * 返回值:
       * {void} 无返回值。
       *
       * 异常:
       * 无显式抛出异常；接口异常由 Promise 链自行处理。
       */
      loadFormData() {
        // 打开加载状态，提示用户正在准备模板数据。
        this.loading = true;
        Promise.all([api.getTemplateComp(this.id), api.getSetting(this.id)])
          .then(([templateComp, setting]) => {
            // 保存设置详情，供提交和回显复用。
            this.formData = setting;

            // 不生成旧版 panels 空模板；由 PrintDot 以空画布开始设计。
            this.value = this.formData.templateJson || null;

            // 保留模板组件列表结果，便于后续页面能力扩展。
            this.widgets = (templateComp || []).map((item) => item.compJson);
            // 回填示例数据，供新设计器预览使用。
            this.demoData = normalizeDemoData(this.formData.demoData);
          })
          .finally(() => {
            // 结束加载状态。
            this.loading = false;
            // 仅在数据准备完成后再挂载设计器。
            this.inited = true;
          });
      },
    },
  });
</script>
