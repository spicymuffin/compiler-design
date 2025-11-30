package minic.astgen;

import minic.scanner.SourcePos;

public class StringType extends Type {

  public StringType (SourcePos pos) {
    super (pos);
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

}
