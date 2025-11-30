package minic.astgen;

import minic.scanner.SourcePos;

public class BoolType extends Type {

  public BoolType (SourcePos pos) {
    super (pos);
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

}
