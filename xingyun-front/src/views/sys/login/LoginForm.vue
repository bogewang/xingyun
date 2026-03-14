<template>
  <a-card>
    <login-form-title v-show="getShow" class="enter-x" />
    <a-form
      class="p-4 enter-x"
      :model="formData"
      :rules="getFormRules"
      ref="formRef"
      v-show="getShow"
      @keypress.enter="handleLogin"
    >
      <a-form-item
        name="tenantName"
        class="enter-x"
        v-if="requireTenant.enable && isEmpty(requireTenant.tenantId)"
      >
        <a-select
          size="large"
          ref="tenantInput"
          v-model:value="formData.tenantName"
          placeholder="请选择租户"
          class="fix-auto-fill"
          show-search
          :options="tenantOptions"
          :filter-option="filterTenantOption"
        />
      </a-form-item>
      <a-form-item name="username" class="enter-x">
        <a-input
          size="large"
          ref="usernameInput"
          v-model:value="formData.username"
          placeholder="请输入用户名"
          class="fix-auto-fill"
        >
          <template #prefix>
            <UserOutlined />
          </template>
        </a-input>
      </a-form-item>
      <a-form-item name="password" class="enter-x">
        <a-input-password
          size="large"
          visibilityToggle
          v-model:value="formData.password"
          placeholder="请输入密码"
        >
          <template #prefix>
            <KeyOutlined />
          </template>
        </a-input-password>
      </a-form-item>
      <a-form-item class="enter-x">
        <a-button type="primary" size="large" block @click="handleLogin" :loading="loading">
          登录
        </a-button>
      </a-form-item>
    </a-form>

    <LoginCaptchaModal ref="loginCaptchaDialog" @confirm="doConfirmCaptcha" />
  </a-card>
</template>
<script lang="ts" setup>
  import { computed, onMounted, reactive, ref, unref } from 'vue';
  import { KeyOutlined, UserOutlined } from '@ant-design/icons-vue';
  import LoginFormTitle from './LoginFormTitle.vue';
  import LoginCaptchaModal from './LoginCaptchaModal.vue';
  import { useUserStore } from '/@/store/modules/user';
  import { LoginStateEnum, useFormRules, useFormValid, useLoginState } from './useLogin';
  import { createSuccessTip } from '@/hooks/web/msg';
  import { welcomeMsg, isEmpty } from '@/utils/utils';
  import { TenantRequireBo } from '@/api/sys/model/tenantRequireBo';
  import { getTenantListApi } from '@/api/sys/user';

  const userStore = useUserStore();
  const { getLoginState } = useLoginState();
  const { getFormRules } = useFormRules();
  const formRef = ref();
  const loading = ref(false);
  const requireTenant = ref({} as TenantRequireBo);
  const tenantOptions = ref([]);

  const formData = reactive({
    tenantName: null,
    username: null,
    password: null,
  });

  const { validForm } = useFormValid(formRef);

  const loginCaptchaDialog = ref();
  const usernameInput = ref();
  const tenantInput = ref();

  const focusInput = () => {
    if (requireTenant.value.enable && isEmpty(requireTenant.value.tenantId)) {
      tenantInput.value?.focus?.();
      return;
    }
    usernameInput.value?.focus?.();
  };

  const filterTenantOption = (inputValue, option) => {
    return option?.label?.indexOf(inputValue) > -1;
  };

  type TenantItem = {
    name: string;
  };

  const loadTenantOptions = async () => {
    const result = await getTenantListApi<TenantItem[]>();
    const datas = result || [];
    tenantOptions.value = datas.map((item: TenantItem) => {
      return {
        label: item.name,
        value: item.name,
      };
    });
    if (!formData.tenantName && tenantOptions.value.length > 0) {
      formData.tenantName = tenantOptions.value[0].value;
    }
  };

  onMounted(async () => {
    requireTenant.value = await userStore.getTenantRequire();
    if (requireTenant.value.enable && isEmpty(requireTenant.value.tenantId)) {
      await loadTenantOptions();
    }

    focusInput();
  });

  const getShow = computed(() => unref(getLoginState) === LoginStateEnum.LOGIN);

  function loginSuccessTip(userInfo) {
    createSuccessTip(welcomeMsg(userInfo.name));
  }

  async function handleLogin() {
    const data = await validForm();
    if (!data) return;

    const captchaRequire = await userStore.getCaptchaRequire(
      data.tenantName,
      data.username,
      requireTenant.value.tenantId,
    );
    if (captchaRequire) {
      loginCaptchaDialog.value.openDialog();
    } else {
      focusInput();
      doLogin(data.tenantName, data.username, data.password, undefined, undefined);
    }
  }

  async function doLogin(tenantName, username, password, sn, captcha) {
    try {
      loading.value = true;
      const userInfo = await userStore.login({
        tenantId: requireTenant.value.tenantId,
        tenantName: tenantName,
        password: password,
        username: username,
        sn: sn,
        captcha: captcha,
      });
      if (userInfo) {
        loginSuccessTip(userInfo);
      }
    } finally {
      loading.value = false;
    }
  }

  async function doConfirmCaptcha({ sn, captcha }) {
    const data = await validForm();
    if (!data) return;
    focusInput();
    doLogin(data.tenantName, data.username, data.password, sn, captcha);
  }
</script>
<style lang="less" scoped>
  .captcha-box {
    cursor: pointer !important;
  }
</style>
