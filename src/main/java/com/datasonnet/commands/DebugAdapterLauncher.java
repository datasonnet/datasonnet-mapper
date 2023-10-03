package com.datasonnet.commands;

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
import com.datasonnet.debugger.da.DataSonnetDebugAdapterServer;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.jsonrpc.Launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DebugAdapterLauncher {

  private static final Logger logger = LoggerFactory.getLogger(DebugAdapterLauncher.class);

  public static void main(String[] args) {
      DebugAdapterLauncher launcher = new DebugAdapterLauncher();
    try {
      launcher.start();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void start() throws IOException {
    logger.debug("DataSonnet DebugAdapterLauncher starting");
    // Used to debug the Debugger Protocol Adapter
    Path diPath = Files.createTempFile("dap-in", ".log");
    Path doPath = Files.createTempFile("dap-out", ".log");
    logger.debug("loggins stdin to " + diPath + " and stdout to " + doPath);
    FileOutputStream fis = new FileOutputStream(diPath.toFile());
    FileOutputStream fos = new FileOutputStream(doPath.toFile());
    TeeOutputStream teeOutputStream = new TeeOutputStream(System.out, fos);
    TeeInputStream teeInputStream = new TeeInputStream(System.in, fis);

    DataSonnetDebugAdapterServer debugServer = new DataSonnetDebugAdapterServer();
    Launcher<IDebugProtocolClient> serverLauncher = DSPLauncher.createServerLauncher(debugServer, teeInputStream, teeOutputStream);
		IDebugProtocolClient clientProxy = serverLauncher.getRemoteProxy();
		debugServer.connect(clientProxy);

    serverLauncher.startListening();
    logger.debug("DataSonnet DebugAdapterLauncher startListening DONE");
  }
}
