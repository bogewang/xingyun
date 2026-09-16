<template>
  <Layout.Header :class="getHeaderClass">
    <!-- left start -->
    <div :class="`${prefixCls}-left`">
      <!-- logo -->
      <AppLogo
        v-if="getShowHeaderLogo || getIsMobile"
        :class="`${prefixCls}-logo`"
        :theme="getHeaderTheme"
        :style="getLogoWidth"
      />
      <LayoutTrigger
        v-if="
          (getShowContent && getShowHeaderTrigger && !getSplit && !getIsMixSidebar) || getIsMobile
        "
        :theme="getHeaderTheme"
        :sider="false"
      />
      <LayoutBreadcrumb v-if="getShowContent && getShowBread" :theme="getHeaderTheme" />
    </div>
    <!-- left end -->

    <!-- menu start -->
    <div v-if="getShowTopMenu && !getIsMobile" :class="`${prefixCls}-menu`">
      <LayoutMenu
        :isHorizontal="true"
        :theme="getHeaderTheme"
        :splitType="getSplitType"
        :menuMode="getMenuMode"
      />
    </div>
    <!-- menu-end -->

    <!-- action  -->
    <div :class="`${prefixCls}-action`">
      <div v-if="tenantName" :class="`${prefixCls}-tenant`" :title="tenantName">
        <span :class="`${prefixCls}-tenant__label`">租户</span>
        <span :class="`${prefixCls}-tenant__name`">{{ tenantName }}</span>
      </div>
      <a-select
        v-if="projects.length"
        v-model:value="currentProjectId"
        size="small"
        style="min-width: 180px; margin-right: 8px"
        @change="changeProject"
      >
        <a-select-option v-for="project in projects" :key="project.id" :value="project.id">
          项目：{{ project.name }}
        </a-select-option>
      </a-select>

      <AppSearch v-if="getShowSearch" :class="`${prefixCls}-action__item `" />

      <ExportCenter :class="`${prefixCls}-action__item export-center-item`" />

      <Notify v-if="getShowNotice" :class="`${prefixCls}-action__item notify-item`" />

      <FullScreen v-if="getShowFullScreen" :class="`${prefixCls}-action__item fullscreen-item`" />

      <UserDropDown :theme="getHeaderTheme" />
    </div>
  </Layout.Header>
</template>
<script lang="ts" setup>
  import { Layout } from 'ant-design-vue';
  import { computed, onMounted, ref, unref } from 'vue';

  import { AppLogo, AppSearch } from '@/components/Application';
  import { SettingButtonPositionEnum } from '@/enums/appEnum';
  import { MenuModeEnum, MenuSplitTyeEnum } from '@/enums/menuEnum';
  import { useHeaderSetting } from '@/hooks/setting/useHeaderSetting';
  import { useMenuSetting } from '@/hooks/setting/useMenuSetting';
  import { useRootSetting } from '@/hooks/setting/useRootSetting';
  import { useUserStore } from '@/store/modules/user';
  import { useAppInject } from '@/hooks/web/useAppInject';
  import { useDesign } from '@/hooks/web/useDesign';
  import { propTypes } from '@/utils/propTypes';
  import { selector, type ProjectSelectorItem } from '@/api/base-data/project';
  import { getCurrentProjectId, setCurrentProjectId } from '@/utils/currentProject';

  import LayoutMenu from '../menu/index.vue';
  import LayoutTrigger from '../trigger/index.vue';
  import { FullScreen, LayoutBreadcrumb, Notify, UserDropDown, ExportCenter } from './components';

  defineOptions({ name: 'LayoutHeader' });

  const props = defineProps({
    fixed: propTypes.bool,
  });
  const { prefixCls } = useDesign('layout-header');
  const userStore = useUserStore();
  const {
    getShowTopMenu,
    getShowHeaderTrigger,
    getSplit,
    getIsMixMode,
    getMenuWidth,
    getIsMixSidebar,
  } = useMenuSetting();
  const { getUseErrorHandle, getShowSettingButton, getSettingButtonPosition } = useRootSetting();

  const {
    getHeaderTheme,
    getShowFullScreen,
    getShowNotice,
    getShowContent,
    getShowBread,
    getShowHeaderLogo,
    getShowHeader,
    getShowSearch,
  } = useHeaderSetting();

  const { getIsMobile } = useAppInject();

  const getHeaderClass = computed(() => {
    const theme = unref(getHeaderTheme);
    return [
      prefixCls,
      {
        [`${prefixCls}--fixed`]: props.fixed,
        [`${prefixCls}--mobile`]: unref(getIsMobile),
        [`${prefixCls}--${theme}`]: theme,
      },
    ];
  });

  const getShowSetting = computed(() => {
    if (!unref(getShowSettingButton)) {
      return false;
    }
    const settingButtonPosition = unref(getSettingButtonPosition);

    if (settingButtonPosition === SettingButtonPositionEnum.AUTO) {
      return unref(getShowHeader);
    }
    return settingButtonPosition === SettingButtonPositionEnum.HEADER;
  });

  const getLogoWidth = computed(() => {
    if (!unref(getIsMixMode) || unref(getIsMobile)) {
      return {};
    }
    const width = unref(getMenuWidth) < 180 ? 180 : unref(getMenuWidth);
    return { width: `${width}px` };
  });

  const getSplitType = computed(() => {
    return unref(getSplit) ? MenuSplitTyeEnum.TOP : MenuSplitTyeEnum.NONE;
  });

  const getMenuMode = computed(() => {
    return unref(getSplit) ? MenuModeEnum.HORIZONTAL : null;
  });

  const tenantName = computed(() => userStore.getUserInfo?.tenantName || '');
  const projects = ref<ProjectSelectorItem[]>([]);
  const currentProjectId = ref(getCurrentProjectId());

  /** 加载项目，并确保当前项目仍可用。 */
  onMounted(async () => {
    projects.value = await selector();
    if (!projects.value.some((item) => item.id === currentProjectId.value) && projects.value.length) {
      currentProjectId.value = projects.value[0].id;
      setCurrentProjectId(currentProjectId.value);
    }
  });

  /** 切换项目后刷新，清除项目范围页面中的旧数据。 */
  function changeProject(projectId: string) {
    setCurrentProjectId(projectId);
    window.location.reload();
  }
</script>
<style lang="less">
  @import url('./index.less');
</style>
