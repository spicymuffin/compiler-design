package minic.astgen;

import minic.scanner.SourcePos;

public class IntLiteral extends Terminal {

  public IntLiteral (String Lexeme, SourcePos pos) {
    super (pos);
    this.Lexeme = Lexeme;
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

}
