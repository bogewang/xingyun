export const gridCollapseHeightMix = {
  data() {
    return {
      gridHeight: null,
    };
  },
  mounted() {
    window.addEventListener('resize', this.handleGridResize);
    this.syncGridHeight();
    window.setTimeout(() => this.syncGridHeight(), 240);
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleGridResize);
  },
  methods: {
    handleGridResize() {
      if (this.gridHeight) {
        this.syncGridHeight();
      }
    },
    syncGridHeight() {
      this.$nextTick(() => {
        const grid = this.$refs.grid;
        const gridEl = grid?.$el || grid;
        const parentEl = gridEl?.parentElement;
        if (!parentEl) {
          return;
        }
        const nextHeight = parentEl.clientHeight || 0;
        if (nextHeight > 0) {
          this.gridHeight = nextHeight;
        }
        if (grid) {
          grid.recalculate(true).then(() => grid.refreshScroll());
        }
      });
    },
    handleFormCollapse() {
      this.syncGridHeight();
      window.setTimeout(() => {
        const grid = this.$refs.grid;
        this.syncGridHeight();
        if (grid) {
          grid.refreshColumn();
        }
      }, 240);
    },
  },
};
