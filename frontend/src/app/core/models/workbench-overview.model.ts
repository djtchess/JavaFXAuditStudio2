export interface RefactoringLot {
  number: number;
  title: string;
  objective: string;
  primaryOutcome: string;
}

export interface AgentOverview {
  id: string;
  label: string;
  responsibility: string;
  preferredModel: string;
}

export interface WorkbenchOverview {
  productName: string;
  summary: string;
  frontendTarget: string;
  backendTarget: string;
  lots: RefactoringLot[];
  agents: AgentOverview[];
}
