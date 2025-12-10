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

  private Expr parseExprList() throws SyntaxError {
    if (currentToken.kind != Token.COMMA) {
      return new EmptyExpr(previousTokenPosition);
    }

    acceptIt();

    Expr expr = parseExpr();

    return new ExprSequence(expr, parseExprList(), previousTokenPosition);
  }

  private Expr parseInitializer() throws SyntaxError {
    if (currentToken.kind == Token.LEFTBRACE) {
      acceptIt();

      Expr first = parseExpr();
      Expr e = parseExprList();
      ExprSequence seq = new ExprSequence(first, e, previousTokenPosition);
      accept(Token.RIGHTBRACE);
      return seq;
    } else {
      return parseExpr();
    }
  }

  // init-decl ::= declarator ("=" initializer)?
  private Decl parseInitDecl(Type t) throws SyntaxError {
    SourcePos pos = new SourcePos();
    start(pos);
    ID id = parseId();
    if (currentToken.kind == Token.LEFTBRACKET) {
      t = parseArrayIndexDecl(t, pos);
    }
    Expr e = new EmptyExpr(previousTokenPosition);
    if (currentToken.kind == Token.ASSIGN) {
      acceptIt();
      e = parseInitializer();
    }
    finish(pos);
    return new VarDecl(t, id, e, pos);
  }

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

  /**
   * parseVarPart(): parses variable declaration past the ID.
   *
   * <p>
   * VarPart ::= ( "[" INTLITERAL "]" )? ( "=" initializer ) ? ( "," init_decl)*
   * ";"
   */
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
      e = parseInitializer();
    }
    finish(pos);
    Decl d = new VarDecl(theType, id, e, pos);
    DeclSequence seq = null;
    // You can use the following code after you have implemented
    // parseInitDeclList():
    seq = new DeclSequence(d, parseInitDeclList(t), previousTokenPosition);
    accept(Token.SEMICOLON);
    return seq;
  }

  private Expr parseExpr() throws SyntaxError {
    SourcePos pos = new SourcePos();
    start(pos);
    Expr left = parseAndExpr();
    while (currentToken.kind == Token.OR) {
      Operator op = new Operator(currentToken.getLexeme(), currentToken.getSourcePos());
      acceptIt();
      Expr right = parseAndExpr();
      finish(pos);
      left = new BinaryExpr(left, op, right, pos);
    }
    return left;
  }

  private Expr parseAndExpr() throws SyntaxError {
    SourcePos pos = new SourcePos();
    start(pos);
    Expr left = parseRelationalExpr();
    while (currentToken.kind == Token.AND) {
      Operator op = new Operator(currentToken.getLexeme(), currentToken.getSourcePos());
      acceptIt();
      Expr right = parseRelationalExpr();
      finish(pos);
      left = new BinaryExpr(left, op, right, pos);
    }
    return left;
  }

  private Expr parseRelationalExpr() throws SyntaxError {
    SourcePos pos = new SourcePos();
    start(pos);
    Expr left = parseAddExpr();
    if (currentToken.kind == Token.EQ
        || currentToken.kind == Token.NOTEQ
        || currentToken.kind == Token.LESS
        || currentToken.kind == Token.LESSEQ
        || currentToken.kind == Token.GREATER
        || currentToken.kind == Token.GREATEREQ) {
      Operator op = new Operator(currentToken.getLexeme(), currentToken.getSourcePos());
      acceptIt();
      Expr right = parseAddExpr();
      finish(pos);
      left = new BinaryExpr(left, op, right, pos);
    }
    return left;
  }

  private Expr parseAddExpr() throws SyntaxError {
    SourcePos pos = new SourcePos();
    start(pos);
    Expr left = parseMultExpr();
    while (currentToken.kind == Token.PLUS
        || currentToken.kind == Token.MINUS) {
      Operator op = new Operator(currentToken.getLexeme(), currentToken.getSourcePos());
      acceptIt();
      Expr right = parseMultExpr();
      finish(pos);
      left = new BinaryExpr(left, op, right, pos);
    }
    return left;
  }

  private Expr parseMultExpr() throws SyntaxError {
    SourcePos pos = new SourcePos();
    start(pos);
    Expr left = parseUnaryExpr();
    while (currentToken.kind == Token.TIMES
        || currentToken.kind == Token.DIV) {
      Operator op = new Operator(currentToken.getLexeme(), currentToken.getSourcePos());
      acceptIt();
      Expr right = parseUnaryExpr();
      finish(pos);
      left = new BinaryExpr(left, op, right, pos);
    }
    return left;
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

    if (currentToken.kind == Token.ID) {
      SourcePos pos = new SourcePos();
      start(pos);

      ID id = new ID(currentToken.getLexeme(), currentToken.getSourcePos());
      VarExpr idExpr = new VarExpr(id, currentToken.getSourcePos());
      acceptIt();
      if (currentToken.kind == Token.LEFTBRACKET) {
        acceptIt();
        Expr index = parseExpr();
        accept(Token.RIGHTBRACKET);
        finish(pos);
        retExpr = new ArrayExpr(idExpr, index, pos);
      } else if (currentToken.kind == Token.LEFTPAREN) {
        Expr args = parseArgList();
        finish(pos);
        retExpr = new CallExpr(id, args, pos);
      } else {
        retExpr = idExpr;
      }
    } else {
      if (currentToken.kind == Token.LEFTPAREN) {
        acceptIt();
        retExpr = parseExpr();
        accept(Token.RIGHTPAREN);
      } else {
        switch (currentToken.kind) {
          case Token.INTLITERAL:
            retExpr = new IntExpr(
                new IntLiteral(currentToken.getLexeme(),
                    currentToken.getSourcePos()),
                currentToken.getSourcePos());
            acceptIt();
            break;
          case Token.BOOLLITERAL:
            retExpr = new BoolExpr(
                new BoolLiteral(currentToken.getLexeme(),
                    currentToken.getSourcePos()),
                currentToken.getSourcePos());
            acceptIt();
            break;
          case Token.FLOATLITERAL:
            retExpr = new FloatExpr(
                new FloatLiteral(currentToken.getLexeme(),
                    currentToken.getSourcePos()),
                currentToken.getSourcePos());
            acceptIt();
            break;
          case Token.STRINGLITERAL:
            retExpr = new StringExpr(
                new StringLiteral(currentToken.getLexeme(),
                    currentToken.getSourcePos()),
                currentToken.getSourcePos());
            acceptIt();
            break;
          default:
            syntaxError("Primary expression expected",
                currentToken.getLexeme());
        }
      }
    }
    return retExpr;
  }

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

  private Stmt parseStmt() throws SyntaxError {
    SourcePos pos = new SourcePos();
    start(pos);

    if (currentToken.kind == Token.LEFTBRACE) {
      return parseCompoundStmt();
    } else if (currentToken.kind == Token.IF) {
      return parseIfStmt(pos);
    } else if (currentToken.kind == Token.WHILE) {
      return parseWhileStmt(pos);
    } else if (currentToken.kind == Token.FOR) {
      return parseForStmt(pos);
    } else if (currentToken.kind == Token.RETURN) {
      acceptIt();
      if (currentToken.kind == Token.SEMICOLON) {
        acceptIt();
        finish(pos);
        return new ReturnStmt(new EmptyExpr(previousTokenPosition), pos);
      } else {
        Expr retExpr = parseExpr();
        accept(Token.SEMICOLON);
        finish(pos);
        return new ReturnStmt(retExpr, pos);
      }
    } else if (currentToken.kind == Token.ID) {
      ID id = new ID(currentToken.getLexeme(), currentToken.getSourcePos());
      VarExpr idExpr = new VarExpr(id, currentToken.getSourcePos());
      acceptIt();
      if (currentToken.kind == Token.LEFTPAREN) {
        Expr args = parseArgList();
        accept(Token.SEMICOLON);
        finish(pos);
        return new CallStmt(new CallExpr(id, args, pos), pos);
      } else if (currentToken.kind == Token.LEFTBRACKET) {
        acceptIt();
        Expr index = parseExpr();
        accept(Token.RIGHTBRACKET);
        accept(Token.ASSIGN);
        Expr value = parseExpr();
        accept(Token.SEMICOLON);
        finish(pos);
        return new AssignStmt(new ArrayExpr(idExpr, index, pos), value, pos);
      } else if (currentToken.kind == Token.ASSIGN) {
        acceptIt();
        Expr value = parseExpr();
        accept(Token.SEMICOLON);
        finish(pos);
        return new AssignStmt(idExpr, value, pos);
      } else {
        syntaxError("Statement expected", currentToken.getLexeme());
        return null;
      }
    } else {
      syntaxError("Statement expected", currentToken.getLexeme());
      return null;
    }
  }

  private Stmt parseIfStmt(SourcePos pos) throws SyntaxError {
    acceptIt();
    accept(Token.LEFTPAREN);
    Expr cond = parseExpr();
    accept(Token.RIGHTPAREN);
    Stmt thenStmt = parseStmt();
    if (currentToken.kind == Token.ELSE) {
      acceptIt();
      Stmt elseStmt = parseStmt();
      finish(pos);
      return new IfStmt(cond, thenStmt, elseStmt, pos);
    } else {
      finish(pos);
      return new IfStmt(cond, thenStmt, pos);
    }
  }

  private Stmt parseWhileStmt(SourcePos pos) throws SyntaxError {
    acceptIt();
    accept(Token.LEFTPAREN);
    Expr cond = parseExpr();
    accept(Token.RIGHTPAREN);
    Stmt body = parseStmt();
    finish(pos);
    return new WhileStmt(cond, body, pos);
  }

  private Stmt parseForStmt(SourcePos pos) throws SyntaxError {
    acceptIt();

    Expr init = new EmptyExpr(previousTokenPosition);
    Expr cond = new EmptyExpr(previousTokenPosition);
    Expr update = new EmptyExpr(previousTokenPosition);

    accept(Token.LEFTPAREN);
    if (currentToken.kind == Token.SEMICOLON) {
      acceptIt();
    } else {
      ID id = new ID(currentToken.getLexeme(), currentToken.getSourcePos());
      VarExpr idExpr = new VarExpr(id, currentToken.getSourcePos());
      accept(Token.ID);
      accept(Token.ASSIGN);
      Expr initExpr = parseExpr();
      init = new AssignExpr(idExpr, initExpr, previousTokenPosition);
      accept(Token.SEMICOLON);
    }

    if (currentToken.kind == Token.SEMICOLON) {
      acceptIt();
    } else {
      cond = parseExpr();
      accept(Token.SEMICOLON);
    }

    if (currentToken.kind == Token.RIGHTPAREN) {
      acceptIt();
    } else {
      ID id = new ID(currentToken.getLexeme(), currentToken.getSourcePos());
      VarExpr idExpr = new VarExpr(id, currentToken.getSourcePos());
      accept(Token.ID);
      accept(Token.ASSIGN);
      Expr updateExpr = parseExpr();
      update = new AssignExpr(idExpr, updateExpr, previousTokenPosition);
      accept(Token.RIGHTPAREN);
    }

    Stmt body = parseStmt();
    finish(pos);
    return new ForStmt(init, cond, update, body, pos);
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
    st = parseStmt();
    Stmt stRest = parseCompoundStmts();
    finish(pos);
    return new StmtSequence(st, stRest, pos);
  }

  /**
   * parseCompoundStmt(): parses a MiniC compound statement.
   *
   * <p>
   * CompoundStmt ::= "{" VariableDef* Stmt* "}"
   */
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

    param = parseExpr();
    finish(pos);
    params = new ActualParam(param, pos);
    if (currentToken.kind == Token.COMMA) {
      // Comma case:
      acceptIt();
      restargs = parseArgs();
      if (restargs instanceof EmptyActualParam) {
        syntaxError("Argument after comma expected", "");
      }
    } else {
      // No comma case:
      restargs = parseArgs();
      if (!(restargs instanceof EmptyActualParam)) {
        syntaxError("Comma between preceeding arguments expected", "");
      }
    }
    finish(pos);

    return new ActualParamSequence(params, restargs, pos);
  }

  /**
   * parseArgList() parses a MiniC procedure arg list.
   *
   * <p>
   * ArgList ::= "(" ( arg ( "," arg )* )? ")"
   */
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
