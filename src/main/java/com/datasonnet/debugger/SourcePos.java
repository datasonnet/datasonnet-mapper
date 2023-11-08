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

import java.nio.file.Path;
import java.util.Objects;

public class SourcePos {

    private int line;
    private int caretPos;
    private int caretPosInLine;
    private String currentFile;

    @Override
    public String toString() {
        return "SourcePos{" +
                "file : " + Objects.toString(getCurrentFile()) + ", " +
                "line : " + getLine() + ", " +
                "caretPos : " + getCaretPos() + ", " +
                "caretLinePos : " + getCaretPosInLine() + ", " +
                '}';
    }

    /**
     * line in the source. This is 0-based
     *
     */
    public int getLine() {
        return line;
    }

    public SourcePos setLine(int line) {
        this.line = line;
        return this;
    }

    /**
     * Caret position in the whole program text ( String )
     */
    public int getCaretPos() {
        return caretPos;
    }

    public SourcePos setCaretPos(int caretPos) {
        this.caretPos = caretPos;
        return this;
    }

    /**
     * caret position in the current line
     */
    public int getCaretPosInLine() {
        return caretPosInLine;
    }

    public SourcePos setCaretPosInLine(int caretPosInLine) {
        this.caretPosInLine = caretPosInLine;
        return this;
    }

    public String getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(String currentFile) {
        this.currentFile = currentFile;
    }
}
