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

import com.datasonnet.debugger.da.DataSonnetDebugListener;
import com.datasonnet.jsonnet.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;
import scala.util.Either;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Singleton that enables debugging features on an Evaluator.
 */
public class DataSonnetDebugger {
    public static final String SELF_VAR_NAME = "self";
    public static final String SUPER_VAR_NAME = "super";
    public static final String DOLLAR_VAR_NAME = "$";
    private static final Logger logger = LoggerFactory.getLogger(DataSonnetDebugger.class);
    /**
     * Instance
     */
    private static DataSonnetDebugger DEBUGGER;

    /**
     * Key is line number
     */
    private final ConcurrentMap<Integer, Breakpoint> breakpoints = new ConcurrentHashMap<>();

    /**
     * DAP server; receives notifications and sets breakpoints
     */
    private DataSonnetDebugListener debugListener;

    /**
     * Is the dapServer attached?
     */
    private AtomicBoolean attached = new AtomicBoolean(false);

    /**
     * Synchronization latch between dapServer and this debugger
     */
    private CountDownLatch latch;

    /**
     * Forces a stopped event on every evaluation step
     */
    private AtomicBoolean stepMode = new AtomicBoolean(false);

    /**
     * Holds information, mainly around the variables and caret pos, when the program stops
     */
    private StoppedProgramContext spc;

    private AtomicInteger lineCount = new AtomicInteger(-1);
    private AtomicInteger diffOffset = new AtomicInteger(-1);

    private ValScope currentValScope;
    private FileScope currentFileScope;
    private EvalScope currentEvalScope;

    public static DataSonnetDebugger getDebugger() {
        if (DEBUGGER == null) {
            DEBUGGER = new DataSonnetDebugger();
        }
        return DEBUGGER;
    }

    public void addBreakpoint(int line) {
        addBreakpoint(line, false);
    }

    public void addBreakpoint(int line, boolean temporary) {
        breakpoints.put(line, new Breakpoint(line, temporary));
    }

    public Breakpoint getBreakpoint(int line) {
        return breakpoints.get(line);
    }

    public void removeBreakpoint(int line) {
        breakpoints.remove(line);
    }

    public void clearBreakpoints() {
        this.breakpoints.clear();
    }

    public void probeExpr(Expr expr, ValScope valScope, FileScope fileScope, EvalScope evalScope) {
        if (isAttached()) {
            this.detach(false);//We must detach the debugger while probing the expression, to avoid stack overflow
        }

        SourcePos sourcePos = this.getSourcePos(expr, fileScope);
        if (sourcePos == null) {
            logger.debug("sourcePos is null, returning");
            if (!isAttached()) {
                this.attach(false);
            }
            return;
        }

        int line = sourcePos.getLine();
        Breakpoint breakpoint = breakpoints.get(line);
        logger.debug("line " + line + " breakpoints " + breakpoint);
        if (this.isStepMode() || (breakpoint != null && breakpoint.isEnabled())) {
            this.saveContext(expr, valScope, fileScope, evalScope, sourcePos);
            if (this.debugListener != null) {
                this.debugListener.stopped(this.spc);
            }
            //We are going to stop at the breakpoint so enter the auto-stepping mode
            setStepMode(true);
            latch = new CountDownLatch(1);
            try {
                // Waits for another thread - the DAP - to resume
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            logger.debug("Resuming after await");
            this.cleanContext();
            if (breakpoint != null && breakpoint.isTemporary()) {
                breakpoints.remove(line);
            }
        }

        if (!isAttached()) {
            this.attach(false);
        }
    }

    public Object evaluateExpression(String expression) {
        if (isAttached()) {
            this.detach(false);//We must detach the debugger while probing the expression, to avoid stack overflow
        }

        Interpreter interpreter = new Interpreter((Evaluator) currentEvalScope);
        Either either = interpreter.parse(expression, currentFileScope.currentFile());
        if (either.isLeft()) {
            return either.left().get();
        }
        Expr expr = (Expr) either.right().get();
        Object value = "";
        try {
            Val exprVal = currentEvalScope.visitExpr(expr, currentValScope, currentFileScope);
            value = Materializer.apply(exprVal, currentEvalScope);
        } catch (Exception e) {
            value = e;
        }

        if (!isAttached()) {
            this.attach(false);
        }

        return value;
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
    private void saveContext(Expr expr, ValScope valScope, FileScope fileScope, EvalScope evalScope, SourcePos sourcePos) {
        if (isAttached()) {
            this.detach(false);//We must detach the debugger while probing the expression, to avoid stack overflow
        }

        currentFileScope = fileScope;
        currentValScope = valScope;
        currentEvalScope = evalScope;

        StoppedProgramContext spc = new StoppedProgramContext();
        spc.setSourcePos(sourcePos);
        Map<String, Object> namedVariables = new HashMap<>();

        namedVariables.put(SELF_VAR_NAME, valScope.self0().nonEmpty() ? this.mapValue(valScope.self0().get(), SELF_VAR_NAME, evalScope, false) : null);
        namedVariables.put(SUPER_VAR_NAME, valScope.super0().nonEmpty() ? this.mapValue(valScope.super0().get(), SUPER_VAR_NAME, evalScope, false) : null);
        namedVariables.put(DOLLAR_VAR_NAME, valScope.dollar0().nonEmpty() ? this.mapValue(valScope.dollar0().get(), DOLLAR_VAR_NAME, evalScope, false) : null);
        logger.debug("saveContext. namedVariables is: " + namedVariables);

        scala.collection.immutable.Map<String, Object> nameIndices = fileScope.nameIndices();

        Val.Lazy[] bindings = valScope.getBindings();

        for (int idx = 0; idx < bindings.length; idx++) {
            Val.Lazy nextBinding = bindings[idx];
            if (nextBinding != null) {
                Option<String> name = fileScope.getNameByIndex(idx);
                if (name.nonEmpty()) {
                    String nameStr = name.get();
                    logger.debug("Next binding name is: " + nameStr);
                    if (!nameStr.equals("std") && !nameStr.equals("cml")) { //TODO we don't need to show them or do we?
                        Val forced = nextBinding.force();
                        Object mapped = this.mapValue(forced, nameStr, evalScope, true);
                        namedVariables.put(nameStr, mapped);
                        logger.debug("Binding '" + nameStr + "' value is: " + mapped);
                    }
                }
            }
        }

        spc.setNamedVariables(namedVariables);
        // FIXME is there a way to get the local variables as bindings? The parser removes the local variables names
        // when processing `local` declarations, replacing them with indexes
        // Also can we get the value? They're instances of com.datasonnet.jsonnet.Val$Lazy
//        spc.setBidings();
        this.spc = spc;

        if (!isAttached()) {
            this.attach(false);
        }
    }

    private Object mapValue(@Nullable Val theVal, String name, EvalScope evalScope, boolean isBinding) {
        Object mapped = null;
        if (theVal instanceof Val.Obj) {
            Val.Obj objectValue = (Val.Obj) theVal;
            if (isBinding) {
                Materializer.apply(objectValue, evalScope);//To populate the cache
            }
            Map<String, ValueInfo> mappedObject = new ConcurrentHashMap<>();

            objectValue.foreachVisibleKey((key, visibility) -> { //TODO Only visible keys?
                Option<Val> member = objectValue.valueCache().get(key);
                if (member.nonEmpty() && !"self".equals(key) && !"$".equals(key) && !"super".equals(key)) {
                    Val memberVal = member.get();
                    Object mappedVal = mapValue(memberVal, key, evalScope, isBinding);
                    mappedObject.put(key, mappedVal instanceof ValueInfo ? (ValueInfo) mappedVal : new ValueInfo(memberVal.sourcePosition(), key, mappedVal));
                } else {
                    mappedObject.put(key, new ValueInfo(0, key, null));
                }
                return null;
            });

            mapped = mappedObject;
        } else if (theVal instanceof Val.Arr) {
            Val.Arr arrValue = (Val.Arr) theVal;
            List<ValueInfo> mappedArr = new CopyOnWriteArrayList<>();

            arrValue.value().foreach(member -> {
                Val memberVal = member.force();
                //FIXME
                Materializer.apply(memberVal, evalScope);//TODO we need to review this - it works but it calculates values of ALL objects, not just previously evaluated ones
                Object mappedVal = mapValue(memberVal, "", evalScope, isBinding);
                mappedArr.add(mappedVal instanceof ValueInfo ? (ValueInfo) mappedVal : new ValueInfo(memberVal.sourcePosition(), "", mapValue(memberVal, "", evalScope, isBinding)));
                return null;
            });

            mapped = mappedArr;
        } else if (theVal instanceof Val.Func) {
            mapped = new ValueInfo(theVal.sourcePosition(), name, "FUNCTION");
        } else {
            mapped = new ValueInfo(theVal.sourcePosition(), name, Materializer.apply(theVal, evalScope));
        }
        return mapped;
    }

    public int getDiffOffset() {
        return diffOffset.get();
    }
    /**
     * Return a SourcePos for the expr on the fileScope<br/>
     *
     * SourcePos contains:<br/>
     *  <li>caretPos: 0-based index, points to the pos of the current position ( first character of the term )</li>
     *  <li>line: 0-based index, line where the caret is</li>
     *  <li>caretPosInLine: 0-based index; caret position within the current line ( count from the left )</li>
     *
     * @param expr
     * @param fileScope
     * @return
     */
    private SourcePos getSourcePos(Expr expr, FileScope fileScope) {
        if (fileScope.source() != null && fileScope.source().length() > 0) {
            String sourceCode = fileScope.source();

            // offset points to the first character of the expressions. Offsets are zero based.

            int diffLinesCount = 0;

            if (lineCount.get() != -1) { // If the code is wrapped by IDE, remove auto-generated lines
                String[] sourceLines = sourceCode.split("\\R");
                diffLinesCount = sourceLines.length - lineCount.get();
                String[] diffLines = Arrays.copyOfRange(sourceLines, 0, diffLinesCount);
                logger.debug("diffLines: " + diffLines);
                diffOffset.set(String.join(System.lineSeparator(), diffLines).length());
                logger.debug("diffOffset: " + diffOffset.get());
            }

            String sourceBeforeCaret = sourceCode.substring(0, expr.offset() + 1);
            int caretLine = sourceBeforeCaret.split("\\R").length - diffLinesCount - 1;
            int caretPos = expr.offset() - diffOffset.get();

            logger.debug("Caret Position: " + caretPos);
            if (caretPos < 0) {
                logger.debug("CaretPos is in invisible code, returning null...");
                return null;
            }
            if (caretPos > sourceCode.length()) {
                logger.error("CaretPos: " + caretPos + " > sourceCode.length() " + sourceCode.length());
                return null;
            }

            SourcePos sourcePos = new SourcePos();
            sourcePos.setCurrentFile(Objects.toString(fileScope.currentFile()));
            sourcePos.setCaretPos(caretPos);

            // Lines are zero-based, and the caret is on the last line of the array
            sourcePos.setLine(caretLine);
            sourcePos.setCaretPosInLine(expr.offset() - (sourceBeforeCaret.lastIndexOf("\n") + 1));
            return sourcePos;
        }
        return null;
    }

    /**
     * resume execution
     */
    public void resume() {
        logger.debug("Resume command received");
        if (latch != null && latch.getCount() > 0) {
            logger.debug("latch.countDown");
            latch.countDown();
        }
    }

    public void attach() {
        this.attach(true);
    }

    public void attach(boolean clear) {
        if (!isAttached()) {
            attached.set(true);
            System.setProperty("debug", "true");
            if (clear) {
                breakpoints.clear();
                setStepMode(false);
            }
        }
    }

    /**
     * detach and resume
     */
    public void detach() {
        this.detach(true);
    }

    public void detach(boolean clear) {
        attached.set(false);
        System.setProperty("debug", "false");
        if (clear) {
            breakpoints.clear();
            setStepMode(false);
            this.resume();
        }
    }

    public boolean isAttached() {
        return attached.get();
    }

    public void setStepMode(boolean stepMode) {
        this.stepMode.set(stepMode);
    }

    public boolean isStepMode() {
        return this.stepMode.get();
    }

    public void setDebuggerAdapter(DataSonnetDebugListener dataSonnetDebugListener) {
        this.debugListener = dataSonnetDebugListener;
    }

    public int getLineCount() {
        return lineCount.get();
    }

    public void setLineCount(int lineCount) {
        this.lineCount.set(lineCount);
    }
}
