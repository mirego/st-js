package org.stjs.generator.writer.innerTypes;

public class InnerTypes31_anonymous_class_inside_inner_class {

    int fieldInRootClass = 10;

    int doIt() {
        return new InnerClass().doIt();
    }

    public class InnerClass {
        int fieldInInnerClass = 200;

        int doIt() {
            MyCallable anonymousInstance = new MyCallable() {
                @Override
                public int doIt() {
                    int valueFromAnonymousClass = 3000;
                    return fieldInRootClass + fieldInInnerClass + valueFromAnonymousClass;
                }
            };

            return anonymousInstance.doIt();
        };
    }

    public interface MyCallable {
        int doIt();
    }

    public static String main(String[] args) {
        return "" + new InnerTypes31_anonymous_class_inside_inner_class().doIt();
    }

}
