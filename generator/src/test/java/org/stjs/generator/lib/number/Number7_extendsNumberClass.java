package org.stjs.generator.lib.number;

public class Number7_extendsNumberClass {

    public static class MyNumber extends Number {

        private int value;

        public MyNumber(int value) {
            this.value = value;
        }

        @Override
        public int intValue() {
            return value;
        }

        @Override
        public long longValue() {
            return value;
        }

        @Override
        public float floatValue() {
            return value;
        }

        @Override
        public double doubleValue() {
            return value;
        }
    }

    public static String main(String[] args) {
        MyNumber myNumber = new MyNumber(1234);
        return "" + myNumber.intValue();
    }
}
