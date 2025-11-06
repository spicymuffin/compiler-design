package minic;

import minic.parser.Parser;
import minic.scanner.Scanner;
import minic.scanner.SourceFile;
import minic.scanner.Token;

/**
 * Class with the main method of the MiniC compiler.
 *
 * <p>Represents the compiler driver that initializes the compiler's infra- structure such as the
 * character stream from the source file and then conducts the compilation phases (lexing, parsing,
 * semantic analysis and code generation.
 */
public class MiniC {

  private static Scanner scanner;
  private static Parser parser;
  private static ErrorReporter reporter;

  static void compileProgram(String sourceName) {

    System.out.println("********** MiniC Compiler **********");

    System.out.println("Syntax Analysis ...");

    SourceFile source = new SourceFile(sourceName);

    Token t;
    scanner = new Scanner(source);
    /*
     * Enable this to observe the sequence of tokens
     * delivered by the scanner:
     *
     */
    // scanner.enableDebugging();

    reporter = new ErrorReporter();
    parser = new Parser(scanner, reporter);
    parser.parse();

    /*
     * The following loop was used with the first assignment
     * to repeatedly request tokens from the scanner.
     * The above call to parser.parse() has replaced it
     * with Assignment 2.
     *
    do {
      t = scanner.scan(); // scan 1 token
    } while (t.kind != Token.EOF);
    */

    boolean successful = (reporter.numErrors == 0);
    if (successful) {
      System.out.println("Compilation was successful.");
    } else {
      System.out.println("Compilation was unsuccessful.");
    }
  }

  /** Main method, which is the entry point when the MiniC compiler is run. */
  public static void main(String[] args) {

    if (args.length != 1) {
      System.out.println("Usage: MiniC filename");
      System.exit(1);
    }

    String sourceName = args[0];
    compileProgram(sourceName);
  }
}
