package org.stjs.generator.writer.innerTypes;

public class InnerTypes30_anonymous_class_calling_parent_method_from_interface {

    // Class setup:
    //     1. abstract class
    //     2. implementing an interface
    //     3. but interface's methods are not implemented by the abstract class
    public interface InterfaceImplementedByClass {
        void methodFromInterface();
    }

    public interface InterfaceForAnonymousClass {
        void dummyMethod();
    }

    public abstract class MyAbstractClass implements InterfaceImplementedByClass {
        Object myField = new InterfaceForAnonymousClass(){
            @Override
            public void dummyMethod() {
                methodFromInterface();
            }
        };
    }

}
