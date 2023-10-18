package com.datasonnet.debugger.da;

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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.datasonnet.Mapper;
import com.datasonnet.debugger.DataSonnetDebugger;
import com.datasonnet.debugger.SourcePos;
import com.datasonnet.debugger.StoppedProgramContext;
import com.datasonnet.debugger.ValueInfo;
import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.MediaTypes;
import org.eclipse.lsp4j.debug.Breakpoint;
import org.eclipse.lsp4j.debug.Capabilities;
import org.eclipse.lsp4j.debug.ConfigurationDoneArguments;
import org.eclipse.lsp4j.debug.ContinueArguments;
import org.eclipse.lsp4j.debug.ContinueResponse;
import org.eclipse.lsp4j.debug.DisconnectArguments;
import org.eclipse.lsp4j.debug.ExitedEventArguments;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.eclipse.lsp4j.debug.NextArguments;
import org.eclipse.lsp4j.debug.OutputEventArguments;
import org.eclipse.lsp4j.debug.Scope;
import org.eclipse.lsp4j.debug.ScopesArguments;
import org.eclipse.lsp4j.debug.ScopesResponse;
import org.eclipse.lsp4j.debug.SetBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetBreakpointsResponse;
import org.eclipse.lsp4j.debug.SetVariableArguments;
import org.eclipse.lsp4j.debug.SetVariableResponse;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.SourcePresentationHint;
import org.eclipse.lsp4j.debug.StackFrame;
import org.eclipse.lsp4j.debug.StackFramePresentationHint;
import org.eclipse.lsp4j.debug.StackTraceArguments;
import org.eclipse.lsp4j.debug.StackTraceResponse;
import org.eclipse.lsp4j.debug.StepInArguments;
import org.eclipse.lsp4j.debug.StepOutArguments;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.StoppedEventArgumentsReason;
import org.eclipse.lsp4j.debug.TerminateArguments;
import org.eclipse.lsp4j.debug.TerminatedEventArguments;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.ThreadsResponse;
import org.eclipse.lsp4j.debug.Variable;
import org.eclipse.lsp4j.debug.VariablePresentationHint;
import org.eclipse.lsp4j.debug.VariablesArguments;
import org.eclipse.lsp4j.debug.VariablesResponse;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the DebuggerAdapterProtocol used by VS Code, Eclipse and other tools.
 *
 * It works with an instance of a DataSonnetDebugger ( currently a Singleton ) to implement the debugging actions and
 * to receive debugging events
 *
 */
public class DataSonnetDebugAdapterServer implements IDebugProtocolServer, DataSonnetDebugListener {

  private static final Logger logger = LoggerFactory.getLogger(DataSonnetDebugAdapterServer.class);

  /**
   * DataSonnet evaluator runs on one thread, so we need just one id
   */
  private static final int DATASONNET_THREAD_ID = 0;

  /**
   * Refs variables ( self, super, dollar ) id
   */
  public static final int REF_VARIABLES_REFERENCE_ID = 256;

  /**
   * Id for "bindings" variables: locals, function params.
   */
  public static final int BINDINGS_VARIABLES_REFERENCE_ID = 257;

  /**
   * Id for "external" variables: passed as params to the Mapper
   */
  public static final int EXT_VARIABLES_REFERENCE_ID = 258;

  /**
   * Id used for self's properties
   */
  public static final int SELF_VAR_REF = 260;

  /**
   * Id used for super's properties
   */
  public static final int SUPER_VAR_REF = 261;

  /**
   * Id used for dollar's properties
   */
  public static final int DOLLAR_VAR_REF = 262;

  /**
   * Holds the (bound) variables keyed by their id. Note that the map gets reset after each step
   */
  private Map<Integer, Variable> variablesMap = new HashMap<>();

  /**
   * To be used for "bindings" variables, to keep track of the ids user for each bound variable. Used as a key for
   * variablesMap
   */
  private AtomicInteger variablesKeyGenerator = new AtomicInteger();

  /**
   * DAP client
   */
  private volatile IDebugProtocolClient client;

  /**
   * FIXME map from breakpoints to our source code file(s)
   */
  private final Map<String, Set<String>> sourceToBreakpointIds = new ConcurrentHashMap<>();

  /**
   * To be removed before release.
   * A thread that sends fake "stopped" events; used to debug the protocol
   */
  private java.lang.Thread fakerThread;

  /**
   * Thread that runs the Mapper
   */
  private java.lang.Thread mapperThread;

  /**
   * Mapper that runs the script being debugged
   */
  private Mapper mapper;

  /**
   * The full path to the script ( program ) run by the Mapper
   * FIXME: Move this to a Context object
   */
  private String program;

  /**
   * The file name of the script ( program ) run by the Mapper
   * FIXME: Move this to a Context object
   */
  private String programBaseName;

  /**
   * The text of the script ( program ) run by the Mapper
   * FIXME: Move this to a Context object
   */
  private String script;

  /**
   * Holds the result of the program once it finishes
   */
  private String resultVariable;

  /**
   * Save the proxy to be used to send events to the client
   *
   * @param clientProxy
   */
  public void connect(IDebugProtocolClient clientProxy) {
    this.client = clientProxy;
  }

  /**
   * Initialize the receiver. The args hold the client capabilities
   * This is the place to adapt the receiver to those capabilities
   *
   * https://microsoft.github.io/debug-adapter-protocol/specification#Requests_Initialize
   *
   * @param args
   * @return the receiver capabilities
   */
  @Override
  public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {
    logger.info("initialize. args:" + args);
    return supplyAsync(
        () -> {
// From VS code we get:
//{
//    "command": "initialize",
//    "arguments": {
//        "clientID": "vscode",
//        "clientName": "Visual Studio Code - Insiders",
//        "adapterID": "datasonnet.debugger",
//        "pathFormat": "path",
//        "linesStartAt1": true,
//        "columnsStartAt1": true,
//        "supportsVariableType": true,
//        "supportsVariablePaging": true,
//        "supportsRunInTerminalRequest": true,
//        "locale": "en",
//        "supportsProgressReporting": true,
//        "supportsInvalidatedEvent": true,
//        "supportsMemoryReferences": true,
//        "supportsArgsCanBeInterpretedByShell": true,
//        "supportsMemoryEvent": true,
//        "supportsStartDebuggingRequest": true
//    },
//    "type": "request",
//    "seq": 1
//}

          // FIXME we're assuming columns start at 1; should save the value from the args

          Capabilities capabilities = new Capabilities();
          // Disabled until we found out how to set a variable on the Evaluator Scope
          capabilities.setSupportsSetVariable(Boolean.FALSE);
          // For now only fixed breakpoints are supported
          capabilities.setSupportsConditionalBreakpoints(Boolean.FALSE);
          capabilities.setSupportsFunctionBreakpoints(Boolean.FALSE);

          capabilities.setSupportsExceptionOptions(Boolean.FALSE);
          capabilities.setSupportsExceptionInfoRequest(Boolean.FALSE);
          capabilities.setSupportsExceptionFilterOptions(Boolean.FALSE);

          // We need this to start the debugee ( Mapper instance )
          capabilities.setSupportsConfigurationDoneRequest(Boolean.TRUE);
          // not yet implemented
          capabilities.setSupportsRestartRequest(Boolean.FALSE);
          // not yet implemented
          capabilities.setSupportSuspendDebuggee(Boolean.FALSE);
          // The debug adapter supports a `format` attribute on the `stackTrace`,
          // `variables`, and `evaluate` requests.
          // Not yet implemented because we don't support the format?: StackFrameFormat parameter on requests
          capabilities.setSupportsValueFormattingOptions(Boolean.FALSE);
          // Debuggee launched: if a debug adapter supports the terminate request, the development tool uses it to
          // terminate the debuggee gracefully, i.e. it gives the debuggee a chance to cleanup everything before terminating.
          capabilities.setSupportTerminateDebuggee(true);
          // The debug adapter supports the `singleThread` property on the execution
          // requests (`continue`, `next`, `stepIn`, `stepOut`, `reverseContinue`,
          // `stepBack`).
          // false because we only support one thread
          capabilities.setSupportsSingleThreadExecutionRequests(Boolean.FALSE);
          // Cancel not supported
          capabilities.setSupportsCancelRequest(Boolean.FALSE);

          logger.info("Returning capabilities..." + capabilities);
          return capabilities;
        }
    );
  }

  /**
   * From https://microsoft.github.io/debug-adapter-protocol/overview; Launching and attaching
   * request: the debug adapter connects to an already running program ( this ).
   * Here the end user is responsible for launching and terminating the program.
   *
   * This is not supported; only "launch"
   *
   * @param args
   * @return
   */
  @Override
  public CompletableFuture<Void> attach(Map<String, Object> args) {
    return runAsync(
        () -> {
          logger.info("attach; args:" + args);
          OutputEventArguments oea = new OutputEventArguments();
          oea.setOutput("attach request not supported");
          client.output(oea);
        }
    );
  }

  /**
   * With the "launch" command the debug adapter launches the program (“debuggee”) in debug mode and then starts
   * to communicate with it.
   * Since the debug adapter is responsible for launching the debuggee, it should provide a mechanism for the
   * end user to configure the debuggee. For example, passing arguments or specifying a working directory.
   *
   * https://microsoft.github.io/debug-adapter-protocol/specification#Requests_Launch
   * @param args
   * @return
   */
  public CompletableFuture<Void> launch(Map<String, Object> args) {
    logger.info("launch; args:" + args);

		/* typical request
{
    "command": "launch",
    "arguments": {
        "type": "datasonnet.debugger",
        "request": "launch",
        "name": "Launch Datasonnet Debugger",
        "program": "/home/dev/code/sampleWorkspace/stg-get.ds",
        "file": "/home/dev/code/sampleWorkspace/stg-get.ds",
        "fileBasename": "stg-get.ds",
        "folder": "/home/dev/code/sampleWorkspace",
        "relativeFile": "stg-get.ds",
        "fileDirName": "/home/dev/code/sampleWorkspace",
        "cwd": "/home/dev/code/sampleWorkspace",
        "lineNumber": "12",
        "pathSeparator": "/",
        "stopOnEntry": true,
        "trace": true,
        "__configurationTarget": 6,
        "__sessionId": "a2efb8e3-af7f-4184-b64b-bcf71e507177"
    },
    "type": "request",
    "seq": 2
}
		*/

    return runAsync(
        () -> {
          try {
            this.program = (String) args.get("program");
            this.programBaseName = (String) args.get("fileBasename");
            this.script = readFileAsString(program);
            logger.info("Running mapper for script: " + this.script);
            mapper = new Mapper(this.script);
            DataSonnetDebugger debugger = DataSonnetDebugger.getDebugger();
            debugger.setStepMode(true);
            // don't start it yet!

            boolean launched = true;
            // Since we're ready to receive breakpoints and other config, we're INITIALIZED
            // Expect:
            //
            //setBreakpoints one request for all breakpoints in a single source,
            //setFunctionBreakpoints if the debug adapter supports function breakpoints,
            //setExceptionBreakpoints if the debug adapter supports any exception options,
            //configurationDoneRequest to indicate the end of the configuration sequence.
            if (launched) {
              java.lang.Thread initializedThread = new java.lang.Thread((Runnable) this::initializedCallback, "initialized callback");
              initializedThread.start();
            }
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          }
        }
    );
  }

  private void initializedCallback() {
    client.initialized();
  }

  /**
   * DatasonnetDebugger callback
   *
   * @param stoppedProgramContext
   */
  @Override
  public void stopped(StoppedProgramContext stoppedProgramContext) {
      logger.info("DatasonnetDebugger callback. stoppedProgramContext: " + stoppedProgramContext);
      this.resetVariablesCache();
      StoppedEventArguments args = new StoppedEventArguments();

//     * The reason for the event.
//     * For backward compatibility this string is shown in the UI if the
//     * `description` attribute is missing (but it must not be translated).
//      args.setReason(StoppedEventArgumentsReason.ENTRY);
//      args.setReason(StoppedEventArgumentsReason.GOTO);
//      args.setReason(StoppedEventArgumentsReason.EXCEPTION);
//      args.setReason(StoppedEventArgumentsReason.FUNCTION_BREAKPOINT);
      args.setReason(StoppedEventArgumentsReason.STEP);
//      args.setReason(StoppedEventArgumentsReason.BREAKPOINT);

//     * A value of true hints to the client that this event should not change the
//     * focus.
      args.setPreserveFocusHint(false);

//     * Additional information. E.g. if reason is `exception`, text contains the
//     * exception name. This string is shown in the UI.
      args.setText("step");

//     * If `allThreadsStopped` is true, a debug adapter can announce that all
//     * threads have stopped.
//     * - The client should use this information to enable that all threads can
//     * be expanded to access their stacktraces.
//     * - If the attribute is missing or false, only the thread with the given
//     * `threadId` can be expanded.
      args.setAllThreadsStopped(true);

//     * The full reason for the event, e.g. 'Paused on exception'. This string is
//     * shown in the UI as is and can be translated.
      args.setDescription("step");

//    Since we can't send the Source here, save it so that we can return it later with the threads-> calls
      //Ids of the breakpoints that triggered the event. In most cases there is
      //only a single breakpoint but here are some examples for multiple
      //breakpoints:
      //- Different types of breakpoints map to the same location.
      //- Multiple source breakpoints get collapsed to the same instruction by
      //the compiler/runtime.
      //- Multiple function breakpoints with different function names map to the
      //same location.
//      args.setHitBreakpointIds(new Integer[]{0});

//     * The thread which was stopped.
      args.setThreadId(DATASONNET_THREAD_ID);

      client.stopped(args);
  }

  private void resetVariablesCache() {
    this.variablesMap.clear();
    this.variablesKeyGenerator = new AtomicInteger();
  }


  /**
   * Save the breakpoints and map them to the current script being debugged
   * FIXME actually save them
   * The setBreakpoints request registers all breakpoints that exist for a single source (so it is not incremental).
   * A simple implementation of these semantics in the debug adapter is to clear all previous breakpoints for the
   * source and then set the breakpoints specified in the request. setBreakpoints and setFunctionBreakpoints are
   * expected to return the ‘actual’ breakpoints and the generic debugger updates the UI dynamically if a breakpoint
   * could not be set at the requested position or was moved by the debugger.
   *
   * @param setBreakpointsArguments
   * @return
   */
  @Override
  public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments setBreakpointsArguments) {
    logger.info("setBreakpoints");
    return supplyAsync(() -> setBreakpointsSync(setBreakpointsArguments));
  }

  private SetBreakpointsResponse setBreakpointsSync(SetBreakpointsArguments setBreakpointsArguments) {
    SetBreakpointsResponse response = new SetBreakpointsResponse();
    Breakpoint[] breakpoints = new Breakpoint[1];
    Breakpoint bp = new Breakpoint();
    bp.setId(2);
    bp.setLine(9);
//		bp.setMessage("this is a breakpoint");
    bp.setVerified(true);
    Source src = new Source();
    src.setName("XXX");
    src.setPath("XXXX program?");
    bp.setSource(src);
    breakpoints[0] = bp;
    response.setBreakpoints(breakpoints);
    return response;

		/*
		We need to save the breakpoints
    and share with the DataSonnetDebugger

		Source source = setBreakpointsArguments.getSource();
		SourceBreakpoint[] sourceBreakpoints = setBreakpointsArguments.getBreakpoints();
		Breakpoint[] breakpoints = new Breakpoint[sourceBreakpoints.length];
		Set<String> breakpointIds = new HashSet<>();
		for (int i = 0; i< sourceBreakpoints.length; i++) {
			SourceBreakpoint sourceBreakpoint = sourceBreakpoints[i];
			int line = sourceBreakpoint.getLine();
			// Create DataSonnetBreakpoint

						breakpoint.setVerified(true);
					} else {
						breakpoint.setMessage(String.format(BREAKPOINT_MESSAGE_CANNOT_FIND_ID, source.getPath(), line));
					}
					breakpoint.setMessage(String.format(BREAKPOINT_MESSAGE_EXCEPTION_OCCURED_WHEN_SEARCHING_ID, baseMessage, e.getMessage()));

				String message = String.format(MESSAGE_NO_ACTIVE_ROUTES_FOUND, source.getPath(), line);
//		this.removeOldBreakpoints(source, breakpointIds);
		sourceToBreakpointIds.put(source.getPath(), breakpointIds);
//		SetBreakpointsResponse response = new SetBreakpointsResponse();
		response.setBreakpoints(breakpoints);
		return response;

		 */
  }

//	private void addBreakpoint(SourceBreakpoint sourceBreakpoint, String nodeId) {
////	}
////
////	private void removeOldBreakpoints(Source source, Set<String> breakpointIds) {
//	}

  //setFunctionBreakpoints if the debug adapter supports function breakpoints,

  //setExceptionBreakpoints if the debug adapter supports any exception options,


  /**
   * This request indicates that the client has finished initialization of the debug adapter.
   * So it is the last request in the sequence of configuration requests (which was started by the initialized event).
   *
   * @param args
   * @return
   */
  @Override
  public CompletableFuture<Void> configurationDone(ConfigurationDoneArguments args) {
    return runAsync(
        () -> {
          DataSonnetDebugger.getDebugger().attach();
          DataSonnetDebugger.getDebugger().setDebuggerAdapter(this);

//					// FIXME we should have a document to transform here as an extra param to the launch config ( the payload )
					String jsonData = "{}";
          // The transformation is run on a Thread
          mapperThread = new java.lang.Thread((Runnable) () -> {
            logger.info("running mapper.transform.");
            String mappedJson = mapper.transform(new DefaultDocument<String>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();
            logger.info("mappedJson: " + mappedJson);
            this.resultVariable = mappedJson;
            // If we got here the mapper finished its job
            this.finished();

            // FIXME We could send an output event from the debugger to show the results
            // FIXME eventually send these two? Currently we're not doing this to avoid terminating the session for a
            // client who may be looking at the result
//            this.terminated();
//            this.exited(0);

            // FIXME catch exceptions and send a breakpoint to the client;
          }, "DataSonnet Thread");
          mapperThread.start();
        }
    );
  }

  /**
   * Sends a finished event to the client
   */
  private void finished() {
    StoppedEventArguments args = new StoppedEventArguments();
    args.setReason("finished");
    args.setDescription("finished");
    args.setThreadId(DATASONNET_THREAD_ID);
    args.setText("Program finished OK");
    this.client.stopped(args);
  }

  /**
   * Sends a terminated event to the client
   */
  private void terminated() {
    TerminatedEventArguments args = new TerminatedEventArguments();
    args.setRestart(false);
    this.client.terminated(args);
  }

  /**
   * Sends a exited event to the client
   */
  private void exited(int i) {
    ExitedEventArguments args = new ExitedEventArguments();
    args.setExitCode(i);
    this.client.exited(args);
  }


  /**
   * After configurationDone is received the client asks for the threads
   *
   * @return
   */
  @Override
  public CompletableFuture<ThreadsResponse> threads() {
    logger.info("threads");
    return supplyAsync(
        () -> {
          ThreadsResponse value = new ThreadsResponse();
          Thread t1 = new Thread();
          t1.setId(DATASONNET_THREAD_ID);
          t1.setName("Datasonnet main thread");
          value.setThreads(new Thread[]{t1});
          return value;
        }
    );
  }

  /**
   * After the threads(), the client requests the stacktrace
   * (a list of stack frames) for the thread mentioned in the stopped event.
   *
   * https://microsoft.github.io/debug-adapter-protocol/specification#Types_StackFrame
   * @param args
   * @return
   */
  @Override
  public CompletableFuture<StackTraceResponse> stackTrace(StackTraceArguments args) {
    logger.info("stackTrace: " + args.toString());
    // {"command":"stackTrace","arguments":{"threadId":0,"startFrame":0,"levels":20},"type":"request","seq":7}
    return supplyAsync(
        () -> {
          StackTraceResponse response = new StackTraceResponse();
          StackFrame sf0 = new StackFrame();
//   * An identifier for the stack frame. It must be unique across all threads. <---- !!! We only have one
//   * This id can be used to retrieve the scopes of the frame with the `scopes`
//   * request or to restart the execution of a stack frame.
          sf0.setId(0);
          sf0.setSource(this.getCurrentSource());
          if ( this.getStoppedAtSourcePos() != null ) {

//   * The line within the source of the frame. If the source attribute is missing
//   * or doesn't exist, `line` is 0 and should be ignored by the client.
            sf0.setLine(this.getStoppedAtSourcePos().getLine());

//   * Start position of the range covered by the stack frame. It is measured in
//   * UTF-16 code units and the client capability `columnsStartAt1` determines
//   * whether it is 0- or 1-based. If attribute `source` is missing or doesn't
//   * exist, `column` is 0 and should be ignored by the client.
            sf0.setColumn(this.getStoppedAtSourcePos().getCaretPosInLine());

//   * The end line of the range covered by the stack frame.
            sf0.setEndLine(this.getStoppedAtSourcePos().getLine());

//   * End position of the range covered by the stack frame. It is measured in
//   * UTF-16 code units and the client capability `columnsStartAt1` determines
//   * whether it is 0- or 1-based.
//  endColumn?: number;
          }
          //   * The name of the stack frame, typically a method name.
          sf0.setName("mapper");

  /**
   * Indicates whether this frame can be restarted with the `restart` request.
   * Clients should only use this if the debug adapter supports the `restart`
   * request and the corresponding capability `supportsRestartRequest` is true.
   * If a debug adapter has this capability, then `canRestart` defaults to
   * `true` if the property is absent.
   */
          sf0.setCanRestart(false);

//   * A hint for how to present this frame in the UI.
//   * A value of `label` can be used to indicate that the frame is an artificial
//   * frame that is used as a visual label or separator. A value of `subtle` can
//   * be used to change the appearance of a frame in a 'subtle' way.
//   * Values: 'normal', 'label', 'subtle'
//   */
//  presentationHint?: 'normal' | 'label' | 'subtle';
          sf0.setPresentationHint(StackFramePresentationHint.NORMAL);

//   * The source of the frame.
        // FIXME note that this could be an import or other source
          sf0.setSource(this.getCurrentSource());

          response.setStackFrames(new StackFrame[]{sf0});
          return response;
        }
    );
  }

  private SourcePos getStoppedAtSourcePos() {
    return DataSonnetDebugger.getDebugger().getStoppedProgramContext().getSourcePos();
  }

  /**
   * A Source is a descriptor for source code.
   *
   * It is returned from the debug adapter as part of a StackFrame and it is used by clients when specifying breakpoints.
   */
  private Source getCurrentSource() {
    Source src = new Source();
    src.setName(this.programBaseName);

//   * The path of the source to be shown in the UI.
//   * It is only used to locate and load the content of the source if no
//   * `sourceReference` is specified (or its value is 0).
    src.setPath(this.program);

    // FIXME To be used to provide a source reference to a library source, which maybe won't be available on the client
//   * If the value > 0 the contents of the source must be retrieved through the
//   * `source` request (even if a path is specified).
//   * Since a `sourceReference` is only valid for a session, it can not be used
//   * to persist a source.
//   * The value should be less than or equal to 2147483647 (2^31-1).
    src.setSourceReference(0);

    // FIXME for libraries this should be a different value
//   * A hint for how to present the source in the UI.
//   * A value of `deemphasize` can be used to indicate that the source is not
//   * available or that it is skipped on stepping.
//   * Values: 'normal', 'emphasize', 'deemphasize'
    src.setPresentationHint(SourcePresentationHint.NORMAL);

    // FIXME for libraries this should be a different value
//   * The origin of this source. For example, 'internal module', 'inlined content
//   * from source map', etc.
    src.setOrigin("client");

//   * A list of sources that are related to this source. These may be the source
//   * that generated this source.
//    src.setSources();

//   * Additional data that a debug adapter might want to loop through the client.
//   * The client should leave the data intact and persist it across sessions. The
//   * client should not interpret the data.
//      src.setAdapterData();

    return src;
  }

  /**
   * A Scope is a named container for variables. Optionally a scope can map to a source or a range within a source.
   * https://microsoft.github.io/debug-adapter-protocol/specification#Types_Scope
   *
   * If the user then drills into the stack frame, the development tool first requests the scopes for a stack frame
   * @param args
   * @return
   */
  @Override
  public CompletableFuture<ScopesResponse> scopes(ScopesArguments args) {
    logger.info("scopes");
    // {"command":"scopes","arguments":{"frameId":0},"type":"request","seq":8}
    return supplyAsync(
        () -> {
          // Create scopes for the datasonnet runtime
          ScopesResponse response = new ScopesResponse();

          // First scope: refs
          Scope refsScope = this.getRefsScope();
          // FIXME Second scope: bindings ( local vars )
          //          Scope bindingsScope = this.getBindingsScope();

          // FIXME third: evalScope
          // The evaluator is also an EvalScope, context that is propagated throughout the Jsonnet evaluation.
          // it has extVars()
          // something else?

          // FIXME fourth: result

//          Scope extVarsScope = this.getExtVarsScope();
          response.setScopes(new Scope[]{refsScope});
          return response;
        }
    );
  }

  @NotNull
  private Scope getRefsScope() {
    Scope refsScope = new Scope();
//   * Name of the scope such as 'Arguments', 'Locals', or 'Registers'. This
//   * string is shown in the UI as is and can be translated.
    refsScope.setName("refs");

//   * The variables of this scope can be retrieved by passing the value of
//   * `variablesReference` to the `variables` request as long as execution
//   * remains suspended. See 'Lifetime of Object References' in the Overview
//   * section for details.
    // So, it's a reference, not the sum of other variables. If we send 0 then we don't get the variables request,
    // so we send a magic number
    refsScope.setVariablesReference(REF_VARIABLES_REFERENCE_ID);

//   * The number of named variables in this scope.
//   * The client can use this information to present the variables in a paged UI
//   * and fetch them in chunks.
    // self, super, $
    refsScope.setNamedVariables(3);

//   * The number of indexed variables in this scope.
//   * The client can use this information to present the variables in a paged UI
//   * and fetch them in chunks.
    refsScope.setIndexedVariables(0);

//   * A hint for how to present this scope in the UI. If this attribute is
//   * missing, the scope is shown with a generic UI.
//   * Values:
//   * 'arguments': Scope contains method arguments.
//   * 'locals': Scope contains local variables.
//   * 'registers': Scope contains registers. Only a single `registers` scope
//   * should be returned from a `scopes` request.
//   * etc.
//  private String presentationHint;
    refsScope.setPresentationHint("arguments");

//   * If true, the number of variables in this scope is large or expensive to
//   * retrieve.
//  private boolean expensive;

//  /**
//   * The source for this scope.
//   */
    refsScope.setSource(this.getCurrentSource());
//
//  /**
//   * The start line of the range covered by this scope.
//   */
//  line?: number;
//
//  /**
//   * Start position of the range covered by the scope. It is measured in UTF-16
//   * code units and the client capability `columnsStartAt1` determines whether
//   * it is 0- or 1-based.
//   */
//  column?: number;
//
//  /**
//   * The end line of the range covered by this scope.
//   */
//  endLine?: number;
//
//  /**
//   * End position of the range covered by the scope. It is measured in UTF-16
//   * code units and the client capability `columnsStartAt1` determines whether
//   * it is 0- or 1-based.
//   */
//  endColumn?: number;
    return refsScope;
  }

  /**
   * and then the variables for a scope
   *
   * A Variable is a name/value pair.
   *
   * The type attribute is shown if space permits or when hovering over the variable’s name.
   *
   * The kind attribute is used to render additional properties of the variable, e.g. different icons
   * can be used to indicate that a variable is public or private.
   *
   * If the value is structured (has children), a handle is provided to retrieve the children with
   * the variables request.
   *
   * If the number of named or indexed children is large, the numbers should be returned via the
   * namedVariables and indexedVariables attributes.
   *
   * The client can use this information to present the children in a paged UI and fetch them in chunks.
   * https://microsoft.github.io/debug-adapter-protocol/specification#Types_Variable
   * @param args
   * @return
   */
  @Override
  public CompletableFuture<VariablesResponse> variables(VariablesArguments args) {

//{"command":"variables","arguments":{"variablesReference":5,"filter":"named"}
//{"command":"variables","arguments":{"variablesReference":5,"filter":"indexed","start":0,"count":2}
    // ^-----  when we set BOTH indexed and named we get two requests
//{"command":"variables","arguments":{"variablesReference":256},"type":"request","seq":9}Content-Length: 319
  // ^-----  if we set just named, we get this instead

    return supplyAsync(
        () -> {
          VariablesResponse response = new VariablesResponse();

          if ( args.getVariablesReference() == REF_VARIABLES_REFERENCE_ID ) {
            List<Variable> vars = this.getRefVariables();
            response.setVariables(vars.toArray(new Variable[0]));
          } else if ( args.getVariablesReference() == SELF_VAR_REF ) {
//        FIXME HERE    List<Variable> vars = this.getRefVariables();
//            response.setVariables(vars.toArray(new Variable[0]));
          } else if ( args.getVariablesReference() == SUPER_VAR_REF ) {
//        FIXME HERE    List<Variable> vars = this.getRefVariables();
//            response.setVariables(vars.toArray(new Variable[0]));
          } else if ( args.getVariablesReference() == DOLLAR_VAR_REF ) {
//        FIXME HERE    List<Variable> vars = this.getRefVariables();
//            response.setVariables(vars.toArray(new Variable[0]));
          }

          // FIXME add result as an extra scope
//          List<Variable> vars = new ArrayList<>();
//          if ( this.resultVariable != null ) {
//            vars.add(createResultVar());
//          }
//
//          response.setVariables(vars.toArray(new Variable[0]));
          return response;
        }
    );
  }

  private List<Variable> getRefVariables() {
    //FIXME this needs to be reworked to support object structures
    StoppedProgramContext spc = DataSonnetDebugger.getDebugger().getStoppedProgramContext();
    Map<String, ValueInfo> selfValue = spc.getNamedVariables().get(DataSonnetDebugger.SELF_VAR_NAME);
    Variable self_ = this.createRefVariable(DataSonnetDebugger.SELF_VAR_NAME, "Object", selfValue == null ? "null" : selfValue.toString(), SELF_VAR_REF);

    Map<String, ValueInfo> superValue = spc.getNamedVariables().get(DataSonnetDebugger.SUPER_VAR_NAME);
    Variable super_ = this.createRefVariable(DataSonnetDebugger.SUPER_VAR_NAME, "Object", superValue == null ? "null" : superValue.toString(), SUPER_VAR_REF);

    Map<String, ValueInfo> dollarValue = spc.getNamedVariables().get(DataSonnetDebugger.DOLLAR_VAR_NAME);
    Variable dollar_ = this.createRefVariable(DataSonnetDebugger.DOLLAR_VAR_NAME, "Object", dollarValue == null ? "null" : dollarValue.toString(), DOLLAR_VAR_REF);

    return List.of(self_, super_, dollar_);
  }

  private Variable createRefVariable(String name, String type, String value, int ref) {
    Variable var_ = new Variable();
    var_.setValue(value);
    var_.setType(type);
    var_.setName(name);
    VariablePresentationHint ph = new VariablePresentationHint();
    ph.setKind("virtual");
    ph.setAttributes(new String[]{"readOnly"});
    ph.setVisibility("final");
    ph.setLazy(false);  // with true, VS Code shows an "eye" to click to expand the value
    var_.setPresentationHint(ph);
    var_.setVariablesReference(ref);
    var_.setNamedVariables(0);
    var_.setIndexedVariables(0);
    return var_;
  }


  @NotNull
  private Variable createResultVar() {
    Variable res = new Variable();

    res.setValue(this.resultVariable);
    res.setType("string");
    res.setName("<result>");
    VariablePresentationHint ph = new VariablePresentationHint();
    ph.setKind("virtual");
    ph.setAttributes(new String[]{"readOnly"});
    ph.setVisibility("final");
    ph.setLazy(false);  // with true, VS Code shows an "eye" to click to expand the value
    res.setPresentationHint(ph);
    res.setVariablesReference(0);
    res.setNamedVariables(0);
    res.setIndexedVariables(0);
    return res;
  }


  /**
   * The request resumes execution of all threads. If the debug adapter supports single thread execution
   * (see capability supportsSingleThreadExecutionRequests), setting the singleThread argument to true
   * resumes only the specified thread. If not all threads were resumed, the allThreadsContinued attribute
   * of the response should be set to false.
   *
   * @param args
   *
   * Specifies the active thread. If the debug adapter supports single thread
   * execution (see `supportsSingleThreadExecutionRequests`) and the argument
   * `singleThread` is true, only the thread with this ID is resumed.
   *  threadId: number;
   *
   * If this flag is true, execution is resumed only for the thread with given
   * `threadId`.
   *  singleThread?: boolean;
   *
   * @return
   */
  @Override
  public CompletableFuture<ContinueResponse> continue_(ContinueArguments args) {
// {"command":"continue","arguments":{"threadId":0},"type":"request","seq":241}Content-Length: 119
    return supplyAsync(
        () -> {
          ContinueResponse response = new ContinueResponse();
          int threadId = args.getThreadId();
          if (threadId == 0) {
            DataSonnetDebugger.getDebugger().setStepMode(false);
            response.setAllThreadsContinued(Boolean.TRUE);
            return response;
          } else {
            throw new RuntimeException("Unknown thread id: " + threadId);
          }
        }
    );
  }

  /**
   * The request executes one step (in the given granularity) for the specified thread and allows all other threads
   * to run freely by resuming them.
   *
   * If the debug adapter supports single thread execution (see capability supportsSingleThreadExecutionRequests),
   * setting the singleThread argument to true prevents other suspended threads from resuming.
   *
   * The debug adapter first sends the response and then a stopped event (with reason step) after the step has
   * completed.
   *
   * Specifies the thread for which to resume execution for one step (of the
   * given granularity).
  threadId: number;

   * If this flag is true, all other suspended threads are not resumed.
  singleThread?: boolean;

   * Stepping granularity. If no granularity is specified, a granularity of
   * `statement` is assumed.
  granularity?: SteppingGranularity;

   The granularity of one ‘step’ in the stepping requests next, stepIn, stepOut, and stepBack. Values:

‘statement’: The step should allow the program to run until the current statement has finished executing. The meaning
   of a statement is determined by the adapter and it may be considered equivalent to a line.
   For example ‘for(int i = 0; i < 10; i++)’ could be considered to have 3 statements ‘int i = 0’, ‘i < 10’, and ‘i++’.
‘line’: The step should allow the program to run until the current source line has executed.
‘instruction’: The step should allow one instruction to execute (e.g. one x86 instruction).

   * @param args
   * @return
   */
  @Override
  public CompletableFuture<Void> next(NextArguments args) {
    return runAsync(
        () -> {
          logger.info("next: " + args);
          // TODO process granularity args
          // TODO validate threadId
          DataSonnetDebugger.getDebugger().resume();
        }
    );
  }

  /**
   * The request resumes the given thread to step into a function/method and allows all other threads to run freely
   * by resuming them.
   *
   * If the debug adapter supports single thread execution (see capability supportsSingleThreadExecutionRequests),
   * setting the singleThread argument to true prevents other suspended threads from resuming.
   *
   * If the request cannot step into a target, stepIn behaves like the next request.
   *  This implementation forwards to next (step over)
   *
   * The debug adapter first sends the response and then a stopped event (with reason step) after the step has
   * completed.
   *
   * If there are multiple function/method calls (or other targets) on the source line,
   * the argument targetId can be used to control into which target the stepIn should occur.
   * The list of possible targets for a given source line can be retrieved via the stepInTargets request.
   *
   */
  @Override
  public CompletableFuture<Void> stepIn(StepInArguments args) {
    NextArguments nextArgs = new NextArguments();
    nextArgs.setThreadId(args.getThreadId());
    nextArgs.setGranularity(args.getGranularity());
    nextArgs.setSingleThread(args.getSingleThread());
    return next(nextArgs);
  }

  /**
   * This implementation forwards to next (step over)
   */
  @Override
  public CompletableFuture<Void> stepOut(StepOutArguments args) {
    NextArguments nextArgs = new NextArguments();
    nextArgs.setThreadId(args.getThreadId());
    nextArgs.setGranularity(args.getGranularity());
    nextArgs.setSingleThread(args.getSingleThread());
    return next(nextArgs);
  }

  /**
   * Debuggee launched: if a debug adapter supports the terminate request, the development tool uses it to terminate
   * the debuggee gracefully, i.e. it gives the debuggee a chance to cleanup everything before terminating.
   *
   * @param args
   * @return
   */
  @Override
  public CompletableFuture<Void> terminate(TerminateArguments args) {
    return runAsync(
        () -> {
          DataSonnetDebugger.getDebugger().detach();
          DataSonnetDebugger.getDebugger().setDebuggerAdapter(null);
        }
    );
  }

  /**
   * Debuggee launched: The disconnect request is expected to terminate the debuggee (and any child processes)
   * forcefully.
   * Debuggee attached: If the debuggee has been “attached” initially, the development tool issues a disconnect request.
   * @param args
   * @return
   */
  @Override
  public CompletableFuture<Void> disconnect(DisconnectArguments args) {
    return runAsync(
        () -> {
          DataSonnetDebugger.getDebugger().detach();
          DataSonnetDebugger.getDebugger().setDebuggerAdapter(null);
        }
    );
  }

  @Override
  public CompletableFuture<SetVariableResponse> setVariable(SetVariableArguments args) {
    return supplyAsync(
        () -> {
          throw new RuntimeException("setVariable not supported");
        }
    );
  }


  /**
   * Executes asynchronously the given task ensuring that the context class loader is properly set to ensure that
   * the classes from third party libraries are found.
   *
   * @param runnable the task to execute
   * @return the new CompletableFuture
   */
  private static CompletableFuture<Void> runAsync(Runnable runnable) {
    final ClassLoader callerCCL = java.lang.Thread.currentThread().getContextClassLoader();
    return CompletableFuture.runAsync(
        () -> {
          final ClassLoader currentCCL = java.lang.Thread.currentThread().getContextClassLoader();
          try {
            java.lang.Thread.currentThread().setContextClassLoader(callerCCL);
            runnable.run();
          } finally {
            java.lang.Thread.currentThread().setContextClassLoader(currentCCL);
          }
        }
    );
  }

  /**
   * Calls asynchronously the given supplier ensuring that the context class loader is properly set to ensure that
   * the classes from third party libraries are found.
   *
   * @param supplier the supplier to call
   * @return the new CompletableFuture
   * @param <U> the type of the result
   */
  private static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
    final ClassLoader callerCCL = java.lang.Thread.currentThread().getContextClassLoader();
    return CompletableFuture.supplyAsync(
        () -> {
          final ClassLoader currentCCL = java.lang.Thread.currentThread().getContextClassLoader();
          try {
            java.lang.Thread.currentThread().setContextClassLoader(callerCCL);
            return supplier.get();
          } finally {
            java.lang.Thread.currentThread().setContextClassLoader(currentCCL);
          }
        }
    );
  }

  /**
   * FIXME util functions should be moved to a helper class
   *
   * @param filePath
   * @return
   * @throws IOException
   */
  public static String readFileAsString(String filePath) throws IOException {
    return new String(readFileAsBytes(filePath));
  }

  public static byte[] readFileAsBytes(String filePath) throws IOException {
    Path path = new File(filePath).toPath();
    return Files.readAllBytes(path);
  }

  /**
   * Starts a thread that calls fakeStop to generate "stopped" events
   */
  private void startFaker() {
    fakerThread = new java.lang.Thread((Runnable) this::fakeStop, "DSl DAP - Faker");
    fakerThread.start();
  }

  /// To be removed: used during development

  /**
   * Whenever the program stops (on program entry, because a breakpoint was hit, an exception occurred, or
   * the user requested execution to be paused), the debug adapter sends a stopped event with the appropriate
   * reason and thread id.
   */
  private void fakeStop() {
    while (!fakerThread.isInterrupted()) {
      try {
        java.lang.Thread.sleep(10000);
      } catch (InterruptedException e) {
        java.lang.Thread.currentThread().interrupt();
        return;
      }
      if (client == null) {
        // no client
        continue;
      }

      StoppedEventArguments args = new StoppedEventArguments();

//     * The reason for the event.
//     * For backward compatibility this string is shown in the UI if the
//     * `description` attribute is missing (but it must not be translated).
      args.setReason(StoppedEventArgumentsReason.ENTRY);
      args.setReason(StoppedEventArgumentsReason.GOTO);
      args.setReason(StoppedEventArgumentsReason.EXCEPTION);
      args.setReason(StoppedEventArgumentsReason.FUNCTION_BREAKPOINT);
      args.setReason(StoppedEventArgumentsReason.STEP);
      args.setReason(StoppedEventArgumentsReason.BREAKPOINT);

//     * A value of true hints to the client that this event should not change the
//     * focus.
      args.setPreserveFocusHint(true);

//     * Additional information. E.g. if reason is `exception`, text contains the
//     * exception name. This string is shown in the UI.
      args.setText("breakpoint such and such was hit");

//     * If `allThreadsStopped` is true, a debug adapter can announce that all
//     * threads have stopped.
//     * - The client should use this information to enable that all threads can
//     * be expanded to access their stacktraces.
//     * - If the attribute is missing or false, only the thread with the given
//     * `threadId` can be expanded.
      args.setAllThreadsStopped(true);

//     * The full reason for the event, e.g. 'Paused on exception'. This string is
//     * shown in the UI as is and can be translated.
      args.setDescription("breakpoint HIT");

//     * Ids of the breakpoints that triggered the event. In most cases there is
//     * only a single breakpoint but here are some examples for multiple
//     * breakpoints:
//     * - Different types of breakpoints map to the same location.
//     * - Multiple source breakpoints get collapsed to the same instruction by
//     * the compiler/runtime.
//     * - Multiple function breakpoints with different function names map to the
//     * same location.
      args.setHitBreakpointIds(new Integer[]{2});


//     * The thread which was stopped.
//      final int threadId = threadIdCounter.incrementAndGet();
      args.setThreadId(DATASONNET_THREAD_ID);

      client.stopped(args);

    }
  }
}
