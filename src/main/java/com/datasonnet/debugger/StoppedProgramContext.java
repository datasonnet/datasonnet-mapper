package com.datasonnet.debugger;

import java.util.Map;

/**
 * Holds the context of a stopped datasonnet program in a format that is independent of what the DataSonnet
 * evaluator uses ( Val scala classes etc. ) and what Dap uses ( Variable, etc. ).
 *
 * This is WIP while we figure out how to share information with the debugger
 */
public class StoppedProgramContext {

  private SourcePos sourcePos;

  private Map<String, String> namedVariables;

  public void setSourcePos(SourcePos sourcePos) {
    this.sourcePos = sourcePos;
  }

  public SourcePos getSourcePos() {
    return this.sourcePos;
  }

  public void setNamedVariables(Map<String, String> namedVariables) {
    this.namedVariables = namedVariables;
  }

  public Map<String, String> getNamedVariables() {
    return this.namedVariables;
  }
}
