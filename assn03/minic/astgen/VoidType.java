package minic.astgen;

import minic.scanner.SourcePos;

public class VoidType extends Type {

  public VoidType (SourcePos pos) {
    super (pos);
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

}
