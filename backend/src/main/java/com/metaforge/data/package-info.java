/**
 * 通用数据接口：/api/data/{model} 的增删改查。所有业务数据的 SQL 都在这里按元模型拼出来。
 *
 * 类各管一件事：
 * - DataController：路由，和契约里的数据接口一一对应；
 * - DataService：流程（钩子 → 校验 → 写库 → 读回）、事务、错误翻译；
 * - SqlBuilder：只拼 SQL 字符串和参数表，不执行，表名列名的白名单规则在这里（决策 004）；
 * - FieldValues：值在 JSON 和数据库之间转换，具体交给 schema 包里的字段类型处理器；
 * - SearchRequest / SearchResult：查询接口的请求和响应形状。
 *
 * 对应任务 B3 和 B4，见 docs/tasks/backend.md。
 */
package com.metaforge.data;
