<template>
  <!-- 使用轻量包装层承接业务旧入口，并将实际设计能力交给 vg-print 的 FullDesigner。 -->
  <div class="print-designer-shell">
    <!-- 传入标准化后的模板与示例数据，并在保存时把 vg-print 的事件结果映射回业务层。 -->
    <FullDesigner
      ref="designerRef"
      :initial-template="normalizedTemplate"
      :initial-print-data="normalizedPrintData"
      default-lang="cn"
      @save="handleSave"
    />
  </div>
</template>

<script>
  // 导入 Vue 核心 API，用于定义组件、计算属性与实例暴露。
  import { computed, defineComponent, ref } from 'vue';
  // 导入 vg-print 的核心设计器组件，作为新的打印设计器实现。
  import { FullDesigner,hiprint } from 'vg-print';
  // 填入生成的 Key
  hiprint.register({ authKey: 'eyJrIjoiZ21jNTc2MDMzNyJ9' });
  /**
   * 构建新的空模板结构。
   *
   * 功能:
   * 返回符合 vg-print 设计器预期的基础模板对象。
   *
   * 参数:
   * 无。
   *
   * 返回值:
   * {Object} 返回包含空 `panels` 数组的模板对象。
   *
   * 异常:
   * 无显式抛出异常。
   */
  function createEmptyTemplate() {
    // 返回最小可用模板结构，避免设计器收到空值后初始化失败。
    return { panels: [] };
  }

  /**
   * 标准化模板数据。
   *
   * 功能:
   * 仅接受 vg-print 可识别的新模板结构；如果不是新结构，则回退为空模板。
   *
   * 参数:
   * @param {Object} templateValue - 业务层传入的模板对象。
   *
   * 返回值:
   * {Object} 返回可直接传给 FullDesigner 的模板对象。
   *
   * 异常:
   * 无显式抛出异常。
   */
  function normalizeTemplate(templateValue) {
    /*
     * 整体思路:
     * 1. 新组件只接受 vg-print 的模板结构。
     * 2. 用户已明确说明不需要兼容旧组件，因此不再尝试把旧 tempItems 结构转成新结构。
     * 3. 当模板为空或结构不符合要求时，统一回退到空模板，保证设计器可以稳定打开。
     */
    // 判断传入值是否为对象，避免后续读取属性时报错。
    const isObjectValue = templateValue && typeof templateValue === 'object';
    // 判断模板是否已经包含 vg-print 所需的 panels 数组。
    const hasPanels = isObjectValue && Array.isArray(templateValue.panels);
    // 如果已经是新模板结构，则直接返回原对象，否则回退为空模板。
    return hasPanels ? templateValue : createEmptyTemplate();
  }

  /**
   * 标准化打印示例数据。
   *
   * 功能:
   * 保证传给 FullDesigner 的打印数据始终为数组，便于预览和导出使用。
   *
   * 参数:
   * @param {Object|Array} demoData - 业务层传入的示例打印数据。
   *
   * 返回值:
   * {Array} 返回标准化后的数组数据。
   *
   * 异常:
   * 无显式抛出异常。
   */
  function normalizePrintData(demoData) {
    // 如果本身已经是数组，则直接复用数组数据。
    if (Array.isArray(demoData)) {
      // 返回数组形式的数据，保持与 vg-print API 一致。
      return demoData;
    }

    // 如果没有示例数据，则返回空数组。
    if (!demoData || typeof demoData !== 'object') {
      // 返回空数组，避免预览阶段拿到无效数据。
      return [];
    }

    // 将单对象包装为数组，满足 FullDesigner 的入参要求。
    return [demoData];
  }

  export default defineComponent({
    // 定义组件名，供 Vue DevTools 与全局注册识别。
    name: 'PrintDesigner',
    // 注册新设计器组件，模板中将直接使用它。
    components: { FullDesigner },
    props: {
      // 接收后端存储的模板对象，并在内部转换为新结构。
      tempValue: {
        // 声明模板参数为对象类型。
        type: Object,
        // 提供空模板默认值，确保首次打开也可正常渲染。
        default: () => createEmptyTemplate(),
      },
      // 接收设计器预览时使用的示例数据。
      demoData: {
        // 声明示例数据允许以对象形式传入。
        type: [Array, Object],
        // 默认返回空对象，交给标准化逻辑统一处理。
        default: () => ({}),
      },
    },
    emits: [
      // 对外保留 save 事件，供业务页面继续提交模板数据。
      'save',
    ],
    setup(props, { emit, expose }) {
      
      // 保存 FullDesigner 实例引用，便于对外暴露保存和预览方法。
      const designerRef = ref(null);
      // 基于传入模板实时生成新设计器可识别的数据结构。
      const normalizedTemplate = computed(() => normalizeTemplate(props.tempValue));
      // 基于传入示例数据实时生成数组形式的打印数据。
      const normalizedPrintData = computed(() => normalizePrintData(props.demoData));

      /**
       * 处理设计器保存事件。
       *
       * 功能:
       * 将 vg-print 的保存事件负载转换为业务层仅关心的模板对象，并向父组件抛出。
       *
       * 参数:
       * @param {Object} payload - vg-print 返回的保存事件对象。
       *
       * 返回值:
       * {void} 无返回值。
       *
       * 异常:
       * 无显式抛出异常。
       */
      function handleSave(payload) {
        // 优先读取新设计器返回的 template 字段，缺失时回退为空模板。
        const template = payload && payload.template ? payload.template : createEmptyTemplate();
        // 将模板对象继续通过旧事件名抛出，保持业务提交入口稳定。
        emit('save', template);
      }

      /**
       * 触发设计器保存。
       *
       * 功能:
       * 调用 FullDesigner 暴露的 `save` 方法，主动触发保存事件。
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
      function saveTemp() {
        // 调用设计器实例的保存方法，让内部统一产出模板数据。
        designerRef.value?.save();
      }

      /**
       * 触发设计器预览。
       *
       * 功能:
       * 调用 FullDesigner 暴露的 `preView` 方法，打开内置预览窗口。
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
      function previewTemp() {
        // 调用设计器实例的预览方法，使用当前模板和示例数据进行渲染。
        designerRef.value?.preView();
      }

      /**
       * 获取底层设计器实例。
       *
       * 功能:
       * 对外暴露 FullDesigner 实例，便于未来业务需要时直接调用底层方法。
       *
       * 参数:
       * 无。
       *
       * 返回值:
       * {Object|null} 返回设计器实例或空值。
       *
       * 异常:
       * 无显式抛出异常。
       */
      function getDesigner() {
        // 返回当前保存的组件实例引用，供外层按需扩展。
        return designerRef.value;
      }

      // 暴露包装层方法，便于保留业务侧 ref 的可用性。
      expose({ getDesigner, previewTemp, saveTemp });

      // 返回模板绑定与事件处理器，供模板直接使用。
      return {
        designerRef,
        handleSave,
        normalizedPrintData,
        normalizedTemplate,
      };
    },
  });
</script>

<style lang="scss">
  .print-designer-shell {
    width: 100%;
    height: 100%;
  }

  .print-designer-shell :deep(.designer-page) {
    height: 100%;
  }
</style>
