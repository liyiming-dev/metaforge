// 列表单元格的显示规则：把接口返回的原始值，转成给用户看的文字。
import type { FieldDef } from '@/types/meta'

/** 系统字段的列标题 */
export const SYSTEM_FIELD_TITLES: Record<string, string> = {
  id: 'ID',
  created_at: '创建时间',
  updated_at: '修改时间',
}

/**
 * 把一个字段的值转成显示文字。
 * @param field 字段定义；系统字段（id、created_at 等）没有定义，传 undefined
 * @param value 记录里该字段的值
 */
export function displayValue(field: FieldDef | undefined, value: unknown): string {
  if (value === null || value === undefined || value === '') {
    return '—'
  }
  if (!field) {
    return String(value)
  }
  switch (field.type) {
    case 'enum': {
      const option = field.options?.find((o) => o.value === value)
      return option ? option.label : String(value)
    }
    case 'boolean':
      return value ? '是' : '否'
    case 'manyToOne':
      // 接口返回 { id, label }，这里取 label；兜底直接 String(value)
      if (typeof value === 'object' && value !== null && 'label' in value) {
        return String((value as { label: unknown }).label)
      }
      return String(value)
    case 'decimal': {
      const scale = field.scale ?? 4
      const num = Number(value)
      if (Number.isNaN(num)) {
        return String(value)
      }
      // 最多保留 scale 位小数，再去掉末尾多余的 0
      return num.toFixed(scale).replace(/\.?0+$/, '')
    }
    default:
      return String(value)
  }
}
