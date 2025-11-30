package minic.parser;

import minic.ErrorReporter;
import minic.parser.SyntaxError;
import minic.scanner.Scanner;
import minic.scanner.SourcePos;
import minic.scanner.Token;

/** Parser class to perform syntax analysis of a MiniC program. */
public class Parser {

  private Scanner scanner;
  private ErrorReporter errorReporter;
  private Token ct;

  /**
   * Constructor.
   *
   * @param lexer    is the scanner provided to the parser.
   * @param reporter is the ErrorReporter object to report syntax errors.
   */
  public Parser(Scanner lexer, ErrorReporter reporter) {
    scanner = lexer;
    errorReporter = reporter;
  }

  // #region first
  private boolean first_program(int tok) {
    if (tok == Token.VOID)
      return true;
    if (tok == Token.INT)
      return true;
    if (tok == Token.BOOL)
      return true;
    if (tok == Token.FLOAT)
      return true;
    // if (tok == -1)
    // return true;
    return false;
  }

  private boolean first_func_def(int tok) {
    if (tok == Token.LEFTPAREN)
      return true;
    return false;
  }

  private boolean first_var_def(int tok) {
    if (tok == Token.LEFTBRACKET)
      return true;
    if (tok == Token.ASSIGN)
      return true;
    if (tok == Token.COMMA)
      return true;
    if (tok == Token.SEMICOLON)
      return true;
    return false;
  }

  private boolean first_init_decl(int tok) {
    if (tok == Token.ID)
      return true;
    return false;
  }

  private boolean first_declarator(int tok) {
    if (tok == Token.ID)
      return true;
    return false;
  }

  private boolean first_initializer(int tok) {
    if (tok == Token.ID)
      return true;
    if (tok == Token.LEFTPAREN)
      return true;
    if (tok == Token.INTLITERAL)
      return true;
    if (tok == Token.BOOLLITERAL)
      return true;
    if (tok == Token.FLOATLITERAL)
      return true;
    if (tok == Token.STRINGLITERAL)
      return true;
    if (tok == Token.PLUS)
      return true;
    if (tok == Token.MINUS)
      return true;
    if (tok == Token.NOT)
      return true;
    if (tok == Token.LEFTBRACE)
      return true;
    return false;
  }

  private boolean first_typespec(int tok) {
    if (tok == Token.VOID)
      return true;
    if (tok == Token.INT)
      return true;
    if (tok == Token.BOOL)
      return true;
    if (tok == Token.FLOAT)
      return true;
    return false;
  }

  private boolean first_compound_stmt(int tok) {
    if (tok == Token.LEFTBRACE)
      return true;
    return false;
  }

  private boolean first_stmt(int tok) {
    if (tok == Token.LEFTBRACE)
      return true;
    if (tok == Token.IF)
      return true;
    if (tok == Token.WHILE)
      return true;
    if (tok == Token.FOR)
      return true;
    if (tok == Token.RETURN)
      return true;
    if (tok == Token.ID)
      return true;
    return false;
  }

  private boolean first_if_stmt(int tok) {
    if (tok == Token.IF)
      return true;
    return false;
  }

  private boolean first_while_stmt(int tok) {
    if (tok == Token.WHILE)
      return true;
    return false;
  }

  private boolean first_for_stmt(int tok) {
    if (tok == Token.FOR)
      return true;
    return false;
  }

  private boolean first_expr(int tok) {
    if (tok == Token.ID)
      return true;
    if (tok == Token.LEFTPAREN)
      return true;
    if (tok == Token.INTLITERAL)
      return true;
    if (tok == Token.BOOLLITERAL)
      return true;
    if (tok == Token.FLOATLITERAL)
      return true;
    if (tok == Token.STRINGLITERAL)
      return true;
    if (tok == Token.PLUS)
      return true;
    if (tok == Token.MINUS)
      return true;
    if (tok == Token.NOT)
      return true;
    return false;
  }

  private boolean first_and_expr(int tok) {
    return first_expr(tok);
  }

  private boolean first_relational_expr(int tok) {
    return first_expr(tok);
  }

  private boolean first_add_expr(int tok) {
    return first_expr(tok);
  }

  private boolean first_mult_expr(int tok) {
    return first_expr(tok);
  }

  private boolean first_unary_expr(int tok) {
    return first_expr(tok);
  }

  private boolean first_primary_expr(int tok) {
    if (tok == Token.ID)
      return true;
    if (tok == Token.LEFTPAREN)
      return true;
    if (tok == Token.INTLITERAL)
      return true;
    if (tok == Token.BOOLLITERAL)
      return true;
    if (tok == Token.FLOATLITERAL)
      return true;
    if (tok == Token.STRINGLITERAL)
      return true;
    return false;
  }

  private boolean first_asgnexpr(int tok) {
    if (tok == Token.ID)
      return true;
    return false;
  }

  private boolean first_params_list(int tok) {
    return first_typespec(tok);
  }

  private boolean first_parameter_decl(int tok) {
    return first_typespec(tok);
  }

  private boolean first_arglist(int tok) {
    if (tok == Token.LEFTPAREN)
      return true;
    return false;
  }

  private boolean first_args(int tok) {
    return first_expr(tok);
  }
  // #endregion

  // accept(int tokenExpected) checks whether the current token matches
  // tokenExpected.
  // If so, it fetches the next token.
  // If not, it reports a syntax error.
  void accept(int tokenExpected) throws SyntaxError {
    if (ct.kind == tokenExpected) {
      ct = scanner.scan();
    } else {
      syntaxError("\"%\" expected here", Token.spell(tokenExpected));
    }
  }

  // accept() unconditionally accepts the current token
  // and fetches the next token from the scanner.
  void accept() {
    ct = scanner.scan();
  }

  void syntaxError(String messageTemplate, String tokenQuoted) throws SyntaxError {
    SourcePos pos = ct.getSourcePos();
    errorReporter.reportError(messageTemplate, tokenQuoted, pos);
    throw (new SyntaxError());
  }

  boolean is_typespec(int token) {
    if (token == Token.VOID
        || token == Token.INT
        || token == Token.BOOL
        || token == Token.FLOAT) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * parse() is the top-level, public-facing method of the parser. It is called
   * for parsing the entire MiniC program. The parse method sets up the current
   * look-ahead token, and it checks that after parsing no tokens remain in the
   * input. It is the top-level exception handler for SyntaxError exceptions.
   */
  public void parse() {
    ct = scanner.scan(); // get first token from scanner...

    try {
      parseProgram();
      if (ct.kind != Token.EOF) {
        syntaxError("\"%\" not expected after end of program",
            ct.getLexeme());
      }
    } catch (SyntaxError s) {
      return;
    }
    return;
  }

  public void parseProgram() throws SyntaxError {
    while (is_typespec(ct.kind)) {
      accept();
      accept(Token.ID);
      if (first_func_def(ct.kind)) {
        parse_func_def();
      } else if (first_var_def(ct.kind)) {
        parse_var_def();
      } else {
        syntaxError("\"%\" not expected", ct.getLexeme());
      }
    }
  }

  public void parse_func_def() throws SyntaxError {
    accept(Token.LEFTPAREN);
    if (first_params_list(ct.kind)) {
      parse_params_list();
    }
    accept(Token.RIGHTPAREN);
    parse_compound_stmt();
  }

  public void parse_var_def() throws SyntaxError {
    if (ct.kind == Token.LEFTBRACKET) {
      accept();
      accept(Token.INTLITERAL);
      accept(Token.RIGHTBRACKET);
    }
    if (ct.kind == Token.ASSIGN) {
      accept();
      parse_initializer();
    }
    while (ct.kind == Token.COMMA) {
      accept();
      parse_init_decl();
    }
    accept(Token.SEMICOLON);
  }

  public void parse_init_decl() throws SyntaxError {
    parse_declarator();
    if (ct.kind == Token.ASSIGN) {
      accept();
      parse_initializer();
    }
  }

  public void parse_declarator() throws SyntaxError {
    accept(Token.ID);
    if (ct.kind == Token.LEFTBRACKET) {
      accept();
      accept(Token.INTLITERAL);
      accept(Token.RIGHTBRACKET);
    }
  }

  public void parse_initializer() throws SyntaxError {
    if (first_expr(ct.kind)) {
      parse_expr();
    } else if (ct.kind == Token.LEFTBRACE) {
      accept();
      parse_expr();
      while (ct.kind == Token.COMMA) {
        accept();
        parse_expr();
      }
      accept(Token.RIGHTBRACE);
    } else {
      syntaxError("\"%\" not expected", ct.getLexeme());
    }
  }

  public void parse_typespec() throws SyntaxError {
    if (first_typespec(ct.kind)) {
      accept();
    } else {
      syntaxError("\"%\" not expected", ct.getLexeme());
    }
  }

  public void parse_compound_stmt() throws SyntaxError {
    accept(Token.LEFTBRACE);
    while (first_typespec(ct.kind)) {
      accept();
      accept(Token.ID);
      parse_var_def();
    }
    while (first_stmt(ct.kind)) {
      parse_stmt();
    }
    accept(Token.RIGHTBRACE);
  }

  public void parse_stmt() throws SyntaxError {
    if (first_compound_stmt(ct.kind)) {
      parse_compound_stmt();
    } else if (first_if_stmt(ct.kind)) {
      parse_if_stmt();
    } else if (first_while_stmt(ct.kind)) {
      parse_while_stmt();
    } else if (first_for_stmt(ct.kind)) {
      parse_for_stmt();
    } else if (ct.kind == Token.RETURN) {
      accept();
      if (first_expr(ct.kind)) {
        parse_expr();
      }
      accept(Token.SEMICOLON);
    } else if (ct.kind == Token.ID) {
      accept();
      if (ct.kind == Token.LEFTBRACKET || ct.kind == Token.ASSIGN) {
        if (ct.kind == Token.LEFTBRACKET) {
          accept();
          parse_expr();
          accept(Token.RIGHTBRACKET);
        }
        accept(Token.ASSIGN);
        parse_expr();
      } else if (first_arglist(ct.kind)) {
        parse_arglist();
      } else {
        syntaxError("\"%\" not expected", ct.getLexeme());
      }
      accept(Token.SEMICOLON);
    } else {
      syntaxError("\"%\" not expected", ct.getLexeme());
    }
  }

  public void parse_if_stmt() throws SyntaxError {
    accept(Token.IF);
    accept(Token.LEFTPAREN);
    parse_expr();
    accept(Token.RIGHTPAREN);
    parse_stmt();
    if (ct.kind == Token.ELSE) {
      accept(Token.ELSE);
      parse_stmt();
    }
  }

  public void parse_while_stmt() throws SyntaxError {
    accept(Token.WHILE);
    accept(Token.LEFTPAREN);
    parse_expr();
    accept(Token.RIGHTPAREN);
    parse_stmt();
  }

  public void parse_for_stmt() throws SyntaxError {
    accept(Token.FOR);
    accept(Token.LEFTPAREN);
    if (first_asgnexpr(ct.kind)) {
      parse_asgnexpr();
    }
    accept(Token.SEMICOLON);
    if (first_expr(ct.kind)) {
      parse_expr();
    }
    accept(Token.SEMICOLON);
    if (first_asgnexpr(ct.kind)) {
      parse_asgnexpr();
    }
    accept(Token.RIGHTPAREN);
    parse_stmt();
  }

  public void parse_expr() throws SyntaxError {
    parse_and_expr();
    while (ct.kind == Token.OR) {
      accept();
      parse_and_expr();
    }
  }

  public void parse_and_expr() throws SyntaxError {
    parse_relational_expr();
    while (ct.kind == Token.AND) {
      accept();
      parse_relational_expr();
    }
  }

  public void parse_relational_expr() throws SyntaxError {
    parse_add_expr();
    if (ct.kind == Token.EQ
        || ct.kind == Token.NOTEQ
        || ct.kind == Token.LESS
        || ct.kind == Token.LESSEQ
        || ct.kind == Token.GREATER
        || ct.kind == Token.GREATEREQ) {
      accept();
      parse_add_expr();
    }
  }

  public void parse_add_expr() throws SyntaxError {
    parse_mult_expr();
    while (ct.kind == Token.PLUS || ct.kind == Token.MINUS) {
      accept();
      parse_mult_expr();
    }
  }

  public void parse_mult_expr() throws SyntaxError {
    parse_unary_expr();
    while (ct.kind == Token.TIMES || ct.kind == Token.DIV) {
      accept();
      parse_unary_expr();
    }
  }

  public void parse_unary_expr() throws SyntaxError {
    if (first_primary_expr(ct.kind)) {
      parse_primary_expr();
    } else if (ct.kind == Token.PLUS
        || ct.kind == Token.MINUS
        || ct.kind == Token.NOT) {
      accept();
      parse_unary_expr();
    } else {
      syntaxError("\"%\" not expected", ct.getLexeme());
    }
  }

  public void parse_primary_expr() throws SyntaxError {
    if (ct.kind == Token.ID) {
      accept();
      if (ct.kind == Token.LEFTBRACKET) {
        accept();
        parse_expr();
        accept(Token.RIGHTBRACKET);
      } else if (first_arglist(ct.kind)) {
        parse_arglist();
      }
      // dont throw error else cuz arglist can derive to epsilon?
    } else if (ct.kind == Token.LEFTPAREN) {
      accept();
      parse_expr();
      accept(Token.RIGHTPAREN);
    } else if (ct.kind == Token.INTLITERAL
        || ct.kind == Token.BOOLLITERAL
        || ct.kind == Token.FLOATLITERAL
        || ct.kind == Token.STRINGLITERAL) {
      accept();
    } else {
      syntaxError("\"%\" not expected", ct.getLexeme());
    }
  }

  public void parse_asgnexpr() throws SyntaxError {
    accept(Token.ID);
    accept(Token.ASSIGN);
    parse_expr();
  }

  public void parse_params_list() throws SyntaxError {
    parse_parameter_decl();
    while (ct.kind == Token.COMMA) {
      accept();
      parse_parameter_decl();
    }
  }

  public void parse_parameter_decl() throws SyntaxError {
    parse_typespec();
    parse_declarator();
  }

  public void parse_arglist() throws SyntaxError {
    accept(Token.LEFTPAREN);
    if (first_args(ct.kind)) {
      parse_args();
    }
    accept(Token.RIGHTPAREN);
  }

  public void parse_args() throws SyntaxError {
    parse_expr();
    while (ct.kind == Token.COMMA) {
      accept();
      parse_expr();
    }
  }
}
