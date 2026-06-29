<template>
  <a-select
    v-model:value="model"
    allow-clear
    show-search
    show-arrow
    style="width: 100%"
    :filter-option="filterOption"
    :loading="loading"
    :options="options"
    :placeholder="placeholder"
    v-bind="$attrs"
    @focus="loadOptions()"
    @search="loadOptions"
    @dropdown-visible-change="handleDropdownVisibleChange"
    @change="onChange"
  />
</template>

<script>
  import { defineComponent } from 'vue';
  import * as api from '@/api/base-data/supplier';
  import {
    buildVisibleSelectOptions,
    filterSelectOption,
    mergeSelectOptionMap,
  } from '@/utils/searchSelect';
  import { isEmpty } from '@/utils/utils';

  function normalizeValue(value) {
    return isEmpty(value) ? undefined : value.toString();
  }

  function mapSupplierOption(item) {
    const value = normalizeValue(item.value ?? item.id);
    const label = item.label ?? item.name;
    return {
      label,
      value,
      keywords: [label, value].filter((text) => !!text).join(' '),
    };
  }

  export default defineComponent({
    name: 'SupplierSelector',
    inheritAttrs: false,
    props: {
      value: {
        type: [String, Number],
        default: undefined,
      },
      requestParams: {
        type: Object,
        default: () => {
          return {};
        },
      },
      placeholder: {
        type: String,
        default: '请选择供应商',
      },
    },
    data() {
      return {
        loading: false,
        options: [],
        optionMap: {},
        loaded: false,
      };
    },
    computed: {
      model: {
        get() {
          return normalizeValue(this.value);
        },
        set() {},
      },
    },
    watch: {
      value: {
        immediate: true,
        handler(value) {
          const normalizedValue = normalizeValue(value);
          if (isEmpty(normalizedValue)) {
            this.options = buildVisibleSelectOptions(undefined, this.optionMap, []);
            return;
          }

          this.ensureOption(normalizedValue);
        },
      },
    },
    created() {
      this.loadOptions();
    },
    methods: {
      filterOption(input, option) {
        return filterSelectOption(input, option);
      },
      async requestOptions(keyword = '') {
        const response = await api.selector({
          pageIndex: 1,
          pageSize: 20,
          ...this.requestParams,
          label: isEmpty(this.requestParams.label) ? keyword : this.requestParams.label,
        });

        return (response.datas || []).map((item) => mapSupplierOption(item));
      },
      syncOptions(searchOptions = []) {
        this.options = buildVisibleSelectOptions(this.model, this.optionMap, searchOptions);
      },
      async loadOptions(keyword = '') {
        this.loading = true;
        try {
          const options = await this.requestOptions(keyword);
          this.optionMap = mergeSelectOptionMap(this.optionMap, options);
          this.syncOptions(options);
          this.loaded = true;
        } finally {
          this.loading = false;
        }
      },
      handleDropdownVisibleChange(open) {
        if (open && (!this.loaded || this.options.length === 0)) {
          this.loadOptions();
        }
      },
      async ensureOption(value) {
        const normalizedValue = normalizeValue(value);
        if (isEmpty(normalizedValue)) {
          this.syncOptions();
          return;
        }

        if (this.optionMap[normalizedValue]) {
          this.syncOptions();
          return;
        }

        const res = await api.loadSupplier([normalizedValue]);
        const options = (res || []).map((item) => mapSupplierOption(item));
        this.optionMap = mergeSelectOptionMap(this.optionMap, options);
        this.syncOptions(options);
        this.loaded = true;
      },
      onChange(value) {
        const normalizedValue = normalizeValue(value);
        this.$emit('update:value', normalizedValue);
        if (isEmpty(normalizedValue)) {
          this.$emit('clear', normalizedValue);
        } else {
          this.$emit('change', normalizedValue);
        }
      },
    },
  });
</script>

<style lang="less"></style>
