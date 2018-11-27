// Tests all kinds of valid stuff

class Main {
    Void main(Int a, Int b, Int c) {
        // tests method-level shadowing
        Subsidiary a;
        a = new Subsidiary();
        return;
    }
}

class Subsidiary {
    // Test overloading
    Int add(Int a, Int b) {
        return a + b;
    }

    Int add(Int a, Int b, Int c) {
        return a + b + c;
    }

    // Test calling different overloaded methods
    Int callOverlords() {
        Int i;
        Int j;
        Int h;

        Int first;
        Int second;

        first = add(i, j);
        second = add(i, j, h);

        return first + second;
    }

    Void noReturn(Int a, Int b) {
        a = a + b;
    }
}

class Blocks {
    Subsidiary sub;

    Void whileTrue() {
        Int a;

        while (true) {
            doStuff();
        }

        while (false) {
            doStuff();
        }
    }

    Int conditional(Int a, Int b) {
        if (!!true && !!!false) {
            conditional(1 / 2, 5 + 5 * 3);
            sub.callOverlords();
            return a + b;
        } else {
            b = b + 1;
            a = sub.callOverlords();
            return b + 1;
        }

        return a;
    }

    Int both(Int a, Int b) {
        while (a < b) {
            if (!!true && !!!false) {
                conditional(1 / 2, 5 + 5 * 3);
                b = sub.callOverlords();
            } else {
                b = b + 1;
                a = sub.callOverlords();
            }
        }

        return a * b;
    }

    Void doStuff() {
        sub.add(1, 2);
        sub.add(3, 4);
        return;
    }
}

class Shadowing {
    Int a;
    Bool b;

    Int increment(Int a) {
        return a + 1;
    }

    Int incrementGlobal(Bool a) {
        return this.a + 1;
    }
}

class BuiltIn {
    Void printLine() {
        String a;

        //a = null;
        println("hello");
        println(a);
    }

    Void readLine() {
        Bool a;
        String b;
        Int c;

        readln(a);
        readln(b);
        readln(c);
    }
}