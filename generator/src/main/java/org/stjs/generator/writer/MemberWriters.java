package org.stjs.generator.writer;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import org.stjs.generator.GeneratorConstants;
import org.stjs.generator.javac.TreeUtils;
import org.stjs.generator.javac.TreeWrapper;
import org.stjs.generator.javascript.Keyword;
import org.stjs.generator.name.DependencyType;
import org.stjs.generator.utils.Scopes;

import javax.lang.model.element.Element;

public final class MemberWriters {
	private MemberWriters() {
		//
	}

	public static <JS, T extends Tree> JS buildTarget(TreeWrapper<T, JS> tw) {
		if (tw.getEnclosingType().isGlobal()) {
			// do this to register the type name as needed
			tw.getEnclosingType().getTypeName(DependencyType.STATIC);
			return null;
		}

		if (tw.isStatic()) {
			return tw.getContext().js().name(tw.getEnclosingType().getTypeName(DependencyType.STATIC));
		}

		if (Scopes.isInvokedElementFromOuterType(tw.getElement(), tw)) {
			return buildTargetForOuterTypeAccess(tw);
		}
		return tw.getContext().js().keyword(Keyword.THIS);
	}

	public static <JS, T extends Tree> boolean shouldSkip(TreeWrapper<T, JS> tw) {
		if (tw.getElement() == null) {
			// static initializer
			return true;
		}
		return tw.isServerSide();
	}

	private static <JS, T extends Tree> JS buildTargetForOuterTypeAccess(TreeWrapper<T, JS> tw) {
		// Find the scope deepness level to which we access the method
		Element element = TreeUtils.elementFromUse((ExpressionTree) tw.getTree());
		int deepnessLevel = Scopes.getElementDeepnessLevelFromUse(tw, element);


		String scopeAccessorPrefix = Scopes.buildOuterClassAccessTargetPrefix();

		return tw.getContext().js().name(scopeAccessorPrefix + GeneratorConstants.AUTO_GENERATED_ELEMENT_SEPARATOR + deepnessLevel);
	}

}
