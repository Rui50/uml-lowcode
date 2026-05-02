// will do same as backend for assoc name
export function associationName(
  className: string,
  multiplicity: string | null,
  roleName: string | null,
) : string {
  if (roleName && roleName.trim() !== '') return roleName.trim()
  const base = className.charAt(0).toLowerCase() + className.slice(1)
  return isCollection(multiplicity) ? base + 's' : base
}

 function isCollection(multiplicity: string | null): boolean {
  if (!multiplicity) return false
  return multiplicity.includes('*') || (multiplicity.includes('..') && multiplicity !== '0..1')
}