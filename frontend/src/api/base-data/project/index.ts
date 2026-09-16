import { defHttp } from '/@/utils/http/axios';

const region = 'cloud-api';

/** 项目选择项。 */
export interface ProjectSelectorItem {
  id: string;
  code: string;
  name: string;
}

/** 查询当前租户的可用项目。 */
export function selector(): Promise<ProjectSelectorItem[]> {
  return defHttp.get<ProjectSelectorItem[]>({ url: '/basedata/project/selector' }, { region });
}
