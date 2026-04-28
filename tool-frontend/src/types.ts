export interface AttributeDto {
  name: string
  type: string
}

export interface ClassDto {
  name: string
  parent: string | null
  attributes: AttributeDto[]
}

export interface AssociationDto {
  source: string
  target: string
  sourceMultiplicity: string
  targetMultiplicity: string
  sourceRole: string
  targetRole: string
}

export interface ParseResponse {
  classes: ClassDto[]
  associations: AssociationDto[]
}