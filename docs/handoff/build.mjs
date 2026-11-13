// 生成发给组员的任务包。
//
// 用法：在仓库根目录执行  node docs/handoff/build.mjs
//
// 它读取本目录下的每个 *.template.md，输出文件名由模板第一行的 <!-- output: 文件名 --> 指定。
// 模板里单独占一行的两种标记会被替换：
//   {{include: 路径}}        把那个文件的内容放进代码块，原样嵌入（给 AI 看的代码和契约用这个）
//   {{embed: 路径#标题}}      把那个 Markdown 文件里某个标题下面的内容直接嵌入（给人看的表格用这个）
//
// 任务包要做到“只发这一个文件就够”，所以契约和现有代码是复制进去的。
// 契约或骨架代码改了以后，重新运行一次，再把新的任务包发给组员。

import { readdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, extname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const root = join(here, '..', '..')

const LANGUAGES = { '.ts': 'ts', '.vue': 'vue', '.json': 'json', '.md': 'markdown' }

function read(path) {
  return readFileSync(join(root, path), 'utf8').replace(/\r\n/g, '\n').trimEnd()
}

function include(path) {
  // 用四个反引号包裹，这样被嵌入的 Markdown 里自带的三个反引号代码块不会把它截断
  const fence = '````'
  return `文件 \`${path}\`：\n\n${fence}${LANGUAGES[extname(path)] ?? ''}\n${read(path)}\n${fence}`
}

function embed(spec) {
  const [path, heading] = spec.split('#')
  const lines = read(path).split('\n')
  if (!heading) {
    return lines.join('\n')
  }
  const start = lines.findIndex((line) => /^#+ /.test(line) && line.replace(/^#+ /, '').trim() === heading)
  if (start < 0) {
    throw new Error(`${path} 里找不到标题“${heading}”`)
  }
  const level = /^#+/.exec(lines[start])[0].length
  let end = lines.length
  for (let i = start + 1; i < lines.length; i++) {
    const match = /^(#+) /.exec(lines[i])
    if (match && match[1].length <= level) {
      end = i
      break
    }
  }
  return lines.slice(start + 1, end).join('\n').trim()
}

for (const name of readdirSync(here).filter((file) => file.endsWith('.template.md'))) {
  const template = readFileSync(join(here, name), 'utf8').replace(/\r\n/g, '\n')
  const output = /^<!-- output: (.+?) -->\n/.exec(template)
  if (!output) {
    throw new Error(`${name} 的第一行要写 <!-- output: 输出文件名 -->`)
  }
  const header = `<!-- 这个文件由 docs/handoff/build.mjs 根据 ${name} 生成。要改内容请改模板，再重新生成。 -->\n`
  const body = template
    .slice(output[0].length)
    .replace(/^\{\{include: (.+?)\}\}$/gm, (_, path) => include(path.trim()))
    .replace(/^\{\{embed: (.+?)\}\}$/gm, (_, spec) => embed(spec.trim()))
  writeFileSync(join(here, output[1]), header + body, 'utf8')
  console.log(`已生成 docs/handoff/${output[1]}（${body.split('\n').length} 行）`)
}
