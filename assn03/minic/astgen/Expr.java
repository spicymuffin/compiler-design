package minic.astgen;

import minic.scanner.SourcePos;

public abstract class Expr extends AST {

  public Expr (SourcePos pos) {
    super (pos);
  }

}
