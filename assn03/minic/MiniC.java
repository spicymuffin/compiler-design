package minic;

import minic.astgen.Program;
import minic.parser.Parser;
import minic.scanner.Scanner;
import minic.scanner.SourceFile;
import minic.treedrawer.Drawer;
import minic.treeprinter.Printer;
import minic.unparser.Unparser;

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
  private static Drawer drawer;
  private static Printer printer;
  private static Unparser unparser;
  /* The abstract syntax tree representing
   * the source program:
   */
  private static Program ast;
  // commandline args:
  private static String sourceName;
  private static boolean DrawTree;
  private static boolean DrawTreePlusPos;
  private static boolean PrintTree;
  private static boolean UnparseTree;
  private static String PrintTreeF;
  private static String UnparseTreeF;

  static void compileProgram(String sourceName) {
    System.out.println("********** MiniC Compiler **********");

    System.out.println("Syntax Analysis ...");

    SourceFile source = new SourceFile(sourceName);

    scanner = new Scanner(source);
    /*
     * Uncomment to observe the sequence of tokens
     * delivered by the scanner:
     *
     */
    // scanner.enableDebugging();

    reporter = new ErrorReporter();
    parser = new Parser(scanner, reporter);
    drawer = new Drawer();
    printer = new Printer();
    unparser = new Unparser();
    ast = parser.parse(); // 1st pass

    boolean successful = (reporter.numErrors == 0);
    if (successful) {
      if (PrintTree) {
        printer.print(ast, PrintTreeF);
      }
      if (UnparseTree) {
        unparser.unparse(ast, UnparseTreeF);
      }
      if (DrawTree || DrawTreePlusPos) {
        drawer.draw(ast, DrawTreePlusPos);
      }
      System.out.println("Compilation was successful.");
    } else {
      System.out.println("Compilation was unsuccessful.");
    }
  }

  static void usage() {
    System.out.println("Usage: MiniC filename");
    System.out.println("Options: -ast to draw the AST");
    System.out.println("Options: -astp to draw the AST plus source pos"); 
    System.out.println("Options: -t <file> to dump the AST to <file>");
    System.out.println("Options: -u <file> to unparse the AST to <file>");
    System.exit(1);
  }

  static void processCmdLine(String[] args) {
    DrawTree = false;
    DrawTreePlusPos = false;
    PrintTree = false;
    PrintTreeF = "";
    UnparseTree = false;
    UnparseTreeF = "";
    sourceName = "";
    int argIndex = 0;
    while (argIndex < args.length) {
      if (args[argIndex].equals("-ast")) {
        DrawTree = true;
        argIndex++;
      } else if (args[argIndex].equals("-astp")) {
        DrawTreePlusPos = true;
        argIndex++;
      } else if (args[argIndex].equals("-t")) {
        PrintTree = true;
        if (args.length < argIndex + 1) {
          usage();
        } else {
          argIndex++;
          PrintTreeF = args[argIndex];
          argIndex++;
        }
      } else if (args[argIndex].equals("-u")) {
        UnparseTree = true;
        if (args.length < argIndex + 1) {
          usage();
        } else {
          argIndex++;
          UnparseTreeF = args[argIndex];
          argIndex++;
        }
      } else {
        sourceName = args[argIndex];
        argIndex++;
      }
    }
    if (sourceName.equals("")) {
      usage();
    }
  }

  /** Main method, which is the entry point when the MiniC compiler is run. */
  public static void main(String[] args) {
    processCmdLine(args);
    compileProgram(sourceName);
  }

}
