// MES 示例：元模型和几条演示数据，只给 mock 用。
// mesBundle 是 docs/contract/samples/mes.json 的副本，契约样例改了，这里要跟着改。

import type { DataRecord, MetaBundle } from '@/types/api'

export const mesBundle: MetaBundle = {
  modules: [
    { name: 'mes', displayName: '生产管理', icon: 'AppstoreOutlined', sort: 10 },
    { name: 'base', displayName: '基础设置', icon: 'SettingOutlined', sort: 20 },
  ],
  models: [
    {
      name: 'material',
      displayName: '物料',
      description: '生产用到的原材料、半成品和辅料',
      module: 'mes',
      showInMenu: true,
      sort: 10,
      fields: [
        { name: 'code', displayName: '物料编码', type: 'string', length: 64, unique: true, readonly: true },
        { name: 'name', displayName: '物料名称', type: 'string', length: 100, required: true },
        { name: 'spec', displayName: '规格', type: 'string', length: 100 },
        {
          name: 'category',
          displayName: '类别',
          type: 'enum',
          required: true,
          defaultValue: 'raw',
          options: [
            { value: 'raw', label: '原材料' },
            { value: 'semi', label: '半成品' },
            { value: 'aux', label: '辅料' },
          ],
        },
        { name: 'unit', displayName: '单位', type: 'string', length: 20, defaultValue: '个' },
        { name: 'safety_stock', displayName: '安全库存', type: 'decimal', precision: 18, scale: 2, defaultValue: 0 },
        { name: 'enabled', displayName: '启用', type: 'boolean', defaultValue: true },
        { name: 'remark', displayName: '备注', type: 'text' },
      ],
      services: [
        {
          name: 'fill_code',
          displayName: '自动生成物料编码',
          trigger: 'beforeCreate',
          script: '待改写：保存前如果没有 code，就按名称为 material 的编码规则生成一个。',
        },
      ],
      views: {
        list: {
          columns: ['code', 'name', 'spec', 'category', 'unit', 'safety_stock', 'enabled'],
          searchFields: ['code', 'name', 'category'],
          defaultSort: { field: 'created_at', order: 'desc' },
          pageSize: 10,
        },
        form: {
          groups: [
            {
              title: '基本信息',
              columns: 2,
              fields: ['code', 'name', 'spec', 'category', 'unit', 'safety_stock', 'enabled'],
            },
            { title: '其它', columns: 1, fields: ['remark'] },
          ],
          tabs: [],
        },
      },
    },
    {
      name: 'product',
      displayName: '产品',
      description: '对外销售的成品',
      module: 'mes',
      showInMenu: true,
      sort: 20,
      fields: [
        { name: 'code', displayName: '产品编码', type: 'string', length: 64, unique: true, readonly: true },
        { name: 'name', displayName: '产品名称', type: 'string', length: 100, required: true },
        { name: 'spec', displayName: '规格', type: 'string', length: 100 },
        { name: 'launch_date', displayName: '上市日期', type: 'date' },
        {
          name: 'bom_lines',
          displayName: 'BOM 明细',
          type: 'oneToMany',
          relation: { model: 'product_bom', mappedBy: 'product' },
        },
      ],
      services: [
        {
          name: 'fill_code',
          displayName: '自动生成产品编码',
          trigger: 'beforeCreate',
          script: '待改写：保存前如果没有 code，就按名称为 product 的编码规则生成一个。',
        },
      ],
      views: {
        list: {
          columns: ['code', 'name', 'spec', 'launch_date'],
          searchFields: ['code', 'name'],
          defaultSort: { field: 'created_at', order: 'desc' },
          pageSize: 10,
        },
        form: {
          groups: [{ title: '基本信息', columns: 2, fields: ['code', 'name', 'spec', 'launch_date'] }],
          tabs: [{ title: 'BOM 明细', field: 'bom_lines' }],
        },
      },
    },
    {
      name: 'product_bom',
      displayName: '产品 BOM',
      description: '一个产品由哪些物料组成，各用多少',
      module: 'mes',
      showInMenu: false,
      sort: 30,
      fields: [
        {
          name: 'product',
          displayName: '产品',
          type: 'manyToOne',
          required: true,
          relation: { model: 'product', displayField: 'name' },
        },
        {
          name: 'material',
          displayName: '物料',
          type: 'manyToOne',
          required: true,
          relation: { model: 'material', displayField: 'name' },
        },
        { name: 'quantity', displayName: '数量', type: 'decimal', precision: 18, scale: 4, required: true, defaultValue: 1 },
      ],
      services: [],
      views: {
        list: {
          columns: ['product', 'material', 'quantity'],
          searchFields: [],
          defaultSort: { field: 'created_at', order: 'asc' },
          pageSize: 10,
        },
        form: {
          groups: [{ title: '基本信息', columns: 1, fields: ['product', 'material', 'quantity'] }],
          tabs: [],
        },
      },
    },
    {
      name: 'code_rule',
      displayName: '编码规则',
      description: '按前缀、日期和流水号生成业务编码',
      module: 'base',
      showInMenu: true,
      sort: 10,
      fields: [
        { name: 'name', displayName: '规则名称', type: 'string', length: 64, required: true, unique: true },
        { name: 'prefix', displayName: '前缀', type: 'string', length: 20 },
        {
          name: 'date_format',
          displayName: '日期格式',
          type: 'enum',
          required: true,
          defaultValue: 'yyyyMMdd',
          options: [
            { value: 'none', label: '不带日期' },
            { value: 'yyyyMM', label: '年月（202611）' },
            { value: 'yyyyMMdd', label: '年月日（20261114）' },
          ],
        },
        { name: 'serial_digits', displayName: '流水号位数', type: 'integer', required: true, defaultValue: 4 },
        { name: 'current_serial', displayName: '当前流水号', type: 'integer', readonly: true, defaultValue: 0 },
      ],
      services: [
        {
          name: 'generate',
          displayName: '生成编码',
          trigger: 'manual',
          scope: 'record',
          script: '待改写：流水号加 1，返回 前缀 + 日期 + 补零的流水号。',
        },
      ],
      views: {
        list: {
          columns: ['name', 'prefix', 'date_format', 'serial_digits', 'current_serial'],
          searchFields: ['name'],
          defaultSort: { field: 'created_at', order: 'asc' },
          pageSize: 10,
        },
        form: {
          groups: [
            {
              title: '基本信息',
              columns: 2,
              fields: ['name', 'prefix', 'date_format', 'serial_digits', 'current_serial'],
            },
          ],
          tabs: [],
        },
      },
    },
  ],
}

/** 演示数据。键是模型名；每个模型的记录按顺序从 1 开始编 id，manyToOne 字段里写的就是这个 id */
export const mesSeed: Record<string, DataRecord[]> = {
  material: [
    { code: 'MAT-20261114-0001', name: '螺丝 M3', spec: 'M3×8', category: 'raw', unit: '个', safety_stock: 500, enabled: true, remark: null },
    { code: 'MAT-20261114-0002', name: '直流电机', spec: '12V 380', category: 'semi', unit: '个', safety_stock: 50, enabled: true, remark: null },
    { code: 'MAT-20261114-0003', name: '润滑脂', spec: '100g', category: 'aux', unit: '支', safety_stock: 10, enabled: true, remark: '齿轮箱用' },
    { code: 'MAT-20261114-0004', name: 'ABS 外壳', spec: '黑色', category: 'semi', unit: '套', safety_stock: 30, enabled: false, remark: null },
  ],
  product: [
    { code: 'PRD-20261114-0001', name: '电动螺丝刀', spec: 'ES-12V', launch_date: '2026-09-01' },
    { code: 'PRD-20261114-0002', name: '手持吸尘器', spec: 'VC-01', launch_date: null },
  ],
  product_bom: [
    { product: 1, material: 1, quantity: 12 },
    { product: 1, material: 2, quantity: 1 },
    { product: 1, material: 4, quantity: 1 },
    { product: 2, material: 2, quantity: 1 },
  ],
  code_rule: [
    { name: 'material', prefix: 'MAT-', date_format: 'yyyyMMdd', serial_digits: 4, current_serial: 4 },
    { name: 'product', prefix: 'PRD-', date_format: 'yyyyMMdd', serial_digits: 4, current_serial: 2 },
  ],
}
