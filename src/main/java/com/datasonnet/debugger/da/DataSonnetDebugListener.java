package com.datasonnet.debugger.da;

import com.datasonnet.debugger.StoppedProgramContext;

public interface DataSonnetDebugListener {
  void stopped(StoppedProgramContext stoppedProgramContext);
}
