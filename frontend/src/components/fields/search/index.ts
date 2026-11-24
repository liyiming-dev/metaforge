// 搜索区字段类型 → 搜索控件的注册表。
// 列表搜索区也像表单一样用 <component :is> 按类型渲染，不在 ModelTable 里写一长串 v-if。
// 和表单字段组件的区别：boolean 用下拉（不是开关）、date/datetime 用范围选择（不是单日期）、enum 始终可清空。
import type { Component } from 'vue'

import type { FieldType } from '@/types/meta'
import ManyToOneField from '../ManyToOneField.vue'
import SearchBooleanField from './SearchBooleanField.vue'
import SearchDateField from './SearchDateField.vue'
import SearchEnumField from './SearchEnumField.vue'
import SearchNumberField from './SearchNumberField.vue'
import SearchStringField from './SearchStringField.vue'

export const searchFieldComponents: Partial<Record<FieldType, Component>> = {
  string: SearchStringField,
  text: SearchStringField,
  enum: SearchEnumField,
  boolean: SearchBooleanField,
  integer: SearchNumberField,
  decimal: SearchNumberField,
  date: SearchDateField,
  datetime: SearchDateField,
  manyToOne: ManyToOneField,
}
