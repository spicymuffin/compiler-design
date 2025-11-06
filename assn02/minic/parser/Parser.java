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
  private Token currentToken;

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

  // accept() checks whether the current token matches tokenExpected.
  // If so, it fetches the next token.
  // If not, it reports a syntax error.
  void accept(int tokenExpected) throws SyntaxError {
    if (currentToken.kind == tokenExpected) {
      currentToken = scanner.scan();
    } else {
      syntaxError("\"%\" expected here", Token.spell(tokenExpected));
    }
  }

  // acceptIt() unconditionally accepts the current token
  // and fetches the next token from the scanner.
  void acceptIt() {
    currentToken = scanner.scan();
  }

  void syntaxError(String messageTemplate, String tokenQuoted) throws SyntaxError {
    SourcePos pos = currentToken.getSourcePos();
    errorReporter.reportError(messageTemplate, tokenQuoted, pos);
    throw (new SyntaxError());
  }

  boolean isTypeSpecifier(int token) {
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

    currentToken = scanner.scan(); // get first token from scanner...

    try {
      parseProgram();
      if (currentToken.kind != Token.EOF) {
        syntaxError("\"%\" not expected after end of program",
            currentToken.getLexeme());
      }
    } catch (SyntaxError s) {
      return; /* to be refined in Assignment 3... */
    }
    return;
  }

  /**
   * parseProgram is the top-level parsing routine responsible for parsing an
   * entire MiniC program.
   *
   * <p>
   * program ::= ( (VOID|INT|BOOL|FLOAT) ID ( FunPart | VarPart ) )*
   */
  public void parseProgram() throws SyntaxError {
    while (isTypeSpecifier(currentToken.kind)) {
      acceptIt();
      accept(Token.ID);
      if (currentToken.kind == Token.LEFTPAREN) {
        parseFunPart();
      } else {
        parseVarPart();
      }
    }
  }

  /**
   * parseFunPart parses the function-declaration part of a MiniC delaration.
   *
   * <p>
   * FunPart ::= ( "(" ParamsList? ")" CompoundStmt )
   */
  public void parseFunPart() throws SyntaxError {
    // We already know that the current token is "(".
    // Otherwise use accept() !
    acceptIt();
    if (isTypeSpecifier(currentToken.kind)) {
      parseParamsList();
    }
    accept(Token.RIGHTPAREN);
    parseCompoundStmt();
  }

  /**
   * parseParamsList parses the parameter declarations of a MiniC function.
   *
   * <p>
   * ParamsList ::= ParameterDecl ( "," ParameterDecl ) *
   */
  public void parseParamsList() throws SyntaxError {
    // to be completed by you...

  }

  /**
   * parseCompoundStmt parses the MiniC compound statements.
   *
   * <p>
   * CompoundStmt ::= "{" VariableDef* Stmt* "}"
   */
  public void parseCompoundStmt() throws SyntaxError {
    // to be completed by you...

  }

  /**
   * parseVarPart parses the variable-portion of a MiniC declaration.
   *
   * <p>
   * VarPart ::= ( "[" INTLITERAL "]" )? ( "=" initializer ) ? ( "," init_decl)*
   * ";"
   */
  public void parseVarPart() throws SyntaxError {
    // to be completed by you...

  }

  // to be completed by you...

}
