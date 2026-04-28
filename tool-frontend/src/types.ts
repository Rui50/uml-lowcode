
///////////////////////////////
// Class stuff
///////////////////////////////

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

////////////////////////
// Sequence diagram
////////////////////////

export interface MessageArgumentDTO {
  name: string
  value: string
  type: string
}

export interface LifelineDTO {
  id: string
  name: string
  represents: string
  type: string
}

export interface MessageDTO {
  id: string
  name: string
  rawName: string
  messageSort: string
  returnType: string | null
  assignVariable: string | null
  sourceLifelineId: string | null
  targetLifelineId: string | null
  sequenceNumber: number
  vpNumber: string | null
  arguments: MessageArgumentDTO[]
}

export interface InteractionDTO {
  id: string
  name: string
  contextClass: string
  lifelines: LifelineDTO[]
  messages: MessageDTO[]
}

///////////
// parsing reply
///////////
export interface ParseResponse {
  classes: ClassDTO[]
  associations: AssociationDTO[]
  interactions: InteractionDTO[]
}