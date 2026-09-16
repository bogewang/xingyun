/** 当前项目在浏览器中的存储键。 */
const CURRENT_PROJECT_KEY = 'xingyun-current-project-id';

/** 读取当前项目；兼容升级前的默认项目。 */
export function getCurrentProjectId(): string {
  return localStorage.getItem(CURRENT_PROJECT_KEY) || 'default-project';
}

/** 切换当前项目。 */
export function setCurrentProjectId(projectId: string): void {
  localStorage.setItem(CURRENT_PROJECT_KEY, projectId);
}
