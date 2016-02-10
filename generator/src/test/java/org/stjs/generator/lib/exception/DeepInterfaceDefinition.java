package org.stjs.generator.lib.exception;

public class DeepInterfaceDefinition {
    public static class VeryDeepInterfaceDefinition {
        public interface InterfaceImplementedByClass {
            int methodFromInterface();
        }
    }
}
