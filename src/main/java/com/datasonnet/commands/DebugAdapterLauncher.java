package com.datasonnet.commands;

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
