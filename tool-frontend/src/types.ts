
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
  operations: OperationDTO[]
  isAbstract: boolean
  realizations: string[]
}

export interface InterfaceDTO {
  name: string
  operations: OperationDTO[]
}

export interface EnumerationDTO {
  name: string
  literals: string[]
}

export interface AssociationDTO {
  source: string
  target: string
  sourceMultiplicity: string
  targetMultiplicity: string
  sourceRole: string
  targetRole: string
  sourceNavigable: boolean
  targetNavigable: boolean
}

export interface ParameterDTO { 
  name: string; type: string 
}

export interface OperationDTO {
  id: string
  name: string
  returnType: string
  visibility: string
  parameters: ParameterDTO[]
}

////////////////////////
// Sequence diagram
////////////////////////
export interface LifelineDTO {
  id: string
  name: string
  represents: string
  type: string | null
}

export interface MessageDTO {
  type: 'message'
  id: string
  label: string              // = rawName na backend
  messageSort: string        // synchCall
  sourceLifelineId: string | null
  targetLifelineId: string | null
}

export interface OperandDTO {
  guard: string | null
  elements: SequenceElement[]
}

export interface FragmentDTO {
  type: 'fragment'
  operator: string           // loop etc
  operands: OperandDTO[]
}

export type SequenceElement = MessageDTO | FragmentDTO

export interface InteractionDTO {
  id: string
  name: string
  contextClass: string
  lifelines: LifelineDTO[]
  elements: SequenceElement[]
}
//////////
// Annotations
//////////

export interface Invariant {
  id: string
  class: string
  expression: string
}

export interface Init { 
  id: string;
  attribute: string; 
  expression: string 
}

export interface Derived {
  id: string;
  attribute: string;
  expression: string;
}

export interface ClassAnnotations {
  invariants: Invariant[]
  inits: Init[]
  derived: Derived[]
}




//////////
// Project settings
//////////

export interface ProjectSettings {
  basePackage?: string
  applicationName?: string
  databaseType?: 'H2' | 'POSTGRESQL' | 'MYSQL'
  serverPort?: number
  openApi?: boolean
  dockerfile?: boolean
}

///////////
// parsing reply
///////////
export interface ParseResponse {
  classes: ClassDTO[]
  associations: AssociationDTO[]
  interactions: InteractionDTO[]
  enumerations: EnumerationDTO[]
  interfaces: InterfaceDTO[]
}