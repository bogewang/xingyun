<template>
  <a-select
    :value="selectedValue"
    :placeholder="row.unitName || row.unit || '请选择单位'"
    :disabled="!row.productId"
    :loading="loading"
    style="width: 100%"
    :options="options"
    :not-found-content="loading ? '加载中' : '请先在商品档案维护单位'"
    @dropdown-visible-change="loadUnits"
    @change="selectUnit"
  />
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import * as productApi from '@/api/base-data/product/info';
  import { createError } from '@/hooks/web/msg';
  import { applyQuoteUnit, expandQuoteUnits, isValidQuoteRate, quoteUnitOptionValue, type QuoteUnit } from '../quoteUnit';

  const props = defineProps<{ row: Record<string, any>; unitNameMap: Record<string, string> }>();
  const units = ref<QuoteUnit[]>([]);
  const loading = ref(false);
  let requestVersion = 0;

  /** 切换商品时清除旧选项，防止异步结果串到其他商品。 */
  watch(() => props.row.productId, () => {
    requestVersion += 1;
    units.value = [];
    loading.value = false;
  });

  /** 保留当前单位名称快照，不能只用单位ID区分包和袋。 */
  const selectedValue = computed(() => {
    const name = props.row.unitName || props.row.unit;
    return name ? quoteUnitOptionValue({ id: props.row.unitId || null, unitName: name }) : undefined;
  });

  // 当前快照始终可读；停用或已删除单位不作为新单位选择。
  const options = computed(() => {
    const result = units.value.map((unit) => ({ value: quoteUnitOptionValue(unit), label: unit.unitName }));
    if (selectedValue.value && !result.some((unit) => unit.value === selectedValue.value)) {
      result.unshift({ value: selectedValue.value, label: props.row.unitName || props.row.unit });
    }
    return result;
  });

  /** 展开时读取商品单位，加载失败允许重新展开重试。 */
  async function loadUnits(open: boolean) {
    if (!open || !props.row.productId || loading.value) return;
    const version = ++requestVersion;
    loading.value = true;
    try {
      const product = await productApi.get(props.row.productId);
      if (version === requestVersion) {
        const configured = product.units || [];
        // 旧商品尚未维护多单位时，商品档案的主单位仍按 1:1 使用。
        const baseName = props.unitNameMap[product.unit] || product.unit || props.row.unit;
        const source = configured.length || !baseName
          ? configured
          : [{ id: null, unitName: baseName, conversionRate: 1, available: true }];
        units.value = expandQuoteUnits(source);
      }
    } catch (error) {
      if (version === requestVersion) createError('商品单位加载失败，请重新展开重试');
    } finally {
      if (version === requestVersion) loading.value = false;
    }
  }

  /** 选中有效单位后更新快照；超过两位精度时提示维护档案而非直接取整。 */
  function selectUnit(value: string) {
    const unit = units.value.find((item) => quoteUnitOptionValue(item) === value);
    if (!unit) return;
    if (!isValidQuoteRate(unit.conversionRate)) {
      createError('该单位换算率必须大于0且最多两位小数，请先在商品档案调整');
      return;
    }
    applyQuoteUnit(props.row, unit);
  }
</script>
