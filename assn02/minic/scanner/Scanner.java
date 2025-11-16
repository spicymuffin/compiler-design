package minic.scanner;

import java.util.HashMap;

/**
 * Implements the scanner with functionality to provide the next token in the
 * input.
 */
public final class Scanner {

  private SourceFile sourceFile;

  private final int MAX_LOOKAHEAD = 5;
  private char[] lookaheadQueue = new char[MAX_LOOKAHEAD];
  private int laq_head_idx;
  private int laq_sz;

  private char currentChar;
  private boolean verbose;
  private StringBuffer currentLexeme;
  private boolean currentlyScanningToken;
  private boolean currentlyScanningMultiLineComment;
  private int currentLineNr;
  private int currentColNr;

  private HashMap<String, Integer> wordMap = new HashMap<String, Integer>();

  private boolean isDigit(char c) {
    return (c >= '0' && c <= '9');
  }

  private boolean isValidIDInitializer(char c) {
    if (c == '_')
      return true;
    c |= 32;
    return (c >= 'a' && c <= 'z');
  }

  // private boolean isLetter(char c) {
  //   c |= 32;
  //   return (c >= 'a' && c <= 'z');
  // }

  private int laq_idx(int off) {
    int m = off % MAX_LOOKAHEAD;
    return (m < 0) ? m + MAX_LOOKAHEAD : m;
  }

  private int laq_tail_index() {
    return laq_idx(laq_head_idx - laq_sz + 1);
  }

  private void laq_queue(char c) {
    if (laq_sz == MAX_LOOKAHEAD)
      throw new RuntimeException("queue full");
    laq_head_idx = laq_idx(laq_head_idx + 1);
    lookaheadQueue[laq_head_idx] = c;
    laq_sz++;
  }

  private char laq_dequeue() {
    if (laq_sz == 0)
      throw new RuntimeException("queue empty");
    int tail = laq_tail_index();
    char c = lookaheadQueue[tail];
    laq_sz--;
    return c;
  }

  private char laq_peek(int n) {
    if (n < 0 || n >= laq_sz)
      throw new RuntimeException("out of bounds");
    int tail = laq_tail_index();
    return lookaheadQueue[laq_idx(tail + n)];
  }
  ///////////////////////////////////////////////////////////////////////////////

  /**
   * Constructs the scanner object.
   *
   * @param source the source code buffer.
   */
  public Scanner(
      SourceFile source) {
    sourceFile = source;
    currentChar = sourceFile.readChar();
    verbose = false;
    currentLineNr = 1;
    currentColNr = 1;

    // initialize wordmap

    // bool literals
    wordMap.put("true", Token.BOOLLITERAL);
    wordMap.put("false", Token.BOOLLITERAL);

    // keywords
    wordMap.put("bool", Token.BOOL);
    wordMap.put("else", Token.ELSE);
    wordMap.put("float", Token.FLOAT);
    wordMap.put("for", Token.FOR);
    wordMap.put("if", Token.IF);
    wordMap.put("int", Token.INT);
    wordMap.put("return", Token.RETURN);
    wordMap.put("void", Token.VOID);
    wordMap.put("while", Token.WHILE);
  }

  /** Ask scanner to emit debug output for every token. */
  public void enableDebugging() {
    verbose = true;
  }

  // takeIt appends the current character to the current token, and gets
  // the next character from the source program (or the to-be-implemented
  // "untake" buffer in case of look-ahead characters that got 'pushed back'
  // into the input stream).

  private void take() {
    currentColNr++;
    if (currentlyScanningToken) {
      currentLexeme.append(currentChar);
    }
    if (laq_sz == 0) {
      currentChar = sourceFile.readChar();
    } else {
      currentChar = laq_dequeue();
    }
  }

  private char peek(int n) {
    if (n == 0) {
      return currentChar;
    }

    if (laq_sz < n) {
      for (int i = 0; i < n - laq_sz; i++) {
        laq_queue(sourceFile.readChar());
      }
    }

    return laq_peek(n - 1);
  }

  private int scanToken() {

    switch (currentChar) {
      // operators
      case '+':
        take();
        return Token.PLUS;
      case '-':
        take();
        return Token.MINUS;
      case '*':
        take();
        return Token.TIMES;
      case '/':
        take();
        return Token.DIV;
      case '=':
        take();
        if (currentChar == '=') {
          take();
          return Token.EQ;
        }
        return Token.ASSIGN;
      case '!':
        take();
        if (currentChar == '=') {
          take();
          return Token.NOTEQ;
        }
        return Token.NOT;
      case '<':
        take();
        if (currentChar == '=') {
          take();
          return Token.LESSEQ;
        }
        return Token.LESS;
      case '>':
        take();
        if (currentChar == '=') {
          take();
          return Token.GREATEREQ;
        }
        return Token.GREATER;
      case '|':
        take();
        if (currentChar == '|') {
          take();
          return Token.OR;
        }
      case '&':
        take();
        if (currentChar == '&') {
          take();
          return Token.AND;
        }
        return Token.ERROR;

      // literals
      case '.':
        // take '.', but this can still be an error
        take();
        if (isDigit(currentChar)) {
          // we are in a float officially, but we can still expand it or leave if
          // lookahead determines so
          take();
          if (currentChar == 'e' || currentChar == 'E') {
            // we need to peek for + and at least another digit
            if ((peek(1) == '+' || peek(1) == '-') && isDigit(peek(2))) {
              take(); // take the 'e' or 'E'
              take(); // take the '+' or '-'
              while (isDigit(currentChar)) {
                // if we are here, that means the float is expanding with 'e' or 'E'
                take();
              }
              return Token.FLOATLITERAL;
            } else if (isDigit(peek(1))) {
              take(); // take the 'e' or 'E'
              while (isDigit(currentChar)) {
                // if we are here, that means the float is expanding with 'e' or 'E'
                take();
              }
              return Token.FLOATLITERAL;
            } else {
              // e is an ID, the float ended in the previous character
              return Token.FLOATLITERAL;
            }
          } else {
            while (isDigit(currentChar)) {
              // if we are here, that means the float is expanding without 'e' or 'E'
              take();
            }
            return Token.FLOATLITERAL;
          }
        } else {
          return Token.ERROR;
        }
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
        take();
        while (isDigit(currentChar)) {
          take();
        }

        if (currentChar == 'e' || currentChar == 'E') {
          if (isDigit(peek(1))) {
            take(); // take the 'e' or 'E'
            while (isDigit(currentChar)) {
              // if we are here, that means the float is expanding with 'e' or 'E'
              take();
            }
            return Token.FLOATLITERAL;
          } else {
            // e is an ID, the float ended in the previous character
            return Token.FLOATLITERAL;
          }
        }

        if (currentChar == '.') {
          // take '.', but this can still be an error
          take();
          if (isDigit(currentChar)) {
            // we are in a float officially, but we can still expand it or leave if
            // lookahead determines so
            take();
            if (currentChar == 'e' || currentChar == 'E') {
              // we need to peek for + and at least another digit
              if ((peek(1) == '+' || peek(1) == '-') && isDigit(peek(2))) {
                take(); // take the 'e' or 'E'
                take(); // take the '+' or '-'
                while (isDigit(currentChar)) {
                  // if we are here, that means the float is expanding with 'e' or 'E'
                  take();
                }
                return Token.FLOATLITERAL;
              } else if (isDigit(peek(1))) {
                take(); // take the 'e' or 'E'
                while (isDigit(currentChar)) {
                  // if we are here, that means the float is expanding with 'e' or 'E'
                  take();
                }
                return Token.FLOATLITERAL;
              } else {
                // e is an ID, the float ended in the previous character
                return Token.FLOATLITERAL;
              }
            } else {
              while (isDigit(currentChar)) {
                // if we are here, that means the float is expanding without 'e' or 'E'
                take();
              }
              return Token.FLOATLITERAL;
            }
          }
          // float of form 4.
          return Token.FLOATLITERAL;
        }

        return Token.INTLITERAL;

      case '"':
        take();
        while (currentChar != '"') {
          if (currentChar == '\\') {
            take(); // take \
            if (currentChar != 'n') {
              System.out.println("ERROR: illegal escape sequence");
            }
          }
          if (currentChar == '\n') {
            System.out.println("ERROR: unterminated string literal");
            currentLexeme.append('"');
            return Token.STRINGLITERAL;
          }
          take();
        }
        // take the closing '"'
        take();
        return Token.STRINGLITERAL;

      // punctuation
      case '{':
        take();
        return Token.LEFTBRACE;
      case '}':
        take();
        return Token.RIGHTBRACE;
      case '[':
        take();
        return Token.LEFTBRACKET;
      case ']':
        take();
        return Token.RIGHTBRACKET;
      case '(':
        take();
        return Token.LEFTPAREN;
      case ')':
        take();
        return Token.RIGHTPAREN;
      case ',':
        take();
        return Token.COMMA;
      case ';':
        take();
        return Token.SEMICOLON;

      // meta
      case SourceFile.EOF:
        currentLexeme.append('$');
        currentColNr++;
        return Token.EOF;

      // ids, keywords, bool literals and maybe error
      default:
        // is an ID, i guess
        if (isValidIDInitializer(currentChar)) {
          take();
          while (isValidIDInitializer(currentChar) || isDigit(currentChar)) {
            take();
          }

          // before returning, check if the thing we parsed is an keyword
          Integer keyword = wordMap.get(currentLexeme.toString());
          if (keyword != null) {
            return (int) keyword;
          }

          return Token.ID;
        }
        take();
        return Token.ERROR;
    }
  }

  /** Scans the next token. */
  public Token scan() {

    currentlyScanningToken = false;
    while (currentChar == ' '
        || currentChar == '\f'
        || currentChar == '\n'
        || currentChar == '\r'
        || currentChar == '\t'
        || (peek(0) == '/' && peek(1) == '/')
        || (peek(0) == '/' && peek(1) == '*')
        || currentlyScanningMultiLineComment) {
      if (peek(0) == '/' && peek(1) == '/' && !currentlyScanningMultiLineComment) {
        take();
        take();
        while (currentChar != '\n') {
          take();
        }
        continue;
      }
      if (peek(0) == '/' && peek(1) == '*' && !currentlyScanningMultiLineComment) {
        take();
        take();
        currentlyScanningMultiLineComment = true;
        continue;
      }
      if (currentlyScanningMultiLineComment) {
        if (currentChar == SourceFile.EOF) {
          System.out.println("ERROR: unterminated multi-line comment.");
          currentlyScanningMultiLineComment = false;
          break;
        }
        if (peek(0) == '*' && peek(1) == '/') {
          take();
          take();
          currentlyScanningMultiLineComment = false;
          continue;
        }
      }
      if (currentChar == '\n') {
        currentColNr = 0;
        currentLineNr++;
      }
      take();
    }

    currentlyScanningToken = true;
    currentLexeme = new StringBuffer("");
    SourcePos pos = new SourcePos();

    pos.startCol = currentColNr;
    pos.startLine = currentLineNr;
    pos.endLine = currentLineNr;
    int kind = scanToken();
    pos.endCol = currentColNr - 1;
    Token currentToken = kind == Token.STRINGLITERAL
        ? new Token(kind, currentLexeme.substring(1, currentLexeme.length() - 1), pos)
        : new Token(kind, currentLexeme.toString(), pos);
    if (verbose) {
      currentToken.print();
    }
    return currentToken;
  }
}
