package org.stjs.generator.writer.declaration;

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;

import org.stjs.generator.GenerationContext;
import org.stjs.generator.GeneratorConstants;
import org.stjs.generator.STJSRuntimeException;
import org.stjs.generator.javac.TreeWrapper;
import org.stjs.generator.javascript.AssignOperator;
import org.stjs.generator.javascript.Keyword;
import org.stjs.generator.name.DependencyType;
import org.stjs.generator.writer.MemberWriters;
import org.stjs.generator.writer.WriterContributor;
import org.stjs.generator.writer.WriterVisitor;

import com.google.common.base.Defaults;
import com.google.common.primitives.Primitives;
import com.sun.source.tree.VariableTree;

/**
 * This will add the declaration of a field. This contributor is not added directly, but redirect from
 * {@link org.stjs.generator.writer.statement.VariableWriter}
 * 
 * @author acraciun
 */
public class FieldWriter<JS> extends AbstractMemberWriter<JS> implements WriterContributor<VariableTree, JS> {
	private static final Map<String, Class<?>> PRIMITIVE_CLASSES = new HashMap<String, Class<?>>();
	static {
		for (Class<?> cls : Primitives.allPrimitiveTypes()) {
			PRIMITIVE_CLASSES.put(cls.getName(), cls);
		}
	}

	private Class<?> primitiveClass(TypeMirror type) {
		Class<?> cls = PRIMITIVE_CLASSES.get(type.toString());
		if (cls == null) {
			throw new STJSRuntimeException("Cannot load class " + type);
		}
		return cls;
	}

	private JS getPrimitiveDefaultValue(Element element, GenerationContext<JS> context) {
		Object defaultValue = Defaults.defaultValue(primitiveClass(element.asType()));
		if (defaultValue instanceof Number) {
			return context.js().number((Number) defaultValue);
		}
		if (defaultValue instanceof Character) {
			return context.js().character(defaultValue.toString());
		}
		if (defaultValue instanceof Boolean) {
			return context.js().keyword(Boolean.TRUE.equals(defaultValue) ? Keyword.TRUE : Keyword.FALSE);
		}
		return context.js().keyword(Keyword.NULL);
	}

	@Override
	public JS visit(WriterVisitor<JS> visitor, VariableTree tree, GenerationContext<JS> context) {
		TreeWrapper<VariableTree, JS> tw = context.getCurrentWrapper();
		if (MemberWriters.shouldSkip(tw)) {
			return null;
		}
		// load the type of the variable
		context.getCurrentWrapper().child(tree.getType()).getTypeName(DependencyType.OTHER);

		JS initializer = null;
		if (tree.getInitializer() == null) {
			if (tw.isPrimitiveType()) {
				initializer = getPrimitiveDefaultValue(tw.getElement(), context);
			} else {
				initializer = context.js().keyword(Keyword.NULL);
			}
		} else {
			initializer = visitor.scan(tree.getInitializer(), context);
		}

		String fieldName = tree.getName().toString();
		if (!tree.getModifiers().getFlags().contains(Modifier.PUBLIC)) {
			fieldName = GeneratorConstants.NON_PUBLIC_METHODS_AND_FIELDS_PREFIX + fieldName;
		}
		if (tw.getEnclosingType().isGlobal()) {
			// var field = init; //for global types
			return context.js().variableDeclaration(true, fieldName, initializer);
		}
		JS member = context.js().property(getMemberTarget(tw), fieldName);
		return context.js().expressionStatement(context.js().assignment(AssignOperator.ASSIGN, member, initializer));
	}
}
