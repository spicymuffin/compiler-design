package minic.astgen;

import minic.scanner.SourcePos;

public class Operator extends Terminal {

  public Operator (String Lexeme, SourcePos pos) {
    super (pos);
    this.Lexeme = Lexeme;
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

}
