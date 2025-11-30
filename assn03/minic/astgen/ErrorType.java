package minic.astgen;

import minic.scanner.SourcePos;

public class ErrorType extends Type {

  public ErrorType (SourcePos pos) {
    super (pos);
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

}
