<template>
  <div class="app-card-container">
    <div v-loading="loading" v-permission="['base-data:product:info:add']">
      <a-form
          ref="form"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 14 }"
          :model="formData"
          :rules="rules"
      >
        <a-row>
          <a-col :md="6" :sm="24">
            <a-form-item label="编号" name="code">
              <a-space :size="4" style="display: flex; flex-wrap: nowrap; width: 100%">
                <a-input
                    v-model:value.trim="formData.code"
                    style="flex: 1; min-width: 0"
                    allow-clear
                />
                <a-button type="primary" style="flex: none" @click="onGenerateCode"
                >点此生成
                </a-button
                >
              </a-space>
            </a-form-item>
          </a-col>
          <a-col :md="6" :sm="24">
            <a-form-item label="名称" name="name">
              <a-input v-model:value="formData.name" allow-clear/>
            </a-form-item>
          </a-col>
          <a-col :md="6" :sm="24">
            <a-form-item label="商品分类" name="categoryId">
              <product-category-selector
                  v-model:value="formData.categoryId"
                  @change="selectCategory"
              />
            </a-form-item>
          </a-col>
          <a-col :md="6" :sm="24">
            <a-form-item label="规格" name="spec">
              <a-input v-model:value="formData.spec" allow-clear/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row>
          <a-col :md="6" :sm="24">
            <a-form-item label="单位" name="unit">
              <a-space
                  :size="6"
                  style="display: flex; flex-wrap: nowrap; align-items: center; white-space: nowrap"
              >
                <a-select
                    v-model:value="formData.unit"
                    placeholder="请选择主单位"
                    allow-clear
                    show-search
                    style="width: 140px; flex: none"
                >
                  <a-select-option v-for="item in unitOptions" :key="item.id" :value="item.id">{{
                      item.name
                    }}
                  </a-select-option>
                </a-select
                >
                <a-checkbox
                    v-model:checked="formData.multiUnitEnabled"
                    style="white-space: nowrap"
                    @change="handleMultiUnitChange"
                >启用多单位
                </a-checkbox
                >
              </a-space>
            </a-form-item>
          </a-col>
          <a-col :md="6" :sm="24">
            <a-form-item label="采购价（元）" name="purchasePrice">
              <a-input v-model:value="formData.purchasePrice" allow-clear/>
            </a-form-item>
          </a-col>

          <a-col :md="6" :sm="24">
            <a-form-item label="销售价（元）" name="salePrice">
              <a-input v-model:value="formData.salePrice" allow-clear/>
            </a-form-item>
          </a-col>
          <a-col :md="6" :sm="24">
            <a-form-item label="零售价（元）" name="retailPrice">
              <a-input v-model:value="formData.retailPrice" allow-clear/>
            </a-form-item>
          </a-col>
          <a-col :md="6" :sm="24">
            <a-form-item label="询价商品" name="inquiryProduct">
              <a-checkbox v-model:checked="formData.inquiryProduct">询价商品</a-checkbox>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row v-if="formData.multiUnitEnabled">
          <a-col :span="24"
          >
            <a-form-item label="辅单位换算" :label-col="{ span: 2 }" :wrapper-col="{ span: 22 }">
              <a-space
                  v-for="(item, index) in formData.auxiliaryUnits"
                  :key="index"
                  style="display: flex; margin-bottom: 8px"
              >
                <a-select
                    v-model:value="item.unitName"
                    placeholder="辅单位"
                    style="width: 130px"
                    show-search
                >
                  <a-select-option
                      v-for="option in unitOptions"
                      :key="option.id"
                      :value="option.name"
                  >{{ option.name }}
                  </a-select-option
                  >
                </a-select
                >
                <span>=</span
                >
                <a-input-number
                    v-model:value="item.conversionRate"
                    :min="0.000001"
                    :precision="6"
                    style="width: 120px"
                />
                <span>{{ getBaseUnitName() }}</span
                >
                <a-button type="link" danger @click="removeAuxiliaryUnit(index)"
                >删除
                </a-button
                >
              </a-space
              >
              <a-button type="dashed" style="width: 360px" @click="addAuxiliaryUnit"
              >+ 继续添加单位
              </a-button
              >
            </a-form-item>
          </a-col
          >
        </a-row>
        <a-row>
          <a-col :md="6" :sm="24">
            <a-form-item label="简称" name="shortName"
            >
              <a-input v-model:value="formData.shortName" allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :md="6" :sm="24">
            <a-form-item label="SKU编号" name="skuCode">
              <a-input v-model:value="formData.skuCode" allow-clear/>
            </a-form-item>
          </a-col>
          <a-col :md="6" :sm="24">
            <a-form-item label="简码" name="externalCode">
              <a-input v-model:value="formData.externalCode" allow-clear/>
            </a-form-item>
          </a-col>
          <a-col :md="6" :sm="24">
            <a-form-item label="商品品牌" name="brandId">
              <product-brand-selector v-model:value="formData.brandId"/>
            </a-form-item>
          </a-col>
          <a-col :md="6" :sm="24"
          >
            <a-form-item label="进项税率（%）" name="taxRate"
            >
              <a-input v-model:value="formData.taxRate" allow-clear/>
            </a-form-item
            >
          </a-col>
          <a-col :md="6" :sm="24"
          >
            <a-form-item label="销项税率（%）" name="saleTaxRate"
            >
              <a-input v-model:value="formData.saleTaxRate" allow-clear/>
            </a-form-item
            >
          </a-col>
          <a-col :md="6" :sm="24">
            <a-form-item label="备注" name="remark">
              <a-input v-model:value="formData.remark" allow-clear/>
            </a-form-item>
          </a-col>
          <a-col :md="6" :sm="24">
            <a-form-item label="备注二" name="remark2">
              <a-input v-model:value="formData.remark2" allow-clear/>
            </a-form-item>
          </a-col>

          <a-col :md="6" :sm="24">
            <a-form-item label="默认供应商" name="defaultSupplier">
              <supplier-selector v-model:value="formData.defaultSupplier"/>
            </a-form-item>
          </a-col>

          <a-col :md="6" :sm="24">
            <a-form-item label="别名" name="alias">
              <a-textarea v-model:value="formData.alias" allow-clear :rows="2"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row>
          <a-col v-for="modelor in modelorList" :key="modelor.id" :md="6" :sm="24">
            <a-form-item :label="modelor.name" :required="modelor.isRequired">
              <a-select
                  v-if="COLUMN_TYPE.MULTIPLE.equalsCode(modelor.columnType)"
                  v-model:value="modelor.text"
                  mode="multiple"
                  placeholder="请选择"
              >
                <a-select-option v-for="item in modelor.items" :key="item.id" :value="item.id">{{
                    item.name
                  }}
                </a-select-option>
              </a-select>
              <a-select
                  v-if="COLUMN_TYPE.SINGLE.equalsCode(modelor.columnType)"
                  v-model:value="modelor.text"
                  placeholder="请选择"
              >
                <a-select-option v-for="item in modelor.items" :key="item.id" :value="item.id">{{
                    item.name
                  }}
                </a-select-option>
              </a-select>
              <div v-else-if="COLUMN_TYPE.CUSTOM.equalsCode(modelor.columnType)">
                <a-input-number
                    v-if="COLUMN_DATA_TYPE.INT.equalsCode(modelor.columnDataType)"
                    v-model:value="modelor.text"
                    class="number-input"
                />
                <a-input-number
                    v-else-if="COLUMN_DATA_TYPE.FLOAT.equalsCode(modelor.columnDataType)"
                    v-model:value="modelor.text"
                    :precision="2"
                    class="number-input"
                />
                <a-input
                    v-else-if="COLUMN_DATA_TYPE.STRING.equalsCode(modelor.columnDataType)"
                    v-model:value="modelor.text"
                />
                <a-date-picker
                    v-else-if="COLUMN_DATA_TYPE.DATE.equalsCode(modelor.columnDataType)"
                    v-model:value="modelor.text"
                    placeholder=""
                    value-format="YYYY-MM-DD"
                />
                <a-time-picker
                    v-else-if="COLUMN_DATA_TYPE.TIME.equalsCode(modelor.columnDataType)"
                    v-model:value="modelor.text"
                    placeholder=""
                    value-format="HH:mm:ss"
                />
                <a-date-picker
                    v-else-if="COLUMN_DATA_TYPE.DATE_TIME.equalsCode(modelor.columnDataType)"
                    v-model:value="modelor.text"
                    placeholder=""
                    show-time
                    value-format="YYYY-MM-DD HH:mm:ss"
                />
              </div>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <div class="form-modal-footer">
        <a-space>
          <a-button type="primary" :loading="loading" @click="submit">保存</a-button>
          <a-button type="primary" :loading="loading" @click="submitAndAdd">保存并新增</a-button>
          <a-button @click="closeDialog">关闭</a-button>
        </a-space>
      </div>
    </div>
  </div>
</template>
<script>
import {defineComponent, nextTick} from 'vue';
import {validCode} from '@/utils/validate';
import * as api from '@/api/base-data/product/info';
import * as propertyApi from '@/api/base-data/product/property';
import * as unitApi from '@/api/base-data/unit';
import {multiplePageMix} from '@/mixins/multiplePageMix';
import {isArray, isEmpty, isFloat, isFloatGeZero, isNumberPrecision} from '@/utils/utils';
import {createError, createSuccessAutoClose} from '@/hooks/web/msg';
import ProductBrandSelector from '@/components/Selector/ProductBrandSelector.vue';
import ProductCategorySelector from '@/components/Selector/ProductCategorySelector.vue';
import SupplierSelector from '@/components/Selector/SupplierSelector.vue';
import {COLUMN_TYPE} from '@/enums/biz/columnType';
import {COLUMN_DATA_TYPE} from '@/enums/biz/columnDataType';

export default defineComponent({
  name: 'AddProduct',
  components: {
    ProductBrandSelector,
    ProductCategorySelector,
    SupplierSelector,
  },
  mixins: [multiplePageMix],
  setup() {
    return {
      COLUMN_TYPE,
      COLUMN_DATA_TYPE,
    };
  },
  data() {
    return {
      // 是否显示加载框
      loading: false,
      // 表单数据
      formData: {},
      unitOptions: [],
      // 属性列表
      modelorList: [],
      // 表单校验规则
      rules: {
        code: [
          {required: true, message: '请输入编号'},
          {validator: validCode, message: '编号必须由字母、数字、"-_."组成，长度不能超过20位'},
        ],
        name: [{required: true, message: '请输入名称'}],
        unit: [{required: true, message: '请输入单位'}],
        categoryId: [{required: true, message: '请选择分类'}],
        taxRate: [
          {
            validator: (rule, value) => {
              if (!isEmpty(value)) {
                if (!isFloat(value)) {
                  return Promise.reject('进项税率（%）必须是数字');
                }
                if (!isFloatGeZero(value)) {
                  return Promise.reject('进项税率（%）不允许小于0');
                }
                if (!isNumberPrecision(value, 2)) {
                  return Promise.reject('进项税率（%）最多允许2位小数');
                }
              }

              return Promise.resolve();
            },
          },
        ],
        saleTaxRate: [
          {
            validator: (rule, value) => {
              if (!isEmpty(value)) {
                if (!isFloat(value)) {
                  return Promise.reject('销项税率（%）必须是数字');
                }
                if (!isFloatGeZero(value)) {
                  return Promise.reject('销项税率（%）不允许小于0');
                }
                if (!isNumberPrecision(value, 2)) {
                  return Promise.reject('销项税率（%）最多允许2位小数');
                }
              }

              return Promise.resolve();
            },
          },
        ],
        purchasePrice: [
          {
            validator: (rule, value) => {
              if (!isEmpty(value)) {
                if (!isFloat(value)) {
                  return Promise.reject('采购价（元）必须是数字');
                }
                if (!isFloatGeZero(value)) {
                  return Promise.reject('采购价（元）不允许小于0');
                }
                if (!isNumberPrecision(value, 6)) {
                  return Promise.reject('采购价（元）最多允许6位小数');
                }
              }

              return Promise.resolve();
            },
          },
        ],
        salePrice: [
          {
            validator: (rule, value) => {
              if (!isEmpty(value)) {
                if (!isFloat(value)) {
                  return Promise.reject('销售价（元）必须是数字');
                }
                if (!isFloatGeZero(value)) {
                  return Promise.reject('销售价（元）不允许小于0');
                }
                if (!isNumberPrecision(value, 6)) {
                  return Promise.reject('销售价（元）最多允许6位小数');
                }
              }

              return Promise.resolve();
            },
          },
        ],
        retailPrice: [
          {
            validator: (rule, value) => {
              if (!isEmpty(value)) {
                if (!isFloat(value)) {
                  return Promise.reject('零售价（元）必须是数字');
                }
                if (!isFloatGeZero(value)) {
                  return Promise.reject('零售价（元）不允许小于0');
                }
                if (!isNumberPrecision(value, 6)) {
                  return Promise.reject('零售价（元）最多允许6位小数');
                }
              }

              return Promise.resolve();
            },
          },
        ],
      },
    };
  },
  computed: {},
  created() {
    // 初始化表单数据
    this.initFormData();
    this.loadUnitOptions();
  },
  methods: {
    loadUnitOptions() {
      unitApi.query({pageSize: 50, pageIndex: 1}).then((res) => {
        this.unitOptions = res?.datas || [];
      });
    },
    getBaseUnitName() {
      return this.unitOptions.find((item) => item.id === this.formData.unit)?.name || '主单位';
    },
    // 关闭对话框
    closeDialog() {
      this.closeCurrentPage();
    },
    // 初始化表单数据
    initFormData() {
      this.formData = {inquiryProduct: false, multiUnitEnabled: false, auxiliaryUnits: []};
      this.modelorList = [];

      this.onGenerateCode();
    },
    // 提交表单事件
    buildUnits() {
      const baseUnit = this.unitOptions.find((item) => item.id === this.formData.unit)?.name;
      const units = baseUnit ? [{unitName: baseUnit, conversionRate: 1, available: true}] : [];
      if (this.formData.multiUnitEnabled) {
        (this.formData.auxiliaryUnits || []).forEach((item) => {
          if (item.unitName && Number(item.conversionRate) > 0)
            units.push({...item, available: true});
        });
      }
      return units;
    },
    addAuxiliaryUnit() {
      this.formData.auxiliaryUnits.push({unitName: '', conversionRate: null});
    },
    removeAuxiliaryUnit(index) {
      this.formData.auxiliaryUnits.splice(index, 1);
    },
    handleMultiUnitChange() {
      if (this.formData.multiUnitEnabled && this.formData.auxiliaryUnits.length === 0)
        this.addAuxiliaryUnit();
    },
    async submit() {
      await this.doSubmit(false);
    },
    async submitAndAdd() {
      await this.doSubmit(true);
    },
    async doSubmit(continueAdd) {
      let valid = true;

      await this.$refs.form.validate().then((res) => {
        valid = res;
      });

      if (!valid) {
        return;
      }
      if (!isEmpty(this.modelorList)) {
        this.modelorList
            .filter((item) => item.isRequired)
            .every((item) => {
              if (isEmpty(item.text)) {
                createError(item.name + '不能为空！');
                valid = false;
                return false;
              }

              return true;
            });
      }

      if (!valid) {
        return;
      }

      const properties = this.modelorList
          .filter((item) => !isEmpty(item.text))
          .map((item) => {
            return {
              id: item.id,
              text: isArray(item.text) ? JSON.stringify(item.text) : item.text,
            };
          });

      const params = Object.assign({}, this.formData, {
        properties: properties,
        units: this.buildUnits(),
      });

      this.loading = true;
      api
          .create(params)
          .then(() => {
            createSuccessAutoClose('新增成功！');
            this.$emit('confirm');
            if (continueAdd) {
              this.initFormData();
              nextTick(() => {
                this.$refs.form?.clearValidate();
              });
              return;
            }

            this.closeDialog();
          })
          .finally(() => {
            this.loading = false;
          });
    },
    selectCategory(val) {
      this.modelorList = [];
      if (!isEmpty(val)) {
        propertyApi.getModelorByCategory(val).then((res) => {
          this.modelorList = res;
        });
      }
    },
    onGenerateCode() {
      api.generateCode().then((res) => {
        this.formData.code = res;
      });
    },
  },
});
</script>
