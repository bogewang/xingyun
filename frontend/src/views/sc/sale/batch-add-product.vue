<template>
  <shared-batch-add-product
    ref="shared"
    biz-type="sale"
    :show-inquiry-product="showInquiryProduct"
    :sc-id="scId"
    :is-return="isReturn"
    :quote-products="quoteProducts"
    :quote-pricing-enabled="quotePricingEnabled"
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
      showInquiryProduct: {
        type: Boolean,
        default: false,
      },
      quoteProducts: {
        type: Array,
        default: () => [],
      },
      quotePricingEnabled: {
        type: Boolean,
        default: false,
      },
    },
    emits: ['close', 'confirm'],
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
