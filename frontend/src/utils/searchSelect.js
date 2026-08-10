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
  if (Array.isArray(value)) {
    return value.filter((item) => !!optionMap[item]);
  }

  return value && optionMap[value] ? value : undefined;
}

export function buildVisibleSelectOptions(selectedValue, optionMap, searchOptions) {
  const selectedValues = Array.isArray(selectedValue)
    ? selectedValue
    : selectedValue
    ? [selectedValue]
    : [];
  const selectedOptions = selectedValues
    .filter((value) => !!optionMap[value])
    .map((value) => optionMap[value]);
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
