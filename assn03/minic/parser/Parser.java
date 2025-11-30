package minic.parser;

import minic.ErrorReporter;
import minic.astgen.*;
import minic.parser.SyntaxError;
import minic.scanner.Scanner;
import minic.scanner.SourcePos;
import minic.scanner.Token;

/** Parser class to perform syntax analysis of a MiniC program. */

public class Parser {

  private Scanner scanner;
  private ErrorReporter errorReporter;
  private Token currentToken;
  private SourcePos previousTokenPosition;

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
  private void accept(int tokenExpected) throws SyntaxError {
    if (currentToken.kind == tokenExpected) {
      previousTokenPosition = currentToken.getSourcePos();
      currentToken = scanner.scan();
    } else {
      syntaxError("\"%\" expected here", Token.spell(tokenExpected));
    }
  }

  // acceptIt() unconditionally accepts the current token
  // and fetches the next token from the scanner.
  private void acceptIt() {
    previousTokenPosition = currentToken.getSourcePos();
    currentToken = scanner.scan();
  }

  // start records the position of the start of a phrase.
  // This is defined to be the position of the first
  // character of the first token of the phrase.
  private void start(SourcePos pos) {
    pos.startCol = currentToken.getSourcePos().startCol;
    pos.startLine = currentToken.getSourcePos().startLine;
  }

  // finish records the position of the end of a phrase.
  // This is defined to be the position of the last
  // character of the last token of the phrase.
  private void finish(SourcePos pos) {
    pos.endCol = previousTokenPosition.endCol;
    pos.endLine = previousTokenPosition.endLine;
  }

  private void syntaxError(String messageTemplate, String tokenQuoted) throws SyntaxError {
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
   * parseArrayIndexDecl(): helper to take [INTLITERAL] and generate an
   * ArrayType.
   */
  private ArrayType parseArrayIndexDecl(Type t, SourcePos allPos) throws SyntaxError {
    accept(Token.LEFTBRACKET);
    SourcePos pos = currentToken.getSourcePos();
    IntLiteral l;
    l = new IntLiteral(currentToken.getLexeme(), pos);
    accept(Token.INTLITERAL);
    accept(Token.RIGHTBRACKET);
    finish(pos);
    finish(allPos);
    IntExpr ie = new IntExpr(l, pos);
    return new ArrayType(t, ie, allPos);
  }

  /**
   * parse(): public-facing top-level parsing routine.
   */
  public Program parse() { // called from the MiniC driver

    Program progAst = null;

    previousTokenPosition = new SourcePos();
    previousTokenPosition.startLine = 0;
    previousTokenPosition.startCol = 0;
    previousTokenPosition.endLine = 0;
    previousTokenPosition.endCol = 0;

    currentToken = scanner.scan(); // get first token from scanner...

    try {
      progAst = parseProgram();
      if (currentToken.kind != Token.EOF) {
        syntaxError("\"%\" not expected after end of program",
            currentToken.getLexeme());
      }
    } catch (SyntaxError s) {
      return null;
    }
    return progAst;
  }

  /**
   * parseProgram(): parses the entire MiniC program.
   *
   * <p>
   * program ::= ( (VOID|INT|BOOL|FLOAT) ID ( FunPart | VarPart ) )*
   */

  // parseProgDecls: recursive helper function to facilitate AST construction.
  private Decl parseProgDecls() throws SyntaxError {
    if (!isTypeSpecifier(currentToken.kind)) {
      return new EmptyDecl(previousTokenPosition);
    }
    SourcePos pos = new SourcePos();
    start(pos);
    Type t = parseTypeSpecifier();
    ID id = parseId();
    if (currentToken.kind == Token.LEFTPAREN) {
      Decl newD = parseFunPart(t, id, pos);
      return new DeclSequence(newD, parseProgDecls(), previousTokenPosition);
    } else {
      DeclSequence vars = parseVarPart(t, id, pos);
      DeclSequence varsTail = vars.GetRightmostDeclSequenceNode();
      Decl remainderDecls = parseProgDecls();
      varsTail.SetRightSubtree(remainderDecls);
      return vars;
    }
  }

  private Program parseProgram() throws SyntaxError {
    SourcePos pos = new SourcePos();
    start(pos);
    Decl d = parseProgDecls();
    finish(pos);
    Program p = new Program(d, pos);
    return p;
  }

  /**
   * parseFunPart(): parses the ``function'' part of a declaration.
   *
   * <p>
   * FunPart ::= ( "(" ParamsList? ")" CompoundStmt )
   */
  private Decl parseFunPart(Type t, ID id, SourcePos pos) throws SyntaxError {
    // We already know that the current token is "(".
    // Otherwise use accept()
    acceptIt();
    Decl parDecl = parseParamsList(); // can also be empty...
    accept(Token.RIGHTPAREN);
    CompoundStmt compStmt = parseCompoundStmt();
    finish(pos);
    return new FunDecl(t, id, parDecl, compStmt, pos);
  }

  /**
   * parseParamsList(): parses the parameter declarations of a function.
   *
   * <p>
   * ParamsList ::= ParameterDecl ( "," ParameterDecl ) *
   */
  private Decl parseParamsList() throws SyntaxError {
    if (!isTypeSpecifier(currentToken.kind)) {
      return new EmptyFormalParamDecl(previousTokenPosition);
    }
    Decl decl1 = parseParameterDecl();
    Decl declR = new EmptyFormalParamDecl(previousTokenPosition);
    if (currentToken.kind == Token.COMMA) {
      acceptIt();
      declR = parseParamsList();
      if (declR instanceof EmptyFormalParamDecl) {
        syntaxError("Declaration after comma expected", "");
      }
    }
    return new FormalParamDeclSequence(decl1, declR, previousTokenPosition);
  }

  /**
   * parseParameterDecl(): parses a MiniC parameter declaration.
   *
   * <p>
   * ParameterDecl ::= (VOID|INT|BOOL|FLOAT) Declarator
   */
  private Decl parseParameterDecl() throws SyntaxError {
    Type t = null;
    Decl d = null;

    SourcePos pos = new SourcePos();
    start(pos);
    if (isTypeSpecifier(currentToken.kind)) {
      t = parseTypeSpecifier();
    } else {
      syntaxError("Type specifier instead of % expected",
          Token.spell(currentToken.kind));
    }
    d = parseDeclarator(t, pos);
    return d;
  }

  /**
   * parseDeclarator(): parses the declarator part of a declaration.
   *
   * <p>
   * Declarator ::= ID ( "[" INTLITERAL "]" )?
   */
  private Decl parseDeclarator(Type t, SourcePos pos) throws SyntaxError {
    ID id = parseId();
    if (currentToken.kind == Token.LEFTBRACKET) {
      ArrayType arrT = parseArrayIndexDecl(t, pos);
      finish(pos);
      return new FormalParamDecl(arrT, id, pos);
    }
    finish(pos);
    return new FormalParamDecl(t, id, pos);
  }

  /**
   * parseVarPart(): parses variable declaration past the ID.
   *
   * <p>
   * VarPart ::= ( "[" INTLITERAL "]" )? ( "=" initializer ) ? ( "," init_decl)*
   * ";"
   */

  // Recursive helper method to parse ( "," init_decl)*
  private Decl parseInitDeclList(Type t) throws SyntaxError {
    if (currentToken.kind != Token.COMMA) {
      return new EmptyDecl(previousTokenPosition);
    }
    // You can use the following code after implementation of parseInitDecl():
    acceptIt(); // Token.Comma
    // Parse the next variable declaration:
    Decl d = parseInitDecl(t);
    // Return a DeclSequence node:
    // - The left child is the parsed declaration d.
    // - The right child is the DeclSequence node returned from the
    // recursive call to parseInitDeclList().
    return new DeclSequence(d, parseInitDeclList(t), previousTokenPosition);
  }

  private DeclSequence parseVarPart(Type t, ID id, SourcePos pos) throws SyntaxError {
    Type theType = t;
    Expr e = new EmptyExpr(previousTokenPosition);
    if (currentToken.kind == Token.LEFTBRACKET) {
      theType = parseArrayIndexDecl(t, pos);
    }
    if (currentToken.kind == Token.ASSIGN) {
      acceptIt();
      // You can use the following code after you have implemented
      // parseInitializer():
      // e = parseInitializer();
    }
    finish(pos);
    Decl d = new VarDecl(theType, id, e, pos);
    DeclSequence seq = null;
    // You can use the following code after you have implemented
    // parseInitDeclList():
    // seq = new DeclSequence(d, parseInitDeclList(t), previousTokenPosition);
    accept(Token.SEMICOLON);
    return seq;
  }

  /**
   * parseUnaryExpr(): parses a MiniC unary expression.
   *
   * <p>
   * UnaryExpr ::= ("+"|"-"|"!")* PrimaryExpr
   */
  private Expr parseUnaryExpr() throws SyntaxError {
    SourcePos pos = new SourcePos();
    start(pos);
    if (currentToken.kind == Token.PLUS
        || currentToken.kind == Token.MINUS
        || currentToken.kind == Token.NOT) {
      Operator opAst = new Operator(currentToken.getLexeme(),
          currentToken.getSourcePos());
      acceptIt();
      Expr tmp = parseUnaryExpr();
      finish(pos);
      return new UnaryExpr(opAst, tmp, pos);
    }
    return parsePrimaryExpr();
  }

  /**
   * parsePrimaryExpr(): parses a MiniC primary expression.
   *
   * <p>
   * PrimaryExpr ::= ID arglist?
   * | ID "[" expr "]"
   * | "(" expr ")"
   * | INTLITERAL | BOOLLITERAL | FLOATLITERAL | STRINGLITERAL
   */
  private Expr parsePrimaryExpr() throws SyntaxError {
    Expr retExpr = null;

    // your code goes here...

    return retExpr;
  }

  /**
   * parseCompoundStmt(): parses a MiniC compound statement.
   *
   * <p>
   * CompoundStmt ::= "{" VariableDef* Stmt* "}"
   */

  // Recursive helper function parseCompoundDecls():
  private Decl parseCompoundDecls() throws SyntaxError {
    if (!isTypeSpecifier(currentToken.kind)) {
      return new EmptyDecl(previousTokenPosition);
    }
    SourcePos pos = new SourcePos();
    start(pos);
    Type t = parseTypeSpecifier();
    ID id = parseId();
    DeclSequence vars = parseVarPart(t, id, pos);
    DeclSequence varsTail = vars.GetRightmostDeclSequenceNode();
    Decl remainderDecls = parseCompoundDecls();
    varsTail.SetRightSubtree(remainderDecls);
    return vars;
  }

  // Recursive helper function parseCompoundStmts():
  private Stmt parseCompoundStmts() throws SyntaxError {
    if (!(currentToken.kind == Token.LEFTBRACE
        || currentToken.kind == Token.IF
        || currentToken.kind == Token.WHILE
        || currentToken.kind == Token.FOR
        || currentToken.kind == Token.RETURN
        || currentToken.kind == Token.ID)) {
      return new EmptyStmt(previousTokenPosition);
    }
    SourcePos pos = new SourcePos();
    start(pos);
    Stmt st = null;
    // You can use the following code after implementation of parseStmt():
    // st = parseStmt();
    Stmt stRest = parseCompoundStmts();
    finish(pos);
    return new StmtSequence(st, stRest, pos);
  }

  private CompoundStmt parseCompoundStmt() throws SyntaxError {
    SourcePos pos = new SourcePos();
    start(pos);
    accept(Token.LEFTBRACE);
    Decl d = parseCompoundDecls();
    Stmt s = parseCompoundStmts();
    accept(Token.RIGHTBRACE);
    finish(pos);
    if ((d.getClass() == EmptyDecl.class)
        && (s.getClass() == EmptyStmt.class)) {
      return new EmptyCompoundStmt(previousTokenPosition);
    } else {
      return new CompoundStmt(d, s, pos);
    }
  }

  /**
   * parseArgList() parses a MiniC procedure arg list.
   *
   * <p>
   * ArgList ::= "(" ( arg ( "," arg )* )? ")"
   */

  // Recursive helper function to parse args:
  private Expr parseArgs() throws SyntaxError {
    if (currentToken.kind == Token.RIGHTPAREN) {
      return new EmptyActualParam(previousTokenPosition);
    }
    SourcePos pos = new SourcePos();
    start(pos);
    Expr param = null;
    Expr params = null;
    Expr restargs = null;
    //
    // You can use the following code after you have implemented parseExpr() aso.:
    /*
     * param = parseExpr();
     * finish(pos);
     * params = new ActualParam(param, pos);
     * if (currentToken.kind == Token.COMMA) {
     * // Comma case:
     * acceptIt();
     * restargs = parseArgs();
     * if (restargs instanceof EmptyActualParam) {
     * syntaxError("Argument after comma expected", "");
     * }
     * } else {
     * // No comma case:
     * restargs = parseArgs();
     * if (!(restargs instanceof EmptyActualParam)) {
     * syntaxError("Comma between preceeding arguments expected", "");
     * }
     * }
     * finish(pos);
     */
    return new ActualParamSequence(params, restargs, pos);
  }

  private Expr parseArgList() throws SyntaxError {
    accept(Token.LEFTPAREN);
    Expr params = parseArgs();
    accept(Token.RIGHTPAREN);
    return params;
  }

  /**
   * parseId() parses a MiniC identifier.
   *
   * <p>
   * ID (terminal)
   */
  private ID parseId() throws SyntaxError {
    ID id = new ID(currentToken.getLexeme(), currentToken.getSourcePos());
    accept(Token.ID);
    return id;
  }

  /**
   * parseTypeSpecifier() parses a MiniC typespecifier.
   *
   * <p>
   * VOID | INT | FLOAT | BOOL (all terminals)
   */
  private Type parseTypeSpecifier() throws SyntaxError {
    Type t = null;
    switch (currentToken.kind) {
      case Token.INT:
        t = new IntType(currentToken.getSourcePos());
        break;
      case Token.FLOAT:
        t = new FloatType(currentToken.getSourcePos());
        break;
      case Token.BOOL:
        t = new BoolType(currentToken.getSourcePos());
        break;
      case Token.VOID:
        t = new VoidType(currentToken.getSourcePos());
        break;
      default:
        syntaxError("Type specifier expected", "");
    }
    acceptIt();
    return t;
  }
}
