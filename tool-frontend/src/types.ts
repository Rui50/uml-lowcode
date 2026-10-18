
///////////////////////////////
// Class stuff
///////////////////////////////

export interface AttributeDTO {
  name: string
  type: string
}

export interface Endpoint {
  httpMethod: string                              // GET, POST, PUT, PATCH, DELETE
  path: string                                    // /api/alunos/{id}/turmaa
  type: 'CRUD' | 'ASSOCIATION' | 'CUSTOM'
  label: string
  dtoType: 'CREATE' | 'UPDATE' | 'RESPONSE' | null
}

export interface RelationField {
  name: string        // campo/role da relação (igual ao dos DTOs gerados)
  collection: boolean // sem toggle no Update (não está no UpdateDTO)
  targetClass: string // classe do outro lado (p/ listar atributos no order-by)
  mandatory: boolean  // single com lower bound >= 1 -> semeia invariante <> null
}

export interface ClassDTO {
  id: string  // xmi:id
  name: string
  parent: string | null
  attributes: AttributeDTO[]
  operations: OperationDTO[]
  abstract: boolean
  realizations: string[]
  endpoints: Endpoint[]
  relations: RelationField[]
}

export interface InterfaceDTO {
  id: string  // xmi:id
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
  abstract: boolean
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

export interface ResponseDTOoverride {
  name: string // vazio nome "automatico"
  hidden: string[]
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
  timestamps?: boolean
  demoFrontend?: boolean
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
  warnings: string[]   // avisos do parse
}
