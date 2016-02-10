package org.stjs.generator.utils;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import org.stjs.generator.GenerationContext;
import org.stjs.generator.GeneratorConstants;
import org.stjs.generator.javac.ElementUtils;
import org.stjs.generator.javac.TreeUtils;
import org.stjs.generator.javac.TreeWrapper;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

public final class Scopes {
	private Scopes() {
		// private
	}

	/**
	 * True if the method element is invoked from the outer type
	 *
	 * @param methodElement
	 * @param context
     * @return
     */
	public static boolean isInvokedElementFromOuterType(Element methodElement, TreeWrapper<?, ?> treeWrapper) {
		TypeElement currentScopeClassElement = TreeUtils.getEnclosingClass(treeWrapper.getPath());
		TypeElement methodOwnerElement = (TypeElement) methodElement.getEnclosingElement();
		return isOuterType(treeWrapper.getContext(), methodOwnerElement, currentScopeClassElement);
	}

	/**
	 * True if outerType is strictly the outer type of the subtype
	 *
	 * @param context
	 * @param methodOwnerElement
	 * @param currentScopeClassElement
     * @return
     */
	public static boolean isOuterType(GenerationContext<?> context, TypeElement methodOwnerElement, TypeElement currentScopeClassElement) {
		TypeMirror subTypeErasure = context.getTypes().erasure(currentScopeClassElement.asType());

		if (!(subTypeErasure instanceof DeclaredType)) {
			return false;
		}

		TypeMirror outerTypeErasure = context.getTypes().erasure(methodOwnerElement.asType());
		for (TypeMirror type = ((DeclaredType) subTypeErasure).getEnclosingType(); type != null; type = ((DeclaredType) type).getEnclosingType()) {
			if (context.getTypes().isSameType(type, outerTypeErasure)) {
				return true;
			}
			if (!(type instanceof DeclaredType)) {
				return false;
			}

			if (isTypeInClassHierarchy(context, outerTypeErasure, type)) {
				return true;
			}
		}

		return false;
	}

	private static boolean isTypeInClassHierarchy(GenerationContext<?> context, TypeMirror outerTypeErasure, TypeMirror type) {
		TypeElement typeElement = ElementUtils.asTypeElement(context, type);

		List<TypeElement> superTypes = ElementUtils.getSuperTypes(typeElement);
		for (TypeElement superType : superTypes) {
			type = context.getTypes().erasure(superType.asType());
			if (context.getTypes().isSameType(type, outerTypeErasure)) {
				return true;
			}
		}

		return false;
	}

	public static <JS, T extends Tree> int getElementDeepnessLevelFromUse(TreeWrapper<T, JS> treeWrapper, Element methodElement) {
		GenerationContext<JS> context = treeWrapper.getContext();

		TypeElement classCallingTheMethod = TreeUtils.getEnclosingClass(treeWrapper.getPath());
		DeclaredType erasedClassCallingTheMethod = (DeclaredType) context.getTypes().erasure(classCallingTheMethod.asType());

		TypeElement targetMethodOwner = (TypeElement) methodElement.getEnclosingElement();
		TypeMirror erasedTargetMethodOwner = context.getTypes().erasure(targetMethodOwner.asType());

		TypeMirror type = erasedClassCallingTheMethod.getEnclosingType();
		while (type != null) {
			Element element = context.getTypes().asElement(type);

			if (isInstanceOfType(context, erasedTargetMethodOwner, type)) {
				return ElementUtils.getClassDeepnessLevel(element);
			}

			if (!ElementUtils.isInnerClass(element)) {
				return 0;
			}

			type = ((DeclaredType) type).getEnclosingType();
		}

		return 0;
	}

	private static <JS> boolean isInstanceOfType(GenerationContext<JS> context, TypeMirror erasedTargetMethodOwner, TypeMirror type) {
		return context.getTypes().isSameType(type, erasedTargetMethodOwner) || isTypeInClassHierarchy(context, erasedTargetMethodOwner, type);
	}

	public static String buildOuterClassAccessTargetPrefix() {
		return GeneratorConstants.THIS + "." + GeneratorConstants.NON_PUBLIC_METHODS_AND_FIELDS_PREFIX
				+ GeneratorConstants.INNER_CLASS_CONSTRUCTOR_PARAM_PREFIX;
	}

	public static <JS> List<JS> buildOuterClassParametersAsNames(GenerationContext<JS> context, Element callingClass, Element targetClass) {

		List<JS> result = new ArrayList<>();
		for (String s : buildNewClassInstanceAllocFunctionParametersAsString(callingClass, targetClass)) {
			result.add(context.js().name(s));
		}

		return result;
	}

	public static List<String> buildOuterClassConstructorParametersNames(Element targetClass) {
		int targetClassDeepnessLevel = ElementUtils.getClassDeepnessLevel(targetClass);

		String prefix = GeneratorConstants.INNER_CLASS_CONSTRUCTOR_PARAM_PREFIX + GeneratorConstants.AUTO_GENERATED_ELEMENT_SEPARATOR;

		List<String> outerClassVarNames = new ArrayList<>();
		for (int i = 0; i <= targetClassDeepnessLevel - 1; i++) {
			outerClassVarNames.add(prefix + i);
		}

		return outerClassVarNames;
	}

	public static List<String> buildNewClassInstanceAllocFunctionParametersAsString(Element callingClass, Element targetClass) {
		int callingClassDeepnessLevel = ElementUtils.getClassDeepnessLevel(callingClass);
		int targetClassDeepnessLevel = ElementUtils.getClassDeepnessLevel(targetClass);

		// get the list of outer variables based on target class deepness:
		//   [outerClass$0, outerClass$1, outerClass$2, ...]
		List<String> outerClassVarNames = buildOuterClassConstructorParametersNames(targetClass);

		// remove any variable that are not available in the calling class scope based on the calling class deepness.
		while(outerClassVarNames.size() > callingClassDeepnessLevel) {
			outerClassVarNames.remove(outerClassVarNames.size() - 1);
		}

		// prefix all variables to get:
		//   [this._outerClass$0, this._outerClass$1, ...]
		for (int i = 0; i < outerClassVarNames.size(); i++) {
			String updatedValue = GeneratorConstants.THIS + "." + GeneratorConstants.NON_PUBLIC_METHODS_AND_FIELDS_PREFIX + outerClassVarNames.get(i);
			outerClassVarNames.set(i, updatedValue);
		}

		// if the target class level is deeper than the calling class, add "this" as the last outerclass parameter
		if (outerClassVarNames.size() <= targetClassDeepnessLevel - 1) {
			outerClassVarNames.add(GeneratorConstants.THIS);
		}

		assert outerClassVarNames.size() == targetClassDeepnessLevel;

		return outerClassVarNames;
	}

	public static boolean isRegularInstanceField(Element fieldElement, IdentifierTree tree) {
		if (fieldElement == null || fieldElement.getKind() != ElementKind.FIELD) {
			// only meant for fields
			return false;
		}
		if (JavaNodes.isStatic(fieldElement)) {
			// only instance fieds
			return false;
		}

		if (GeneratorConstants.THIS.equals(tree.getName().toString()) || GeneratorConstants.SUPER.equals(tree.getName().toString())) {
			return false;
		}
		return true;
	}
}
