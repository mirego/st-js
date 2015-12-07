package org.stjs.generator.writer.declaration;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.Block;
import org.stjs.generator.AnnotationUtils;
import org.stjs.generator.GenerationContext;
import org.stjs.generator.GeneratorConstants;
import org.stjs.generator.javac.ElementUtils;
import org.stjs.generator.javac.InternalUtils;
import org.stjs.generator.javac.TreeUtils;
import org.stjs.generator.javac.TreeWrapper;
import org.stjs.generator.javascript.AssignOperator;
import org.stjs.generator.utils.FieldUtils;
import org.stjs.generator.utils.JavaNodes;
import org.stjs.generator.utils.Scopes;
import org.stjs.generator.writer.JavascriptKeywords;
import org.stjs.generator.writer.MemberWriters;
import org.stjs.generator.writer.WriterContributor;
import org.stjs.generator.writer.WriterVisitor;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MethodWriter<JS> extends AbstractMemberWriter<JS> implements WriterContributor<MethodTree, JS> {

	private static String changeName(String name) {
		if (name.equals(GeneratorConstants.ARGUMENTS_PARAMETER)) {
			return "_" + name;
		}

		return name;
	}

	/**
	 * @return true if this method is the unique method of an online declaration
	 */
	private boolean isMethodOfJavascriptFunction(TreeWrapper<Tree, JS> treeWrapper) {
		TreeWrapper<Tree, JS> parent = treeWrapper.parent().parent();
		if (parent.getTree() instanceof NewClassTree) {
			return parent.child(((NewClassTree) parent.getTree()).getIdentifier()).isJavaScriptFunction();
		}
		return false;
	}

	private String getAnonymousTypeConstructorName(MethodTree tree, GenerationContext<JS> context) {
		if (!JavaNodes.isConstructor(tree)) {
			return null;
		}
		Element typeElement = TreeUtils.elementFromDeclaration((ClassTree) context.getCurrentPath().getParentPath().getLeaf());
		boolean anonymous = typeElement.getSimpleName().toString().isEmpty();
		if (anonymous) {
			return InternalUtils.getSimpleName(typeElement);
		}
		return null;
	}

	private String decorateMethodName(MethodTree tree, GenerationContext<JS> context) {
		Symbol.MethodSymbol element = (Symbol.MethodSymbol) context.getCurrentWrapper().getElement();
		String methodName = element.getSimpleName().toString();

		if (AnnotationUtils.JSOverloadName.isPresent(element)
				|| ElementUtils.hasAnOverloadedEquivalentMethod(TreeUtils.elementFromDeclaration(tree), context.getElements())) {
			methodName = AnnotationUtils.JSOverloadName.decorate(element);
		}

		if (!JavaNodes.isPublic(tree) && !isFromInterface(context)) {
			return GeneratorConstants.NON_PUBLIC_METHODS_AND_FIELDS_PREFIX + methodName;
		}

		return methodName;
	}

	private boolean isFromInterface(GenerationContext<JS> context) {
		return ElementKind.INTERFACE.equals(context.getCurrentWrapper().getEnclosingType().getElement().getKind());
	}

	public static <JS> List<JS> getParams(List<? extends VariableTree> treeParams, GenerationContext<JS> context) {
		List<JS> params = new ArrayList<JS>();
		for (VariableTree param : treeParams) {
			params.add(context.js().name(changeName(param.getName().toString())));
		}
		return params;
	}

	public static int getTHISParamPos(List<? extends VariableTree> parameters) {
		for (int i = 0; i < parameters.size(); ++i) {
			VariableTree param = parameters.get(i);
			if (GeneratorConstants.SPECIAL_THIS.equals(param.getName().toString())) {
				return i;
			}
		}
		return -1;
	}

	protected boolean accept(TreeWrapper<MethodTree, JS> tw) {
		if (tw.isNative()) {
			// native methods are there only to indicate already existing javascript code - or to allow method
			// overloading
			return false;
		}
		if (MemberWriters.shouldSkip(tw)) {
			return false;
		}

		return true;
	}

	private void addFieldInitializersToConstructor(WriterVisitor<JS> visitor, JS constructorBody, GenerationContext<JS> context) {
		List<JS> expressions = new ArrayList<>();

		for (Tree tree : context.getCurrentWrapper().getEnclosingType().getTree().getMembers()) {
			if (tree.getKind() == Tree.Kind.VARIABLE) {
				TreeWrapper<VariableTree, JS> variableTreeWrapper = context.wrap(TreeUtils.elementFromDeclaration((VariableTree) tree));
				if (isFieldInitializerRequired(variableTreeWrapper)) {
					expressions.add(context.js().expressionStatement(
							context.js().assignment(AssignOperator.ASSIGN,
									context.js().property(
											context.js().name(JavascriptKeywords.THIS),
											FieldUtils.getFieldName(variableTreeWrapper.getTree())
									),
									visitor.scan(
											variableTreeWrapper.getTree().getInitializer(),
											context)
							)
					));
				}
			}
		}

		Collections.reverse(expressions);

		for (JS expression : expressions) {
			context.js().addStatementBeginning(constructorBody, expression);
		}
	}

	private boolean isFieldInitializerRequired(TreeWrapper<VariableTree, JS> variableTreeWrapper) {
		if (!FieldUtils.isFieldDeclaration(variableTreeWrapper.getContext())) {
			return false;
		}

		if (MemberWriters.shouldSkip(variableTreeWrapper)) {
			return false;
		}

		if (variableTreeWrapper.isStatic()) {
			return false;
		}

		if (variableTreeWrapper.getTree().getInitializer() == null) {
			return false;
		}

		if (FieldUtils.isInitializerLiteral(variableTreeWrapper.getTree().getInitializer())) {
			return false;
		}

		return true;
	}

	@Override
	public JS visit(WriterVisitor<JS> visitor, MethodTree tree, GenerationContext<JS> context) {
		TreeWrapper<MethodTree, JS> tw = context.getCurrentWrapper();
		if (!accept(tw)) {
			return null;
		}

		List<JS> params = getParams(tree.getParameters(), context);

		JS body = visitor.scan(tree.getBody(), context);

		// set if needed Type$1 name, if this is an anonymous type constructor
		String anonymousTypeConstructorName = getAnonymousTypeConstructorName(tree, context);

		if (anonymousTypeConstructorName == null) {
			JS thisScopeAccessor = addThisScopeAccessorIfNeeded(tree, context, body);
			if (thisScopeAccessor != null) {
				// Insert our accessor at the beginning of the current method block { }
				((Block) body).addChildToFront((Node) thisScopeAccessor);
			}
		}

		JS decl = context.js().function(anonymousTypeConstructorName, params, body);

		if (JavaNodes.isConstructor(tree)) {
			addFieldInitializersToConstructor(visitor, body, context);
		}

		JS constructorOrPrototypePrefix = addConstructorOrPrototypePrefixIfNeeded(tree, context, tw, decl);
		if (constructorOrPrototypePrefix == null) {
			return decl;
		} else {
			return constructorOrPrototypePrefix;
		}
	}

	/**
	 * Evaluate if any accessor may require our current scope.
	 * In this case, we return a "var this$X = this;" where x is the deepness level ourself.
	 *
	 * @param tree
	 * @param context
	 * @param body
     * @return
     */
	private JS addThisScopeAccessorIfNeeded(MethodTree tree, GenerationContext<JS> context, JS body) {
		if (isFromInterface(context) || JavaNodes.isConstructor(tree) || isMethodOfJavascriptFunction(context.getCurrentWrapper())) {
			return null;
		}

		Element element = TreeUtils.elementFromDeclaration(tree);
		int deepnessLevel = Scopes.getElementDeepnessLevel(element);
		String thisScopeAccessorVariable = GeneratorConstants.THIS + GeneratorConstants.AUTO_GENERATED_ELEMENT_SEPARATOR + deepnessLevel;

		if (findAccessor(body, thisScopeAccessorVariable)) {
			return context.js().variableDeclaration(true, thisScopeAccessorVariable, context.js().name(GeneratorConstants.THIS));
		}

		return null;
	}

	private boolean findAccessor(JS body, String thisScopeAccessorVariable) {
		if (body instanceof Block) {
			String thisScopeIteratorReady = thisScopeAccessorVariable + ".";
			Iterator blockIterator = ((Block) body).iterator();
			while (blockIterator.hasNext()) {
				Node node = (Node) blockIterator.next();
				if (node instanceof AstNode && ((AstNode) node).toSource().contains(thisScopeIteratorReady)) {
					return true;
				}
			}
		}
		return false;
	}

	private JS addConstructorOrPrototypePrefixIfNeeded(MethodTree tree, GenerationContext<JS> context,
													   TreeWrapper<MethodTree, JS> tw, JS declaration) {
		if (JavaNodes.isConstructor(tree) || isMethodOfJavascriptFunction(context.getCurrentWrapper())) {
			return null;
		}

		String methodName = decorateMethodName(tree, context);
		if (tw.getEnclosingType().isGlobal()) {
			// var method=function() ...; //for global types
			return context.js().variableDeclaration(true, methodName, declaration);
		}
		JS member = context.js().property(getMemberTarget(tw), methodName);
		return context.js().expressionStatement(context.js().assignment(AssignOperator.ASSIGN, member, declaration));
	}
}
