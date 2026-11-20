// 决策 006 试做：Groovy 4.0.33，生成编码
// 上下文：db 是受控的数据访问对象；params.ruleId 是编码规则 id。
// 试做输出：MAT-202611200001、MAT-202611200002（两次调用，流水号连续）。
def rule = db.get('code_rule', params.ruleId)
def serial = db.increment('code_rule', rule.id, 'current_serial')
def digits = serial.toString()
while (digits.length() < rule.serial_digits) {
    digits = '0' + digits
}
return rule.prefix + db.today(rule.date_format) + digits
