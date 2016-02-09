package org.stjs.generator.lib.exception;

public class Exception2_toString_contains_the_exception_className {

    public static class MyException extends RuntimeException {
        public MyException(String message) {
            super(message);
        }
    }

    public static String main(String[] args) {
        RuntimeException exception = new MyException("the message of the exception");
        return exception.toString();
    }

}
