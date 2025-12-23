package minic.astgen;

import minic.scanner.SourcePos;

public class BoolLiteral extends Terminal {

  boolean value;

  public BoolLiteral (String Lexeme, SourcePos pos) {
    super (pos);
    this.Lexeme = Lexeme;
    this.value = Boolean.parseBoolean(Lexeme);
  }

  public boolean GetValue() {
    return value;
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

}
