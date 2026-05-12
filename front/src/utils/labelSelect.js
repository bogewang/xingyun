import * as customerApi from '@/api/base-data/customer';
import * as supplierApi from '@/api/base-data/supplier';
import * as userApi from '@/api/system/user';

export async function requestLabelSelectOptions(selectorApi, keyword = '', extraParams = {}) {
  const response = await selectorApi({
    pageIndex: 1,
    pageSize: 20,
    label: keyword,
    ...extraParams,
  });

  return (response.datas || []).map((item) => ({
    label: item.label,
    value: item.value,
    keywords: [item.label, item.value].filter((value) => !!value).join(' '),
  }));
}

export function requestCustomerSelectOptions(keyword = '') {
  return requestLabelSelectOptions(customerApi.selector, keyword);
}

export function requestSupplierSelectOptions(keyword = '') {
  return requestLabelSelectOptions(supplierApi.selector, keyword);
}

export function requestUserSelectOptions(keyword = '') {
  return requestLabelSelectOptions(userApi.selector, keyword);
}
