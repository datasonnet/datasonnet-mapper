package com.datasonnet.debugger;

public class SourcePos {

  private int line;

  private int caretPos;

  private int caretPosInLine;

    @Override
    public String toString() {
        return "SourcePos{" +
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
}
