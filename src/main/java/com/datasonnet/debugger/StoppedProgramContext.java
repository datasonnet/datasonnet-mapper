package com.datasonnet.debugger;
/*-
 * Copyright 2019-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Map;

/**
 * Holds the context of a stopped datasonnet program in a format that is independent of what the DataSonnet
 * evaluator uses ( Val scala classes etc. ) and what Dap uses ( Variable, etc. ).
 *
 * This is WIP while we figure out how to share information with the debugger
 */
public class StoppedProgramContext {

  private SourcePos sourcePos;

  private Map<String, Object> namedVariables;

  public void setSourcePos(SourcePos sourcePos) {
    this.sourcePos = sourcePos;
  }

  public SourcePos getSourcePos() {
    return this.sourcePos;
  }

  public void setNamedVariables(Map<String, Object> namedVariables) {
    this.namedVariables = namedVariables;
  }

  public Map<String, Object> getNamedVariables() {
    return this.namedVariables;
  }
}
