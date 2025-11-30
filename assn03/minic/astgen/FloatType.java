package minic.astgen;

import minic.scanner.SourcePos;

public class FloatType extends Type {

  public FloatType (SourcePos pos) {
    super (pos);
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

}
