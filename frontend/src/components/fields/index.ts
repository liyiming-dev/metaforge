// 字段类型 -> 输入组件的注册表。表单里一律用 <component :is="fieldComponents[field.type]"> 渲染，
// 避免在表单里写一长串按字段类型判断的 v-if。以后新增一种字段类型，只需在这里加一行。
import type { Component } from 'vue'

import type { FieldType } from '@/types/meta'
import StringField from './StringField.vue'
import TextField from './TextField.vue'
import NumberField from './NumberField.vue'
import BooleanField from './BooleanField.vue'
import DateField from './DateField.vue'
import EnumField from './EnumField.vue'

export const fieldComponents: Partial<Record<FieldType, Component>> = {
  string: StringField,
  text: TextField,
  integer: NumberField,
  decimal: NumberField,
  boolean: BooleanField,
  date: DateField,
  datetime: DateField,
  enum: EnumField,
}
