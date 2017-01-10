package com.realtek.Utils;

public class ParseException extends Exception
{
  private final String line;
  private final int lineNumber;

  public ParseException(String line, int lineNumber, Throwable cause)
  {
    super(cause);
    this.line = line;
    this.lineNumber = lineNumber;
  }

  public ParseException(String line, int lineNumber, String message) {
    super(message);
    this.line = line;
    this.lineNumber = lineNumber;
  }

  public String getLine() {
    return this.line;
  }

  public int getLineNumber() {
    return this.lineNumber;
  }

  public String getMessage()
  {
    return "Error at line " + getLineNumber() + ": " + getLine() + "\n" + super.getMessage();
  }
}