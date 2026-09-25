<template>
  <a-select
    :value="value"
    mode="multiple"
    :options="options"
    :loading="loading"
    placeholder="请选择报价单（可多选）"
    allow-clear
    show-search
    option-filter-prop="label"
    style="width: 100%"
    @update:value="$emit('update:value', $event)"
  />
</template>
<script setup lang="ts">
  import { onMounted, ref } from 'vue';
  import { quoteOptions } from '@/api/base-data/product/info';

  defineProps<{ value?: string[] }>();
  defineEmits(['update:value']);
  const options = ref<{ value: string; label: string }[]>([]);
  const loading = ref(false);
  /** 加载报价单名称及有效期，便于区分同名报价单。 */
  async function loadOptions() {
    loading.value = true;
    try {
      options.value = (await quoteOptions()).map((item) => ({
        value: item.id,
        label: `${item.name}（${item.startDate} ~ ${item.endDate}）`,
      }));
    } finally {
      loading.value = false;
    }
  }
  onMounted(loadOptions);
</script>
