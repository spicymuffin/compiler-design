package minic.codegen;

import java.io.*;
import minic.ErrorReporter;
import minic.StdEnvironment;
import minic.astgen.*;

/** Emitter class that implements the code generator. */
public class Emitter implements Visitor {

  private ErrorReporter reporter;
  private FileWriter fstream;
  private BufferedWriter out;
  private String className;
  private int indent;
  private final int indentPerLevel = 3; // amount of indentation per level
  private Frame frame;

  // Upper bound for the maximum operand stack height for a MiniC function.
  // The actual stack height can be determined by interpreting the function's
  // bytecode. 
  private int maxOperandStackSize = 150;

  private int labelIndent;
  private boolean isMain; // true if we are generating code for "main".
  private boolean isGlobalScope; // true if we are in the outermost "global" scope.

  /** Constructor of the Emitter class. */
  public Emitter(String infile, ErrorReporter reporter) {
    try {
      this.isMain = false;
      this.isGlobalScope = true;
      this.reporter = reporter;
      labelIndent = 1;
      String outfile;
      String namepart;
      // Create output file name:
      File f = new File(infile);
      namepart = f.getName(); // strip directory part
      int l = namepart.length();
      if (namepart.charAt(l - 3) == '.'
          && namepart.charAt(l - 2) == 'm'
          && namepart.charAt(l - 1) == 'c') {
        className = new String(namepart.substring(0, l - 3));
        outfile = new String(namepart.substring(0, l - 2));
        outfile = outfile.concat("j");
      } else {
        className = new String(namepart);
        outfile = new String(namepart);
        outfile = outfile.concat(".j");
      }
      // Create output file: 
      fstream = new FileWriter(outfile);
      out = new BufferedWriter(fstream);
      indent = 0;
    } catch (Exception e) {
      // Catch exception if any:
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    }
  }

  /** Top-level routine, called by the compiler driver. */
  public void genCode(Program progAst) {
    visit(progAst);
    try {
      out.close();
    } catch (Exception e) {
      // Catch exception if any
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    }
  }

  /*
   * emit* routines output Jasmin assembly code of various sorts.
   */

  /** Method emitNoIndent(String s).
   * Emit a single string, but do not indent.
   */
  private void emitNoIndent(String s) {
    try {
      out.write(s + "\n");
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    }
  }

  /** Method emit(String s).
   * Emit single string using indentation.
   */
  private void emit(String s) {
    try {
      for (int i = 1; i <= indent * indentPerLevel; i++) {
        out.write(" ");
      }
      out.write(s + "\n");
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    }
  }

  /** Method emit(String s, int value).
   * Overloaded emit() method for string and int output.
   */
  private void emit(String s, int value) {
    emit(s + " " + value);
  }

  /** Method emit(String s, float value).
   * Overloaded emit() method for string and float output.
   */
  private void emit(String s, float value) {
    emit(s + " " + value);
  }

  /** Method getLabelString.
   * For a given label nr, return the string representation
   * of that label.
   */
  private String getLabelString(int label) {
    return new String("Label" + label);
  }

  /** Method emitLabel.
   * Emit the defining occurrence of a label.
   */
  private void emitLabel(int label) {
    assert (label >= 0);
    String ind = new String(" ");
    for (int i = 1; i <= labelIndent; i++) {
      ind = ind.concat(" ");
    }
    emitNoIndent(ind + "Label" + label + ":");
  }

  /** Method emitICONST.
   * Emit an integer constant.
   */
  private void emitICONST(int value) {
    if (value == -1) {
      emit(JVM.ICONST_M1);
    } else if (value >= 0 && value <= 5) {
      emit(JVM.ICONST + "_" + value);
    } else if (value >= -128 && value <= 127) {
      emit(JVM.BIPUSH, value);
    } else if (value >= -32768 && value <= 32767) {
      emit(JVM.SIPUSH, value);
    } else {
      emit(JVM.LDC, value);
    }
  }

  /** Method emitFCONST.
   * Emit a floating point constant.
   */
  private void emitFCONST(float value) {
    if (value == 0.0) {
      emit(JVM.FCONST_0);
    } else if (value == 1.0) {
      emit(JVM.FCONST_1);
    } else if (value == 2.0) {
      emit(JVM.FCONST_2);
    } else {
      emit(JVM.LDC, value);
    }
  }

  /** Method emitBCONST.
   * Emit a boolean constant.
   */
  private void emitBCONST(boolean value) {
    if (value) {
      emit(JVM.ICONST_1); // true = 1 with the JVM
    } else {
      emit(JVM.ICONST_0);
    }
  }

  /** Method emitILOAD.
   * Emit an integer load instruction for the given local
   * variable array slot index.
   */
  private void emitILOAD(int localVarIndex) {
    if (localVarIndex == 0) {
      emit(JVM.ILOAD_0);
    } else if (localVarIndex == 1) {
      emit(JVM.ILOAD_1);
    } else if (localVarIndex == 2) {
      emit(JVM.ILOAD_2);
    } else if (localVarIndex == 3) {
      emit(JVM.ILOAD_3);
    } else {
      emit(JVM.ILOAD, localVarIndex);
    }
  }

  /** Method emitISTORE.
   * Emit an integer store instruction for the given local
   * variable array slot index.
   */
  private void emitISTORE(int localVarIndex) {
    if (localVarIndex == 0) {
      emit(JVM.ISTORE_0);
    } else if (localVarIndex == 1) {
      emit(JVM.ISTORE_1);
    } else if (localVarIndex == 2) {
      emit(JVM.ISTORE_2);
    } else if (localVarIndex == 3) {
      emit(JVM.ISTORE_3);
    } else {
      emit(JVM.ISTORE, localVarIndex);
    }
  }

  /** Method emitFLOAD.
   * Emit a floating point load instruction for the given local
   * variable array slot index.
   */
  private void emitFLOAD(int localVarIndex) {
    if (localVarIndex == 0) {
      emit(JVM.FLOAD_0);
    } else if (localVarIndex == 1) {
      emit(JVM.FLOAD_1);
    } else if (localVarIndex == 2) {
      emit(JVM.FLOAD_2);
    } else if (localVarIndex == 3) {
      emit(JVM.FLOAD_3);
    } else {
      emit(JVM.FLOAD, localVarIndex);
    }
  }

  /** Method emitFSTORE.
   * Emit a floating point store instruction for given
   * variable index.
   */
  private void emitFSTORE(int localVarIndex) {
    if (localVarIndex == 0) {
      emit(JVM.FSTORE_0);
    } else if (localVarIndex == 1) {
      emit(JVM.FSTORE_1);
    } else if (localVarIndex == 2) {
      emit(JVM.FSTORE_2);
    } else if (localVarIndex == 3) {
      emit(JVM.FSTORE_3);
    } else {
      emit(JVM.FSTORE, localVarIndex);
    }
  }

  /** Method emitRETURN.
   * Emit a return statement of a given type t.
   */
  private void emitRETURN(Type t) {
    if (t.Tequal(StdEnvironment.intType)
        || t.Tequal(StdEnvironment.boolType)) {
      emit(JVM.IRETURN);
    } else if (t.Tequal(StdEnvironment.floatType)) {
      emit(JVM.FRETURN);
    } else if (t.Tequal(StdEnvironment.voidType)) {
      emit(JVM.RETURN);
    }
  }

  /** Method emitConstructor.
   * Emit the constructor for the class of our MiniC program.
   */
  private void emitConstructor() {
    emit("\n.method public <init>()V");
    indent++;
    emit(".limit stack 1");
    emit(".limit locals 1");
    emit(".var 0 is this L" + className + "; from Label0 to Label1\n");
    emitLabel(0);
    emit("aload_0");
    emit("invokespecial java/lang/Object/<init>()V");
    emitLabel(1);
    emit("return");
    indent--;
    emit(".end method");
  }

  /** Method emitStaticClassVariableDeclaration.
   * Emit declarations for the static class variables. Static class variables
   * correspont to MiniC global variables. This function recursively traverses
   * the declarations in the global block of the program.
   */
  private void emitStaticClassVariableDeclaration(Decl d) {
    assert (d != null);
    if (d instanceof DeclSequence) {
      DeclSequence sd = (DeclSequence) d;
      emitStaticClassVariableDeclaration(sd.D1);
      emitStaticClassVariableDeclaration(sd.D2);
    } else if (d instanceof VarDecl) {
      VarDecl vd = (VarDecl) d;
      Type t = typeOfDecl(vd);
      emit(".field static " + vd.idAST.Lexeme + " "
          + getTypeDescriptorLabel(t));         
    }
  }

  /** Method emitInitializer.
   * Emit initializers for the static class variables. This function
   * recursively traverses the declarations in the global block of the program.
   */
  private void emitInitializer(Decl d) {
    assert (d != null);
    if (d instanceof DeclSequence) {
      DeclSequence sd = (DeclSequence) d;
      emitInitializer(sd.D1);
      emitInitializer(sd.D2);
    } else if (d instanceof VarDecl) {
      VarDecl vd = (VarDecl) d;
      Type t = typeOfDecl(vd);
      Expr initExpr = vd.eAST;
      if (initExpr instanceof EmptyExpr) {
        // Programmer did not provide initializer for global variable.
        // Initialize to something safe:
        if (t.Tequal(StdEnvironment.intType)
            || t.Tequal(StdEnvironment.boolType)) {
          emit(JVM.ICONST_0);
        } else if (t.Tequal(StdEnvironment.floatType)) {
          emit(JVM.FCONST_0);
        } else {
          // Type not supported for global variable initializer:
          assert (false);
        }
      } else {
        // Programmer provided initializer expression, emit it:
        initExpr.accept(this);
      }
      emitStaticVariableReference(vd.idAST, vd.tAST, true); 
    }
  }

  /** Method emitClassInitializer.
   * Emit a class initializer method for the global MiniC variables. Global
   * MiniC variables correspond to static Java class variables in our code
   * generation model. Our MiniC assembly code needs one class initializer
   * where all class variables are initialized.
   */
  private void emitClassInitializer(Decl d) {
    emit("\n.method static <clinit>()V");
    indent++;
    emit(".limit stack 1");
    emit(".limit locals 0");
    emitInitializer(d);
    emit(JVM.RETURN);
    indent--;
    emit(".end method");
  }

  /** Method getTypeDescriptorLabel.
   * Return the JVM type descriptor for a given MiniC type t.
   */
  private String getTypeDescriptorLabel(Type t) {
    String l = new String("");
    assert ((t != null) && !(t instanceof ErrorType));
    if (t.Tequal(StdEnvironment.intType)) {
      l = new String("I");
    } else if (t.Tequal(StdEnvironment.boolType)) {
      l = new String("Z");
    } else if (t.Tequal(StdEnvironment.floatType)) {
      l = new String("F");
    } else if (t.Tequal(StdEnvironment.stringType)) {
      l = new String("Ljava/lang/String;");
    } else if (t.Tequal(StdEnvironment.voidType)) {
      l = new String("V");
    } else {
      assert (false);
    }
    return l;
  }

  /** Method typeOfDecl.
   * Return the type of the given declaration d.
   */
  private Type typeOfDecl(AST d) {
    final Type retType;
    final Type t;
    assert (d != null);
    assert ((d instanceof FunDecl) || (d instanceof VarDecl)
        || (d instanceof FormalParamDecl));
    if (d instanceof FunDecl) {
      t = ((FunDecl) d).tAST;
    } else if (d instanceof VarDecl) {
      t = ((VarDecl) d).tAST;
    } else {
      t = ((FormalParamDecl) d).astType;
    }
    if (t instanceof ArrayType) {
      reporter.reportError("Arrays not implemented", "", d.pos);
      retType = ((ArrayType) t).astType;
    } else {
      retType = t;
    }
    return retType;
  }

  /** Method emitStaticVariableReference.
   * Global MiniC variables become static variables in the Jasmin assembly
   * code. References to those variables are generated using this function. The
   * boolean "write" value distinguishes between read access (write=false) and
   * write access (write=true).
   */
  private void emitStaticVariableReference(ID ident, Type t, boolean write) {
    String ref;
    if (write) {
      ref = new String(JVM.PUTSTATIC);
    } else {
      ref = new String(JVM.GETSTATIC);
    }
    ref = ref.concat(" " + className + "." + ident.Lexeme + " "
        + getTypeDescriptorLabel(t));
    emit(ref);
  }

  /** Method isStaticMethod(FunDecl f).
   * For function delcaration f, return true if function f must become a
   * static method (aka class method) in the generated Jasmin assembly code.
   * The methods belonging to this category are:
   *   - all functions from the StdEnvironment
   *   - main()
   */
  private boolean isStaticMethod(FunDecl f) {
    String n = f.idAST.Lexeme;
    if (n.equals(StdEnvironment.getInt.idAST.Lexeme)
        || n.equals(StdEnvironment.putInt.idAST.Lexeme)
        || n.equals(StdEnvironment.getBool.idAST.Lexeme)
        || n.equals(StdEnvironment.putBool.idAST.Lexeme)
        || n.equals(StdEnvironment.getFloat.idAST.Lexeme)
        || n.equals(StdEnvironment.putFloat.idAST.Lexeme)
        || n.equals(StdEnvironment.getString.idAST.Lexeme)
        || n.equals(StdEnvironment.putString.idAST.Lexeme)
        || n.equals(StdEnvironment.putLn.idAST.Lexeme)
        || n.equals("main")) {
      return true;
    }
    return false;
  }

  /** Method getNrOfFormalParams(FunDecl f).
   * For function declaration f, return the number of formal parameters
   * of the function. E.g., for function ``void foo (int a, bool b) {}'',
   * the return value will be 2.
   * Note: this function assumes the AST tree layout from Assignment 3.
   */
  private int getNrOfFormalParams(FunDecl f) {
    int nrArgs = 0;
    Decl d = f.paramsAST;
    assert ((d instanceof EmptyFormalParamDecl)
            || (d instanceof FormalParamDeclSequence));
    if (d instanceof EmptyFormalParamDecl) {
      return 0;
    }
    while (d instanceof FormalParamDeclSequence) {
      nrArgs++;
      d = ((FormalParamDeclSequence) d).rAST;
      assert ((d instanceof EmptyFormalParamDecl)
              || (d instanceof FormalParamDeclSequence));
    }
    return nrArgs;
  }

  /** Method getFormalParam(FunDecl f, int nr).
   * For function declaration f, return the AST for parameter nr
   * (nr is the number of the parameter).
   *
   * E.g., for the following function and nr=2,
   *    void foo (int a, bool b) {}
   * the AST returned will be for "bool b".
   * Note: this function assumes the AST tree layout from Assignment 3.
   */
  private FormalParamDecl getFormalParam(FunDecl f, int nr) {
    int funArgs = getNrOfFormalParams(f);
    assert (funArgs >= 0);
    assert (nr <= funArgs);
    FormalParamDeclSequence s = (FormalParamDeclSequence) f.paramsAST;
    for (int i = 1; i < nr; i++) {
      assert (s.rAST instanceof FormalParamDeclSequence);
      s = (FormalParamDeclSequence) s.rAST;
    }
    assert (s.lAST instanceof FormalParamDecl);
    return (FormalParamDecl) s.lAST;
  }

  /** Method getDescriptor().
   * Constructs the method descriptor (of type string) for a given MiniC
   * function declaration.
   */
  private String getDescriptor(FunDecl f) {
    String ret = new String("(");
    for (int arg = 1; arg <= getNrOfFormalParams(f); arg++) {
      FormalParamDecl d = getFormalParam(f, arg);
      ret = ret.concat(getTypeDescriptorLabel(typeOfDecl(d)));
    }
    ret = ret.concat(")");
    ret = ret.concat(getTypeDescriptorLabel(f.tAST));
    return ret;
  }

  /*
   *
   * Here the Visitor methods for our code generator start:
   *
   *
   */

  /** visit method for Program. */
  public void visit(Program x) {
    emit("; Jassmin assembly code");
    emit("; MiniC v. 1.0");
    emit(".class public " + className);
    emit(".super java/lang/Object");
    // emit("; Program");
    emitStaticClassVariableDeclaration(x.D);
    emitClassInitializer(x.D);
    emitConstructor();
    x.D.accept(this);
  }

  /** visit method for EmptyDecl. */
  public void visit(EmptyDecl x) {
    // emit("; EmptyDecl");
  }

  /** visit method for FunDecl. */
  public void visit(FunDecl x) {
    isGlobalScope = false;
    // Allocate a frame for this function:
    isMain = x.idAST.Lexeme.equals("main");
    if (isMain) {
      frame = new Frame(true);
      emit("\n.method public static main([Ljava/lang/String;)V");
      // .var for main"s "this" pointer:
      // emit (".var 0 is this L" + className + "; from Label0 to Label1");
      // .var for main's String[] argument:
      // emit (".var 1 is arg0 [Ljava/lang/String; from Label0 to Label1");
    } else {
      frame = new Frame(false);
      emit("\n.method public " + x.idAST.Lexeme
          + getDescriptor(x));
      x.paramsAST.accept(this); // process formal parameters to adjust the
      // local variable count.
    }
    indent++;
    final int l0 = frame.getNewLabel();
    final int l1 = frame.getNewLabel();
    emitLabel(l0);
    if (isMain) {
      emit("new " + className);
      emit("dup");
      emit("invokespecial " + className + "/<init>()V");
      emit("astore_1");
    }
    // x.tAST.accept(this);
    // x.idAST.accept(this);
    x.stmtAST.accept(this);
    emitLabel(l1);
    if (isMain) {
      emit(JVM.RETURN);
    }
    emit(".limit locals " + frame.getNewLocalVarIndex());
    emit(".limit stack " + maxOperandStackSize);
    indent--;
    emit(".end method");
    isGlobalScope = true;
    isMain = false;
  }

  /** visit method for TypeDecl. */
  public void visit(TypeDecl x) {
    assert (false); // Can only occur in the StdEnvironment AST!
  }

  /** visit method for FormalParamDecl. */
  public void visit(FormalParamDecl x) {
    // emit("; FormalParamDecl");
    // TBD:
    // Here you need to allocate a new local variable index for the formal
    // parameter and store it for later use with the declaration AST.
    //
    // Relevant: x.index, frame.getNewLocalVarIndex();

  }

  /** visit method for FormalParamDeclSequence. */
  public void visit(FormalParamDeclSequence x) {
    // emit("; FormalParamDeclSequence");
    x.lAST.accept(this);
    x.rAST.accept(this);
  }

  /** visit method for EmptyFormalParamDecl. */
  public void visit(EmptyFormalParamDecl x) {
    // emit("; EmptyFormalParamDecl");
  }

  /** visit method for StmtSequence. */
  public void visit(StmtSequence x) {
    x.s1AST.accept(this);
    x.s2AST.accept(this);
  }

  /** visit method for AssignStmt. */
  public void visit(AssignStmt x) {
    emit("; AssignStmt, line " + x.pos.startLine);
    // x.lAST.accept(this);
    x.rAST.accept(this);
    if (x.lAST instanceof VarExpr) {
      VarExpr v = (VarExpr) x.lAST;
      Decl d = (Decl) v.Ident.declAST; 
      Type t = typeOfDecl(d);
      // TBD:
      // Here you have to distinguish between local and global MiniC variables.
      // Local variables are kept in the JVM's local variable array.  Global
      // variables are kept as static JVM class variables. The code for the
      // right-hand side of the assignment statement has already been generated
      // by calling method x.rAST.accept(). Now the result of the right-hand
      // side of the expression needs to be written back from the stack to the
      // left-hand side variable.
      //
      // Relevant functions: isGlobal()
      //                     emitStaticVariableReference()
      //                     emitISTORE()
      //                     emitFSTORE()

    } else {
      assert (false); // Arrays not implemented.
    }
  }

  /** visit method for IfStmt. */
  public void visit(IfStmt x) {
    emit("; IfStmt, line " + x.pos.startLine);
    // TBD:
    // The following code evaluates the condition of the if statement. After
    // execution of this code, the stack will contain 0 if the condition
    // evaluated to false, and 1 if the condition evaluated to true. You
    // should apply the template for if statements from the lecture slides
    // to finish code generation at the places marked by // TBD: below.
    x.eAST.accept(this);
    // Allocate 2 new labels for this if statement.
    final int l1 = frame.getNewLabel();
    final int l2 = frame.getNewLabel();
    // TBD:

    x.thenAST.accept(this);
    // TBD:

    if (x.elseAST != null) {
      x.elseAST.accept(this);
    }
    // TBD:

  }

  /** visit method for WhileStmt. */
  public void visit(WhileStmt x) {
    emit("; WhileStmt, line " + x.pos.startLine);
    // TBD:
    // Please apply the code template for while loops from the lecture
    // slides.

  }

  /** visit method for ForStmt. */
  public void visit(ForStmt x) {
    emit("; ForStmt, line " + x.pos.startLine);
    // TBD:
    // No template was given for "for" loops, but you can find out by compiling
    // a Java "for" loop to bytecode, use "classfileanalyzer" to disassemble
    // and look how it is done there. Using the classfileanalyzer is described
    // in the Assignment 5 spec.

  }

  /** visit method for ReturnStmt. */
  public void visit(ReturnStmt x) {
    emit("; ReturnStmt, line " + x.pos.startLine);
    x.eAST.accept(this); // visit even in "main", for possible side-effects
    if (isMain || x.eAST instanceof EmptyExpr) {
      emitRETURN(StdEnvironment.voidType);
    } else {
      emitRETURN(x.eAST.type);
    }
  }

  /** visit method for CompoundStmt. */
  public void visit(CompoundStmt x) {
    x.astDecl.accept(this);
    x.astStmt.accept(this);
  }

  /** visit method for EmptyStmt. */
  public void visit(EmptyStmt x) {
    // emit("; EmptyStmt");
  }

  /** visit method for EmptyCompoundStmt. */
  public void visit(EmptyCompoundStmt x) {
    // emit("; EmptyCompoundStmt");
  }

  /** visit method for CallStmt. */
  public void visit(CallStmt x) {
    emit("; CallStmt, line " + x.pos.startLine);
    x.eAST.accept(this);
  }

  /** visit method for VarDecl. */
  public void visit(VarDecl x) {
    if (x.isGlobal()) {
      // Global variables have been treated already in the static
      // variable initializer clinit. Nothing to be done here,
      // early exit:
      return;
    }
    // TBD:
    // If we fall through here, we're visiting a local variable declaration.
    // You have to allocate a new local variable index from "frame"
    // and assign it to x.index.
    //
    // Relevant methods:
    //                        frame.getNewLocalVarIndex
    //                        x.tAST.accept(this);
    //                        x.idAST.accept(this);
    //                        x.eAST.accept(this);

  }

  /** visit method for DeclSequence. */
  public void visit(DeclSequence x) {
    if ((x.D1 instanceof VarDecl) && isGlobalScope) {
      ((VarDecl) x.D1).setGlobal();
    }
    if ((x.D2 instanceof VarDecl) && isGlobalScope) {
      ((VarDecl) x.D2).setGlobal();
    }
    x.D1.accept(this);
    x.D2.accept(this);
  }

  /** visit method for VarExpr. */
  public void visit(VarExpr x) {
    Decl d = (Decl) x.Ident.declAST; 
    Type t = typeOfDecl(d);
    // TBD:
    // Here we are dealing with read-accesses of applied occurrences of
    // variables. Why only read-access? Basically, no left-hand side of an
    // assignment statement will occur here, because we do not invoke accept()
    // on left-hand sides of assignment statements. This means that left-hand
    // sides of assignments are not traversed; they are handled right at the
    // visit method for AssignStmt.
    //
    // Example1: a = b + 1
    // The left-hand side of the assignment won't be traversed ("a"). But the
    // right-hand side ("b+1") will.
    //
    // Example2: foo(247+x)
    // "x" will be another VarExpr, again a read-access.
    //
    // What you should do:
    // - if x is global, emit a static variable access to push the static
    //   variable onto the stack.
    // - if x is a local variable, you need to emit an ILOAD or an FLOAD,
    //   depending on the type of variable (ILOAD for int and bool).
    //
    // Relevant methods: isGlobal()
    //                   emitStaticVariableReference()
    //                   emitILOAD(), emitFLOAD()

  }

  /** visit method for AssignExpr. */
  public void visit(AssignExpr x) {
    emit("; AssignExpr");
    // x.lAST.accept(this);
    // x.rAST.accept(this);
    x.rAST.accept(this);
    if (x.lAST instanceof VarExpr) {
      VarExpr v = (VarExpr) x.lAST;
      Decl d = (Decl) v.Ident.declAST; 
      Type t = typeOfDecl(d);
      if (d.isGlobal()) {
        emitStaticVariableReference(v.Ident, typeOfDecl(v.Ident.declAST), true);
      } else {
        if (t.Tequal(StdEnvironment.intType)
            || t.Tequal(StdEnvironment.boolType)) {
          emitISTORE(d.index);
        } else if (t.Tequal(StdEnvironment.floatType)) {
          emitFSTORE(d.index);
        } else {
          assert (false);
        }
      } 
    } else {
      assert (false); // Arrays not implemented.
    }
  }

  /** visit method for IntExpr. */
  public void visit(IntExpr x) {
    x.astIL.accept(this);
  }

  /** visit method for FloatExpr. */
  public void visit(FloatExpr x) {
    x.astFL.accept(this);
  }

  /** visit method for BoolExpr. */
  public void visit(BoolExpr x) {
    x.astBL.accept(this);
  }

  /** visit method for StringExpr. */
  public void visit(StringExpr x) {
    x.astSL.accept(this);
  }

  /** visit method for ArrayExpr. */
  public void visit(ArrayExpr x) {
    emit("; ArrayExpr");
    x.idAST.accept(this);
    x.indexAST.accept(this);
  }

  /** visit method for BinaryExpr. */
  public void visit(BinaryExpr x) {
    // emit("; BinaryExpr");
    String op = new String(x.oAST.Lexeme);
    if (op.equals("&&")) {
      final int l1 = frame.getNewLabel();
      final int l2 = frame.getNewLabel();
      // TBD:
      // Implement the code template for && short circuit evaluation
      // from the lecture slides.

      return;
    }
    if (op.equals("||")) {
      // TBD:
      // Implement || short circuit evaluation.
      // This is similar to &&. You may use a Java example to figure out
      // the details.

      return;
    }
    x.lAST.accept(this);
    x.rAST.accept(this);
    // TBD:
    //
    // Here we treat binary operators +, -, *, / >, >=, <, <=, ==, !=
    // See the code templates in the lecture slides. Remaining cases are
    // similar, you can check how the javac compiler does it.

  }

  /** visit method for UnaryExpr. */
  public void visit(UnaryExpr x) {
    // emit("; UnaryExpr");
    String op = new String(x.oAST.Lexeme);
    x.eAST.accept(this);
    // TBD:
    //
    // Here we treat the following cases.
    //   unary "-": emit JVM.INEG for integers
    //   unary "+": do nothing
    //   "i2f": emit JVM.I2F instruction
    //   "!": you can use the following code template:
    //
    //    !E  =>    [[E]]
    //              ifne Label1
    //              iconst_1
    //              goto Label2
    //           Label1:
    //              iconst_0
    //           Label2:

  }

  /** visit method for EmptyExpr. */
  public void visit(EmptyExpr x) {
    // emit("; EmptyExpr");
  }

  /** visit method for ActualParam. */
  public void visit(ActualParam x) {
    emit("; ActualParam");
    x.pAST.accept(this);
  }

  /** visit method for EmptyActualParam. */
  public void visit(EmptyActualParam x) {
    // emit("; EmptyActualParam");
  }

  /** visit method for ActualParamSequence. */
  public void visit(ActualParamSequence x) {
    // emit("; ActualParamSequence");
    x.lAST.accept(this);
    x.rAST.accept(this);
  }

  /** visit method for CallExpr. */
  public void visit(CallExpr x) {
    emit("; CallExpr");
    // x.idAST.accept(this);
    assert (x.idAST.declAST instanceof FunDecl);
    FunDecl f = (FunDecl) x.idAST.declAST;
    if (!isStaticMethod(f)) {
      emit("; \"this\"-pointer is the first ActualParam with instance methods:");
      if (isMain) {
        emit(JVM.ALOAD_1);
      } else {
        emit(JVM.ALOAD_0);
      }
    }
    x.paramAST.accept(this);
    if (isStaticMethod(f)) {
      emit(JVM.INVOKESTATIC + " minic/lang/System/"
          + x.idAST.Lexeme + getDescriptor(f));
    } else {
      // TBD:
      // In case of an instance method, you need emit an JVM.INVOKEVIRTUAL
      // instruction. The name of the function consists of
      // <ClassName>/<functionname><functiondescriptor>.
      //
      // Relevant variables/functions: see above code for static methods.

    }
  }

  /** visit method for ExprSequence. */
  public void visit(ExprSequence x) {
    // emit("; ExprSequence");
    x.lAST.accept(this);
    x.rAST.accept(this);
  }

  /** visit method for ID. */
  public void visit(ID x) {
    // emit("; ID: " + x.Lexeme);
  }

  /** visit method for Operator. */
  public void visit(Operator x) {
    // emit("; Operator: " + x.Lexeme);
  } 

  /** visit method for IntLiteral. */
  public void visit(IntLiteral x) {
    // emit("; IntLiteral: " + x.Lexeme + "\n");
    // TBD:
    // Here you have to emit an ICONST instruction to load the integer
    // literal onto the JVM stack. (See method emitICONST above.)

  } 

  /** visit method for FloatLiteral. */
  public void visit(FloatLiteral x) {
    // emit("; FloatLiteral: " + x.Lexeme + "\n");
    // TBD: same for type float

  } 

  /** visit method for BoolLiteral. */
  public void visit(BoolLiteral x) {
    // emit("; BoolLiteral: " + x.Lexeme + "\n");
    // TBD: and bool...

  } 

  /** visit method for StringLiteral. */
  public void visit(StringLiteral x) {
    // emit("; StringLiteral: " + x.Lexeme);
    emit(JVM.LDC + " \"" + x.Lexeme + "\"");
  } 

  /** visit method for IntType. */
  public void visit(IntType x) {
    // emit("; IntType");
  }

  /** visit method for FloatType. */
  public void visit(FloatType x) {
    // emit("; FloatType");
  }

  /** visit method for BoolType. */
  public void visit(BoolType x) {
    // emit("; BoolType");
  }

  /** visit method for StringType. */
  public void visit(StringType x) {
    // emit("; StringType");
  }

  /** visit method for VoidType. */
  public void visit(VoidType x) {
    // emit("; VoidType");
  }

  /** visit method for ArrayType. */
  public void visit(ArrayType x) {
    // emit("; ArrayType");
  }

  /** visit method for ErrorType. */
  public void visit(ErrorType x) {
    emit("; ErrorType");
    assert (false);
  }

}
