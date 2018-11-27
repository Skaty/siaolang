class Main {
    Void main() {
        MyInt a;
        a = new MyInt();
        a.init();
        while (a.getValue() < 10) {
            a.increment();
            println(a.getValue());
            println("\n");
        }
    }
}

class MyInt {
    Int a;

    Void init() {
        a = 0;
    }

    Void increment() {
        a = a + 1;
    }

    Int getValue() {
        return a;
    }
}