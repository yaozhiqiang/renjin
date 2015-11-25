package org.renjin.gcc.codegen.expr;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.renjin.gcc.InternalCompilerException;
import org.renjin.gcc.codegen.call.CallGenerator;
import org.renjin.gcc.gimple.type.GimpleType;
import org.renjin.gcc.gimple.type.GimpleVoidType;

import java.util.List;

/**
 * Generates a call to free(ptr). 
 */
public class FreeCallGenerator implements CallGenerator {
  @Override
  public void emitCall(MethodVisitor mv, List<ExprGenerator> argumentGenerators) {
    if(argumentGenerators.size() != 1) {
      throw new InternalCompilerException("Expected single argument to free, found " +
        argumentGenerators.size() + " arguments");
    }

    // this is a no op - but not sure we can be sure that 
    // there are no side effects 
    ExprGenerator ptr = argumentGenerators.get(0);
    ptr.emitPushPtrArrayAndOffset(mv);
    mv.visitInsn(Opcodes.POP2);
  }

  @Override
  public Type returnType() {
    return Type.VOID_TYPE;
  }

  @Override
  public GimpleType getGimpleReturnType() {
    return new GimpleVoidType();
  }

  @Override
  public ExprGenerator expressionGenerator(List<ExprGenerator> argumentGenerators) {
    throw new UnsupportedOperationException();
  }
}