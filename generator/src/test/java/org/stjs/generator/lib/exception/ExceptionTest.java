package org.stjs.generator.lib.exception;

import org.junit.Assert;
import org.junit.Test;
import org.stjs.generator.utils.AbstractStjsTest;

public class ExceptionTest extends AbstractStjsTest{

    @Test
    public void testCanCallGetMessageOnExceptions() throws Exception {
        Assert.assertEquals("This is the exception message", execute(Exception1_ExceptionGetMessage.class));
    }

    @Test
    public void testException_toString_contains_the_exception_className() throws Exception {
        Assert.assertEquals(true, ((String)execute(Exception2_toString_contains_the_exception_className.class)).contains("MyException: the message of the exception"));
    }

    @Test
    public void testException_toString_contains_cause_exception() throws Exception {
        Assert.assertEquals(true, ((String)execute(Exception3_toString_contains_cause_exception.class)).contains("Caused by: MyExceptionA: the cause exception message"));
    }

}
