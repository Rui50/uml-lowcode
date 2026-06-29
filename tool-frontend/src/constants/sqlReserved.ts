// palavras reservadas comuns em SQL — uma classe que colida gera um nome de tabela
// reservado e pode falhar na criação do schema (ex: classe "Order" -> tabela "order")
export const SQL_RESERVED = new Set<string>([
  'order', 'user', 'group', 'table', 'column', 'select', 'from', 'where', 'index',
  'key', 'primary', 'foreign', 'constraint', 'join', 'inner', 'outer', 'left', 'right',
  'values', 'insert', 'update', 'delete', 'create', 'drop', 'alter', 'transaction',
  'case', 'check', 'default', 'unique', 'references', 'having', 'distinct', 'union',
  'exists', 'between', 'like', 'null', 'true', 'false', 'and', 'or', 'not',
])
