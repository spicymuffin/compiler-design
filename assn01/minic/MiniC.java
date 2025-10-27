package minic;

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

  static void compileProgram(String sourceName) {

    System.out.println("********** MiniC Compiler **********");

    System.out.println("Lexical Analysis ...");

    SourceFile source = new SourceFile(sourceName);

    Token t;
    scanner = new Scanner(source);
    scanner.enableDebugging();
    do {
      t = scanner.scan(); // scan 1 token
    } while (t.kind != Token.EOF);
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
