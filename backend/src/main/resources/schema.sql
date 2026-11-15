-- 元模型自己的两张表。每次启动都会执行，所以都用 IF NOT EXISTS。
-- 业务模型（物料、产品等）的表不在这里，它们是保存模型时由程序动态建出来的。
-- 为什么模型定义存成一整段 JSON，见 docs/decisions/001-元模型定义怎么存.md。

CREATE TABLE IF NOT EXISTS meta_module (
  name         VARCHAR(30)  NOT NULL PRIMARY KEY,
  display_name VARCHAR(100) NOT NULL,
  icon         VARCHAR(50),
  sort         INT          NOT NULL DEFAULT 100
);

CREATE TABLE IF NOT EXISTS meta_model (
  name        VARCHAR(30) NOT NULL PRIMARY KEY,
  module_name VARCHAR(30) NOT NULL,
  definition  MEDIUMTEXT  NOT NULL,
  created_at  DATETIME    NOT NULL,
  updated_at  DATETIME    NOT NULL
);
