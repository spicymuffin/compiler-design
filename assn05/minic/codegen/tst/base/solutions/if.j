; Jassmin assembly code
; MiniC v. 1.0
.class public if
.super java/lang/Object

.method static <clinit>()V
   .limit stack 1
   .limit locals 0
   return
.end method

.method public <init>()V
   .limit stack 1
   .limit locals 1
   .var 0 is this Lif; from Label0 to Label1

  Label0:
   aload_0
   invokespecial java/lang/Object/<init>()V
  Label1:
   return
.end method

.method public static main([Ljava/lang/String;)V
  Label0:
   new if
   dup
   invokespecial if/<init>()V
   astore_1
   ; AssignStmt, line 3
   bipush 22
   istore_2
   ; IfStmt, line 7
   iload_2
   bipush 23
   if_icmpgt Label2
   iconst_0
   goto Label3
  Label2:
   iconst_1
  Label3:
   ifeq Label4
   ; CallStmt, line 5
   ; CallExpr
   ; ActualParam
   ldc "a>23\n"
   invokestatic lang/System/putString(Ljava/lang/String;)V
   goto Label5
  Label4:
   ; CallStmt, line 7
   ; CallExpr
   ; ActualParam
   ldc "a<=23\n"
   invokestatic lang/System/putString(Ljava/lang/String;)V
  Label5:
  Label1:
   return
   .limit locals 3
   .limit stack 150
.end method
