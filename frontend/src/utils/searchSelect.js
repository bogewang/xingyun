export function buildSelectKeywords(...values) {
  return values.filter((value) => !!value).join(' ');
}

export function mergeSelectOptionMap(optionMap, options) {
  const nextOptionMap = { ...optionMap };

  options.forEach((option) => {
    nextOptionMap[option.value] = option;
  });

  return nextOptionMap;
}

export function normalizeSelectValue(value, optionMap) {
  return value && optionMap[value] ? value : undefined;
}

export function buildVisibleSelectOptions(selectedValue, optionMap, searchOptions) {
  const selectedOptions =
    selectedValue && optionMap[selectedValue] ? [optionMap[selectedValue]] : [];
  const visibleOptionMap = {};

  [...selectedOptions, ...searchOptions].forEach((option) => {
    visibleOptionMap[option.value] = option;
  });

  return Object.values(visibleOptionMap);
}

export function filterSelectOption(input, option) {
  const keyword = (option?.keywords || option?.label || '').toString().toLowerCase();
  return keyword.includes((input || '').toLowerCase());
}
