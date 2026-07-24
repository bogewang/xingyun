<template>
  <shared-batch-add-product
    ref="shared"
    biz-type="sale"
    :show-inquiry-product="true"
    :sc-id="scId"
    :is-return="isReturn"
    v-bind="$attrs"
    @close="$emit('close')"
    @confirm="(records) => $emit('confirm', records)"
  />
</template>
<script>
  import { defineComponent } from 'vue';
  import SharedBatchAddProduct from '@/views/sc/shared/batch-add-product.vue';

  export default defineComponent({
    name: 'SaleBatchAddProduct',
    emits: ['close', 'confirm'],
    components: {
      SharedBatchAddProduct,
    },
    props: {
      scId: {
        type: String,
        default: '',
      },
      isReturn: {
        type: Boolean,
        default: false,
      },
    },
    methods: {
      openDialog() {
        this.$refs.shared.openDialog();
      },
      closeDialog() {
        this.$refs.shared.closeDialog();
      },
    },
  });
</script>
