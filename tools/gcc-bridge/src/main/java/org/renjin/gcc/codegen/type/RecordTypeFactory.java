package org.renjin.gcc.codegen.type;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.renjin.gcc.InternalCompilerException;
import org.renjin.gcc.codegen.GeneratorFactory;
import org.renjin.gcc.codegen.LocalVarAllocator;
import org.renjin.gcc.codegen.RecordClassGenerator;
import org.renjin.gcc.codegen.expr.ExprGenerator;
import org.renjin.gcc.codegen.field.*;
import org.renjin.gcc.codegen.param.ParamGenerator;
import org.renjin.gcc.codegen.param.RecordPtrParamGenerator;
import org.renjin.gcc.codegen.ret.RecordPtrReturnGenerator;
import org.renjin.gcc.codegen.ret.ReturnGenerator;
import org.renjin.gcc.codegen.var.RecordArrayVarGenerator;
import org.renjin.gcc.codegen.var.RecordPtrVarGenerator;
import org.renjin.gcc.codegen.var.RecordVarGenerator;
import org.renjin.gcc.codegen.var.VarGenerator;
import org.renjin.gcc.gimple.expr.GimpleConstructor;
import org.renjin.gcc.gimple.expr.GimpleNopExpr;
import org.renjin.gcc.gimple.type.GimpleArrayType;

import java.util.List;
import java.util.Map;

/**
 * Creates generators for variables and values of type {@code GimpleRecordType}
 */
public class RecordTypeFactory extends TypeFactory {
  private final RecordClassGenerator generator;

  public RecordTypeFactory(RecordClassGenerator generator) {
    this.generator = generator;
  }

  @Override
  public TypeFactory pointerTo() {
    return new Pointer();
  }

  @Override
  public VarGenerator addressableVarGenerator(LocalVarAllocator allocator) {
    return new RecordVarGenerator(generator, allocator.reserveObject());
  }

  @Override
  public TypeFactory arrayOf(GimpleArrayType arrayType) {
    return new Array(arrayType);
  }

  @Override
  public FieldGenerator fieldGenerator(String className, String fieldName) {
    return new RecordFieldGenerator(className, fieldName, generator);
  }

  @Override
  public ExprGenerator constructorExpr(GeneratorFactory generatorFactory, GimpleConstructor value) {
    Map<String, ExprGenerator> fields = Maps.newHashMap();
    for (GimpleConstructor.Element element : value.getElements()) {
      if(!(element.getValue() instanceof GimpleNopExpr)) {
        FieldGenerator fieldGenerator = generator.getFieldGenerator(element.getFieldName());
        ExprGenerator fieldValue = generatorFactory.forExpression(fieldGenerator.getType(), element.getValue());
        fields.put(element.getFieldName(), fieldValue);
      }
    }
    return new RecordConstructor(generator, fields);
  }

  public class Array extends TypeFactory {

    private GimpleArrayType arrayType;

    public Array(GimpleArrayType arrayType) {
      this.arrayType = arrayType;
    }

    @Override
    public FieldGenerator fieldGenerator(String className, String fieldName) {
      return new RecordArrayFieldGenerator(className, fieldName, generator, arrayType);
    }

    @Override
    public VarGenerator varGenerator(LocalVarAllocator allocator) {
      return new RecordArrayVarGenerator(arrayType, generator, allocator.reserveObject());
    }

    @Override
    public VarGenerator addressableVarGenerator(LocalVarAllocator allocator) {
      return varGenerator(allocator);
    }

    @Override
    public ExprGenerator constructorExpr(GeneratorFactory generatorFactory, GimpleConstructor value) {

      if(arrayType.getElementCount() != value.getElements().size()) {
        throw new InternalCompilerException(String.format(
            "array type defined as size of %d, only % constructors provided", 
              arrayType.getElementCount(), value.getElements().size()));
      }
      
      List<ExprGenerator> elements = Lists.newArrayList();
      for (GimpleConstructor.Element element : value.getElements()) {
        GimpleConstructor elementValue = (GimpleConstructor) element.getValue();
        ExprGenerator elementConstructor = RecordTypeFactory.this.constructorExpr(generatorFactory, elementValue);
        elements.add(elementConstructor);
      }
      
      return new RecordArrayConstructor(generator, arrayType, elements);
    }
  }

  public class Pointer extends TypeFactory {

    @Override
    public ParamGenerator paramGenerator() {
      return new RecordPtrParamGenerator(generator);
    }

    @Override
    public FieldGenerator fieldGenerator(String className, String fieldName) {
      return new RecordPtrFieldGenerator(className, fieldName, generator);
    }

    @Override
    public ReturnGenerator returnGenerator() {
      return new RecordPtrReturnGenerator(generator);
    }

    @Override
    public VarGenerator varGenerator(LocalVarAllocator allocator) {
      return new RecordPtrVarGenerator(generator, allocator.reserveObject());
    }
  }
}
