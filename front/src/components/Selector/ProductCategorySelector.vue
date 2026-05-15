<template>
  <a-tree-select
    v-model:value="model"
    show-search
    style="width: 100%"
    :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
    :placeholder="placeholder"
    allow-clear
    tree-default-expand-all
    :tree-data="treeData"
    tree-node-filter-prop="label"
    :field-names="{ label: 'label', key: 'value', value: 'value', children: 'children' }"
    :disabled="disabled"
    v-bind="$attrs"
    @change="onChange"
  >
    <template #title="{ label }">
      {{ label }}
    </template>
  </a-tree-select>
</template>

<script>
  import { defineComponent } from 'vue';
  import * as api from '@/api/base-data/product/category';
  import { isEmpty } from '@/utils/utils';

  function normalizeTreeValue(value) {
    if (Array.isArray(value)) {
      return value.map((item) => normalizeTreeValue(item)).filter((item) => !isEmpty(item));
    }

    return isEmpty(value) ? undefined : value.toString();
  }

  function mapTreeNodes(list, onlyFinal) {
    return (list || []).map((item) => {
      const children = mapTreeNodes(item.children || [], onlyFinal);
      const disabled = !!onlyFinal && !isEmpty(children);
      return {
        label: item.name,
        value: normalizeTreeValue(item.id),
        children,
        disabled,
      };
    });
  }

  function buildCategoryTree(list) {
    const nodeMap = {};
    const roots = [];

    (list || []).forEach((item) => {
      nodeMap[item.id] = {
        ...item,
        children: [],
      };
    });

    Object.values(nodeMap).forEach((item) => {
      const parent = nodeMap[item.parentId];
      if (parent) {
        parent.children.push(item);
      } else {
        roots.push(item);
      }
    });

    return roots;
  }

  export default defineComponent({
    name: 'ProductCategorySelector',
    inheritAttrs: false,
    props: {
      value: {
        type: [String, Number, Array],
        default: undefined,
      },
      onlyFinal: {
        type: Boolean,
        default: false,
      },
      disabled: {
        type: Boolean,
        default: false,
      },
      placeholder: {
        type: String,
        default: '请选择商品分类',
      },
    },
    data() {
      return {
        treeData: [],
      };
    },
    computed: {
      model: {
        get() {
          return normalizeTreeValue(this.value);
        },
        set() {},
      },
    },
    created() {
      this.loadTreeData();
    },
    methods: {
      loadTreeData() {
        api.selector({}).then((res) => {
          this.treeData = mapTreeNodes(buildCategoryTree(res), this.onlyFinal);
        });
      },
      onChange(value) {
        const normalizedValue = normalizeTreeValue(value);
        if (isEmpty(value)) {
          this.$emit('update:value', normalizedValue);
          this.$emit('clear', normalizedValue);
        } else {
          this.$emit('update:value', normalizedValue);
          this.$emit('change', normalizedValue);
        }
      },
    },
  });
</script>

<style lang="less"></style>
