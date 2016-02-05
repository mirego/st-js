package org.stjs.generator.writer.inheritance;

import junit.framework.Assert;
import org.junit.Test;
import org.stjs.generator.STJSRuntimeException;
import org.stjs.generator.utils.AbstractStjsTest;

public class InheritanceGeneratorTest extends AbstractStjsTest {

	@Test
	public void testImplements() {
		assertCodeContains(Inheritance1.class, "stjs.extend(Inheritance1, stjs.Java.Object, [MyInterface],");
	}

	@Test
	public void testExtends() {
		assertCodeContains(Inheritance2.class, "stjs.extend(Inheritance2, MySuperClass, [],");
	}

	@Test
	public void testAccessProtectedField() {
		// the this. prefix should be added for fields from the super class too
		assertCodeContains(Inheritance3.class, "return this._field;");
	}

	@Test
	public void testExtendsMore() {
		assertCodeContains(Inheritance4.class, "stjs.extend(Inheritance4, stjs.Java.Object, [MyInterface, MyInterface2],");
	}

	@Test
	public void testAbstractAndGeneric() {
		assertCodeContains(Inheritance5.class, "stjs.extend(Inheritance5, stjs.Java.Object, [MyInterface3],");
	}

	@Test
	public void testImplementsSyntheticType() {
		assertCodeContains(Inheritance6.class, "stjs.extend(Inheritance6, stjs.Java.Object, [],");
	}

	@Test
	public void testExtendsSyntheticType() {
		assertCodeContains(Inheritance9.class, "stjs.extend(Inheritance9, null, [],");
	}

	@Test
	public void testExtendsEmptyContructor() {
		// check that the super constructor is called for empty constructor in the child class
		assertCodeContains(Inheritance7.class, "MySuperClass.prototype._constructor.call(this);");
	}

	@Test
	public void testExtendsInnerClass() {
		assertCodeContains(Inheritance8.class, "stjs.extend(Inheritance8, MyClass1.MyInnerClass, [],");
	}

	@Test
	public void testPrivateConstructor() throws Exception {
		Assert.assertEquals("Value passed as parameters on the static constructor method", execute(Inheritance9_private_constructor.class));
	}

	@Test
	public void testSubclass_defined_before_baseclass() throws Exception {
		try {
			execute(Inheritance10_subclass_defined_before_baseclass.class);
			Assert.fail("Should not get here. An exception was expected");
		} catch (STJSRuntimeException e) {
			Assert.assertTrue(e.getMessage().contains("_super is undefined when creating class Inheritance10_subclass_defined_before_baseclass.SubClass"));
		}
	}
}
