package minic.astgen;

import minic.scanner.SourcePos;

public class FloatLiteral extends Terminal {

  float value;

  public FloatLiteral (String Lexeme, SourcePos pos) {
    super (pos);
    this.Lexeme = Lexeme;
    this.value = Float.parseFloat(Lexeme);
  }

  public float GetValue() {
    return value;
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

}
