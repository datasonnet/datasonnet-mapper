package com.datasonnet.debugger;

import com.datasonnet.debugger.da.DataSonnetDebugAdapterServer;
import com.datasonnet.jsonnet.Expr;
import com.datasonnet.jsonnet.FileScope;
import com.datasonnet.jsonnet.Val;
import com.datasonnet.jsonnet.ValScope;
import com.datasonnet.wrap.DataSonnetPath;
import scala.Option;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton that enables debugging features on an Evaluator.
 */
public class DataSonnetDebugger {

  private static final Logger logger = LoggerFactory.getLogger(DataSonnetDebugger.class);

  /**
   * Instance
   */
  private static DataSonnetDebugger DEBUGGER;

  /**
   * Key is line number
   * <p>
   * TODO
   * 1) why String and not Integer as a key?
   */
  private final ConcurrentMap<String, Breakpoint> breakpoints = new ConcurrentHashMap<>();

  /**
   * DAP server; receives notifications and sets breakpoints
   */
  private DataSonnetDebugAdapterServer dapServer;

  /**
   * Is the dapServer attached?
   */
  private boolean attached = false;

  /**
   * Synchronization latch between dapServer and this debugger
   */
  private CountDownLatch latch;

  /**
   * Forces a stopped event on every evaluation step
   */
  private boolean autoStepping = false;

  /**
   * Holds information, mainly around the variables and caret pos, when the program stops
   */
  private StoppedProgramContext spc;

  public static DataSonnetDebugger getDebugger() {
    if (DEBUGGER == null) {
      DEBUGGER = new DataSonnetDebugger();
    }
    return DEBUGGER;
  }

  public void addBreakpoint(int line) {
    breakpoints.put(String.valueOf(line), new Breakpoint(line));
  }

  public Breakpoint getBreakpoint(int line) {
    return breakpoints.get(String.valueOf(line));
  }

  public void removeBreakpoint(int line) {
    breakpoints.remove(String.valueOf(line));
  }

  public void probeExpr(Expr expr, ValScope valScope, FileScope fileScope) {

    SourcePos sourcePos = this.getSourcePos(expr, fileScope);
    Breakpoint breakpoint = sourcePos != null ? breakpoints.get(sourcePos.getLine()) : null;
    if (this.isAutoStepping() || (breakpoint != null && breakpoint.isEnabled())) {
      this.saveContext(expr, valScope, fileScope, sourcePos);
      if (this.dapServer != null) {
        this.dapServer.stopped(this.spc);
      }
      latch = new CountDownLatch(1);
      try {
        // Waits for another thread - the DAP - to resume
        latch.await();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      logger.info("Resuming after await");
      this.cleanContext();
    }
  }

  private void cleanContext() {
    this.spc = null;
  }

  public StoppedProgramContext getStoppedProgramContext() {
    return this.spc;
  }

  /**
   * Saves the execution context, so that it can later be retrieved from the DAP server while the evaluator is paused
   *
   * @param expr
   * @param valScope
   * @param fileScope
   * @param sourcePos
   */
  private void saveContext(Expr expr, ValScope valScope, FileScope fileScope, SourcePos sourcePos) {
    StoppedProgramContext spc = new StoppedProgramContext();
    spc.setSourcePos(sourcePos);
    Map<String, String> namedVariables = new HashMap<>();
    // FIXME Need to not save the string representation but the composed object, so that it can be expanded on the client
    namedVariables.put("self", this.valToString(valScope.self0()));
    namedVariables.put("super", this.valToString(valScope.super0()));
    namedVariables.put("dollar", this.valToString(valScope.dollar0()));

    scala.collection.immutable.Map<String, Object> nameIndices = fileScope.nameIndices();

    spc.setNamedVariables(namedVariables);
    // FIXME is there a way to get the local variables as bindings? The parser removes the local variables names
    // when processing `local` declarations, replacing them with indexes
    // Also can we get the value? They're instances of com.datasonnet.jsonnet.Val$Lazy
//        spc.setBidings();
    this.spc = spc;
  }

  private String valToString(Option<Val.Obj> optVal) {
    if (optVal.isEmpty()) return "null";
    Val.Obj vo = optVal.get();

    StringBuffer str = new StringBuffer();
    str.append("{ ");
    vo.foreachVisibleKey((key, visibility) -> {
      // Accessing the cache to avoid running into a computation while debugging
      Option<Val> member = vo.valueCache().get(key);
      str.append("" + key + ": " + (member.isEmpty() ? "null" : member.get()) + ", ");
      if (member.nonEmpty()) {
        // FIXME could do a recursive valToString on this member too
      }
      // FIXME print arrays too
      return null;
    });
    str.append("}");
    return str.toString();
  }

  /**
   * Return a SourcePos for the expr on the fileScope
   *
   * @param expr
   * @param fileScope
   * @return
   */
  private SourcePos getSourcePos(Expr expr, FileScope fileScope) {
    if (fileScope.currentFile() instanceof DataSonnetPath) {
      String sourceCode = ((DataSonnetPath) fileScope.currentFile()).source();
      int caretPos = expr.offset();
      if (caretPos > sourceCode.length()) {
        logger.error("caretPos: " + caretPos + " > sourceCode.length() " + sourceCode.length());
        return null;
      }
      String preface = sourceCode.substring(0, expr.offset());

      String[] lines = preface.split("\n");
      SourcePos sourcePos = new SourcePos();
      sourcePos.setCaretPos(caretPos);
      sourcePos.setLine(lines.length); // lines are 0-based, and this counts the number of previous lines
      sourcePos.setCaretPosInLine(caretPos - preface.lastIndexOf("\n"));

      // Mapper.asFunction wraps the script with `function (payload) {` as the first line, and `}` at the end.
      // so here we add need so subtract 1 to the line
      // TODO Also see Run::alreadyWrapped, that's a parameter to avoid adding this wrapping function
      sourcePos.setLine(sourcePos.getLine() - 1);
      return sourcePos;
    }
    return null;
  }

  /**
   * resume execution
   */
  public void resume() {
    logger.debug("resume");
    if (latch != null && latch.getCount() > 0) {
      logger.debug("latch.countDown");
      latch.countDown();
    }
  }

  public void attach() {
    attached = true;
  }

  /**
   * detach and resume
   */
  public void detach() {
    attached = false;
    this.resume();
  }

  public boolean isAttached() {
    return attached;
  }

  public void setAutoStepping(boolean sm) {
    this.autoStepping = sm;
  }

  public boolean isAutoStepping() {
    return this.autoStepping;
  }

  public void setDebuggerAdapter(DataSonnetDebugAdapterServer dataSonnetDebugAdapterServer) {
    this.dapServer = dataSonnetDebugAdapterServer;
  }
}
