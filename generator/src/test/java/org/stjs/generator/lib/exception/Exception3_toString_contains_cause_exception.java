package org.stjs.generator.lib.exception;

public class Exception3_toString_contains_cause_exception {

    public static class MyExceptionA extends RuntimeException {
        public MyExceptionA(String message) {
            super(message);
        }
    }

    public static class MyExceptionB extends RuntimeException {
        public MyExceptionB(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static String main(String[] args) {
        MyExceptionA causeException = new MyExceptionA("the cause exception message");
        MyExceptionB exception = new MyExceptionB("the message of the exception", causeException);
        return exception.toString();
    }

}
