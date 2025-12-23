; Jassmin assembly code
; MiniC v. 1.0
.class public assignment
.super java/lang/Object

.method static <clinit>()V
   .limit stack 1
   .limit locals 0
   return
.end method

.method public <init>()V
   .limit stack 1
   .limit locals 1
   .var 0 is this Lassignment; from Label0 to Label1

  Label0:
   aload_0
   invokespecial java/lang/Object/<init>()V
  Label1:
   return
.end method

.method public static main([Ljava/lang/String;)V
  Label0:
   new assignment
   dup
   invokespecial assignment/<init>()V
   astore_1
   ; AssignStmt, line 3
   bipush 22
   istore_2
   ; CallStmt, line 4
   ; CallExpr
   ; ActualParam
   iload_2
   invokestatic lang/System/putInt(I)V
   ; CallStmt, line 5
   ; CallExpr
   invokestatic lang/System/putLn()V
  Label1:
   return
   .limit locals 3
   .limit stack 150
.end method
