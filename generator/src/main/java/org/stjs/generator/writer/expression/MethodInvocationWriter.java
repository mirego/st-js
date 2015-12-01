package org.stjs.generator.writer.expression;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import org.stjs.generator.AnnotationUtils;
import org.stjs.generator.GenerationContext;
import org.stjs.generator.GeneratorConstants;
import org.stjs.generator.javac.ElementUtils;
import org.stjs.generator.javac.InternalUtils;
import org.stjs.generator.javac.TreeUtils;
import org.stjs.generator.javac.TreeWrapper;
import org.stjs.generator.visitor.DiscriminatorKey;
import org.stjs.generator.writer.JavascriptKeywords;
import org.stjs.generator.writer.MemberWriters;
import org.stjs.generator.writer.WriterContributor;
import org.stjs.generator.writer.WriterVisitor;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;

public class MethodInvocationWriter<JS> implements WriterContributor<MethodInvocationTree, JS> {

	public static <JS, T extends MethodInvocationTree> JS buildTarget(WriterVisitor<JS> visitor, TreeWrapper<T, JS> tw) {
		ExpressionTree select = tw.getTree().getMethodSelect();

		if (select instanceof IdentifierTree) {
			// simple call: method(args)
			return MemberWriters.buildTarget(tw);
		}
		// calls with target: target.method(args)
		if (TreeUtils.isSuperCall(tw.getTree())) {
			// this is a call of type super.staticMethod(args) -> it should be handled as a simple call to
			// staticMethod
			return MemberWriters.buildTarget(tw);
		}
		MemberSelectTree memberSelect = (MemberSelectTree) select;
		JS targetJS = visitor.scan(memberSelect.getExpression(), tw.getContext());
		if (tw.isStatic() && !ElementUtils.isTypeKind(tw.child(memberSelect).child(memberSelect.getExpression()).getElement())) {
			//this is static method called from an instances: e.g. x.staticMethod()
			targetJS = tw.getContext().js().property(targetJS, JavascriptKeywords.CONSTRUCTOR);
		}

		return targetJS;
	}

	public static <JS> String buildMethodName(MethodInvocationTree tree, GenerationContext<JS> context) {
		ExpressionTree select = tree.getMethodSelect();

		if (select instanceof IdentifierTree) {
			// simple call: method(args)
			return buildMethodNameForIdentifierTree(tree, context, (IdentifierTree) select);
		} else if (select instanceof MemberSelectTree) {
			// calls with target: target.method(args)
			return buildMethodNameForMemberSelectTree(select);
		}
		throw context.addError(tree, "Unsupported tree type during buildMethodName.");
	}

	private static String buildMethodNameForMemberSelectTree(ExpressionTree select) {
		MemberSelectTree memberSelect = (MemberSelectTree) select;
		String methodName = memberSelect.getIdentifier().toString();
		Symbol symbol = null;

		if (TreeUtils.isFieldAccess(memberSelect.getExpression())) {
            symbol = (Symbol) InternalUtils.symbol(select);
        }

		if (symbol != null && (symbol.getKind() == ElementKind.FIELD || symbol.getKind() == ElementKind.METHOD)) {
            return prefixNonPublicMethods(methodName, symbol);
        } else {
            return methodName;
        }
	}

	private static <JS> String buildMethodNameForIdentifierTree(MethodInvocationTree tree, GenerationContext<JS> context, IdentifierTree select) {
		String methodName = select.getName().toString();

		// Ignore super() calls, these are never going to be prefixed
		if (GeneratorConstants.SUPER.equals(methodName)) {
			return methodName;
		}

		Symbol symbol = (Symbol.MethodSymbol) InternalUtils.symbol(tree);
		ExecutableElement methodElement = TreeUtils.getMethod((Symbol.MethodSymbol) symbol);

		if (methodElement != null
                && (AnnotationUtils.JSOverloadName.isPresent((Symbol.MethodSymbol) methodElement)
                || hasAnOverloadedMethod(context, methodElement))) {
            methodName = AnnotationUtils.JSOverloadName.decorate((Symbol.MethodSymbol) methodElement);
        }
		return prefixNonPublicMethods(methodName, symbol);
	}

	private static <JS> boolean hasAnOverloadedMethod(GenerationContext<JS> context, ExecutableElement methodElement) {
		if (context == null) {
			return false;
        }
		return ElementUtils.hasAnOverloadedEquivalentMethod(methodElement, context.getElements());
	}

	private static String prefixNonPublicMethods(String methodName, Symbol element) {
		if (element != null && element.getModifiers().contains(Modifier.PUBLIC)) {
            return methodName;
        } else {
            return GeneratorConstants.NON_PUBLIC_METHODS_AND_FIELDS_PREFIX + methodName;
        }
	}

	public static <JS> List<JS> buildArguments(WriterVisitor<JS> visitor, MethodInvocationTree tree, GenerationContext<JS> context) {
		List<JS> arguments = new ArrayList<JS>();
		for (Tree arg : tree.getArguments()) {
			arguments.add(visitor.scan(arg, context));
		}
		return arguments;
	}

	public static <JS> String buildTemplateName(MethodInvocationTree tree, GenerationContext<JS> context) {
		String name = context.getCurrentWrapper().getMethodTemplate();
		if (name != null) {
			return name;
		}
		return context.getCurrentWrapper().getEnclosingType().isJavaScriptFunction() ? "invoke" : "none";
	}

	@Override
	public JS visit(WriterVisitor<JS> visitor, MethodInvocationTree tree, GenerationContext<JS> context) {
		String templateName = buildTemplateName(tree, context);

		return visitor.forward(DiscriminatorKey.of(MethodInvocationWriter.class.getSimpleName(), templateName), tree, context);
	}
}
