; Jassmin assembly code
; MiniC v. 1.0
.class public coercion
.super java/lang/Object

.method static <clinit>()V
   .limit stack 1
   .limit locals 0
   return
.end method

.method public <init>()V
   .limit stack 1
   .limit locals 1
   .var 0 is this Lcoercion; from Label0 to Label1

  Label0:
   aload_0
   invokespecial java/lang/Object/<init>()V
  Label1:
   return
.end method

.method public static main([Ljava/lang/String;)V
  Label0:
   new coercion
   dup
   invokespecial coercion/<init>()V
   astore_1
   ; CallStmt, line 2
   ; CallExpr
   ; ActualParam
   iconst_1
   i2f
   invokestatic lang/System/putFloat(F)V
   ; CallStmt, line 3
   ; CallExpr
   invokestatic lang/System/putLn()V
  Label1:
   return
   .limit locals 2
   .limit stack 150
.end method
