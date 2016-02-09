package org.stjs.generator.lib.exception;

public class Exception4_call_getMessage_on_js_exceptions {

    public static String main(String[] args) {
        String exceptionMessage = null;
        try {
            String s = null;
            exceptionMessage.toString();
        } catch (Exception e) {
            exceptionMessage = e.getMessage();
        }
        return exceptionMessage;
    }

}
