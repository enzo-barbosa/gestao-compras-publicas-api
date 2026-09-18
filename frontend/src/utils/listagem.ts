export function paramsListagem(sort?: string): Record<string, string | number> {
  return sort ? { size: 100, sort } : { size: 100 }
}
