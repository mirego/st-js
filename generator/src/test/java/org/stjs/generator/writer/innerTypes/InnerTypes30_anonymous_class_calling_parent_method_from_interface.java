package org.stjs.generator.writer.innerTypes;

import org.stjs.generator.lib.exception.DeepInterfaceDefinition;

public class InnerTypes30_anonymous_class_calling_parent_method_from_interface {

    int fieldFromRootClass = 10;

    // Class setup:
    //     1. abstract class
    //     2. implementing an interface
    //     3. but interface's methods are not implemented by the abstract class

    public abstract class MyAbstractClass implements DeepInterfaceDefinition.VeryDeepInterfaceDefinition.InterfaceImplementedByClass {
        Object myField = new InterfaceForAnonymousClass(){
            @Override
            public int dummyMethod() {
                return methodFromInterface() + fieldFromRootClass;
            }
        };
    }

}
