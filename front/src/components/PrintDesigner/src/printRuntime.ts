import { openPrintDialog } from '/@/components/PrintDialog';
import { normalizePrintData, normalizeTemplate } from './printUtils';

interface TemplateOption {
  id: string;
  name: string;
  bizType?: string;
}

interface PreviewOptions {
  title?: string;
  bizType?: string;
  templateId?: string;
  templateList?: TemplateOption[];
  enableTemplateSwitch?: boolean;
}

function preview(templateJson: unknown, data: unknown, options: PreviewOptions = {}) {
  openPrintDialog({
    bizType: options.bizType,
    templateId: options.templateId,
    templateList: options.templateList || [],
    enableTemplateSwitch: options.enableTemplateSwitch === true,
    templateJson: normalizeTemplate(templateJson),
    printData: normalizePrintData(data),
  });
}

export default {
  preview,
};
