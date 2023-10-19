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
import scala.Option;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private boolean attached = false;

    /**
     * Synchronization latch between dapServer and this debugger
     */
    private CountDownLatch latch;

    /**
     * Forces a stopped event on every evaluation step
     */
    private boolean stepMode = false;

    /**
     * Holds information, mainly around the variables and caret pos, when the program stops
     */
    private StoppedProgramContext spc;

    private int lineCount = -1;
    private int diffOffset = -1;
    private int diffLinesCount = -1;

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

    public void probeExpr(Expr expr, ValScope valScope, FileScope fileScope, EvalScope evalScope) {
        SourcePos sourcePos = this.getSourcePos(expr, fileScope);
        if (sourcePos == null) {
            logger.debug("sourcePos is null, returning");
            return;
        }
        int line = sourcePos.getLine();
        Breakpoint breakpoint = sourcePos != null ? breakpoints.get(line) : null;
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
        StoppedProgramContext spc = new StoppedProgramContext();
        spc.setSourcePos(sourcePos);
        Map<String, Map<String, ValueInfo>> namedVariables = new HashMap<>();

        namedVariables.put(SELF_VAR_NAME, valScope.self0().nonEmpty() ? this.mapObject(valScope.self0().get(), evalScope) : null);
        namedVariables.put(SUPER_VAR_NAME, valScope.super0().nonEmpty() ? this.mapObject(valScope.super0().get(), evalScope) : null);
        namedVariables.put(DOLLAR_VAR_NAME, valScope.dollar0().nonEmpty() ? this.mapObject(valScope.dollar0().get(), evalScope) : null);
        logger.debug("saveContext. namedVariables is: " + namedVariables);

        scala.collection.immutable.Map<String, Object> nameIndices = fileScope.nameIndices();

        spc.setNamedVariables(namedVariables);
        // FIXME is there a way to get the local variables as bindings? The parser removes the local variables names
        // when processing `local` declarations, replacing them with indexes
        // Also can we get the value? They're instances of com.datasonnet.jsonnet.Val$Lazy
//        spc.setBidings();
        this.spc = spc;
    }

    private List<ValueInfo> mapArr(@Nullable Val.Arr arrValue, EvalScope evalScope) {
        if (this.attached) {
            this.detach(false);//We must detach the debugger while forcing the lazy value, to avoid deadlocks
        }

        List<ValueInfo> mappedArr = new CopyOnWriteArrayList<>();

        arrValue.value().foreach(member -> {
/*
            Val memberVal = member.force();
            if (memberVal instanceof Val.Obj) {
                Object mappedMember = mapObject((Val.Obj) memberVal, evalScope);
                mappedArr.add(new ValueInfo(0, "", mappedMember));
            } else if (memberVal instanceof Val.Arr) {
                Object mappedArray = mapArr((Val.Arr) memberVal, evalScope);
                mappedArr.add(new ValueInfo(0, "", mappedArray));
            } else {
                mappedArr.add(new ValueInfo(0, "", Materializer.apply(memberVal, evalScope)));
            }
*/

            return null;
        });

        if (!this.attached) {
            this.attach(false);
        }

        return mappedArr;
    }

    private Map<String, ValueInfo> mapObject(@Nullable Val.Obj objectValue, EvalScope evalScope) {
        if (objectValue == null) {
            return null;
        }

        if (this.attached) {
            this.detach(false);//We must detach the debugger while forcing the lazy value, to avoid deadlocks
        }

        Map<String, ValueInfo> mappedObject = new ConcurrentHashMap<>();

        objectValue.foreachVisibleKey((key, visibility) -> { //TODO Only visible keys?
            Option<Val> member = objectValue.valueCache().get(key);
            if (member.nonEmpty()) {
                Val memberVal = member.get();
                if (memberVal instanceof Val.Obj) {
                    Object mappedMember = mapObject((Val.Obj) memberVal, evalScope);
                    mappedObject.put(key, new ValueInfo(0, key, mappedMember));
                } else if (memberVal instanceof Val.Arr) {
                    Object mappedArr = mapArr((Val.Arr) memberVal, evalScope);
                    mappedObject.put(key, new ValueInfo(0, key, mappedArr));
                } else {
                    mappedObject.put(key, new ValueInfo(0, key, Materializer.apply(memberVal, evalScope)));
                }
            } else {
                mappedObject.put(key, new ValueInfo(0, key, null));
            }
            return null;
        });

        if (!this.attached) {
            this.attach(false);
        }

        return mappedObject;
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
        if (fileScope.source() != null) {
            String sourceCode = fileScope.source();
            // offset points to the first character of the expressions. Offsets are zero based.
            String visibleCode = sourceCode;

            if (lineCount != -1) { // If the code is wrapped by IDE, remove auto-generated lines
                String[] sourceLines = sourceCode.split("\\R");
                logger.debug("sourceLines: " + sourceLines);

                if (diffLinesCount == -1 && diffOffset == -1) {
                    diffLinesCount = sourceLines.length - lineCount;
                    String[] diffLines = Arrays.copyOfRange(sourceLines, 0, diffLinesCount);
                    logger.debug("diffLines: " + diffLines);
                    diffOffset = String.join(System.lineSeparator(), diffLines).length();
                    logger.debug("diffOffset: " + diffOffset);
                }

                sourceLines = Arrays.copyOfRange(sourceLines, diffLinesCount, sourceLines.length);
                visibleCode = String.join(System.lineSeparator(), sourceLines);
            }

            int caretPos = expr.offset() - (diffOffset != -1 ? diffOffset : 0);
            logger.debug("caretPos: " + caretPos);
            if (caretPos < 0) {
                logger.debug("caretPos is in invisible code");
                return null;
            }
            if (caretPos > sourceCode.length()) {
                logger.error("caretPos: " + caretPos + " > sourceCode.length() " + sourceCode.length());
                return null;
            }

            String prefaceIncludingCurrent = visibleCode.substring(0, caretPos + 1);
            String[] lines = prefaceIncludingCurrent.split("\\R");

            SourcePos sourcePos = new SourcePos();
            sourcePos.setCurrentFile(Objects.toString(fileScope.currentFile()));
            sourcePos.setCaretPos(caretPos);
            // Lines are zero-based, and the caret is on the last line of the array
            sourcePos.setLine(lines.length - (diffLinesCount != -1 ? diffLinesCount : 0) - 1);
            sourcePos.setCaretPosInLine(caretPos - ( prefaceIncludingCurrent.lastIndexOf("\n") + 1 ));
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
        this.attach(true);
    }
    public void attach(boolean clear) {
        if (!attached) {
            attached = true;
            System.setProperty("debug", "true");
            if (clear) {
                breakpoints.clear();
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
        attached = false;
        System.setProperty("debug", "false");
        if (clear) {
            breakpoints.clear();
        }
        this.resume();
    }

    public boolean isAttached() {
        return attached;
    }

    public void setStepMode(boolean stepMode) {
        this.stepMode = stepMode;
    }

    public boolean isStepMode() {
        return this.stepMode;
    }

    public void setDebuggerAdapter(DataSonnetDebugListener dataSonnetDebugListener) {
        this.debugListener = dataSonnetDebugListener;
    }

    public int getLineCount() {
        return lineCount;
    }

    public void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }
}
