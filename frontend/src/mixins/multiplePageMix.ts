import eventBus from '@/events/eventBus';
import { useRefreshStore } from '@/store/modules/multipleTab';
import { isEmpty } from '@/utils/utils';

export const multiplePageMix = {
  activated() {
    const refreshStore = useRefreshStore();
    const currentPath = this.$route.path;
    if (refreshStore.checkAndClear(currentPath)) {
      try {
        this.onRefreshPage();
      } catch (e) {
        /* empty */
      }
    }
  },
  methods: {
    openChildPage(route: string | { path?: string; [key: string]: any }) {
      const refreshStore = useRefreshStore();
      const targetPath =
        typeof route === 'string' ? route.split('?')[0] : route.path ? String(route.path) : '';
      refreshStore.setCacheFlag(targetPath, this.$route.path);
      this.$router.push(route);
    },
    closeCurrentPage(refreshParent: boolean = true): void {
      const refreshStore = useRefreshStore();
      const parentPath = refreshParent ? refreshStore.getCacheFlag(this.$route.path) : '';
      refreshStore.setRefreshFlag(parentPath);
      eventBus.$emit(eventBus.$otherEvent.CLOSE_CURRENT_TAB);

      if (!isEmpty(parentPath)) {
        this.$router.push(parentPath);
      }
    },
  },
};
