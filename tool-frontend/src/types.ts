export interface AttributeDTO {
  name: string
  type: string
}

export interface ClassDTO {
  name: string
  parent: string | null
  attributes: AttributeDTO[]
}

export interface AssociationDTO {
  source: string
  target: string
  sourceMultiplicity: string
  targetMultiplicity: string
  sourceRole: string
  targetRole: string
}

export interface ParseResponse {
  classes: ClassDTO[]
  associations: AssociationDTO[]
}