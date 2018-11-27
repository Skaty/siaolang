class Main {
    Void main() { return; }
}

class Basics {
    A eh;
    Int a;
    Bool b;

    Void unaryTest() {
        Bool u;
        Int c;
        c = --5;
        u = !!!!!!true;
    }

    Bool bopType() {
        Bool f;
        f = true && false || false && b;
        a = 1 + 5 * 4 + 3 / 3;
        return 5 < 6;
    }

    Void fields() {
        eh.a.a.a.a.c = 5;
    }
}

class Sub {
    Int a;
    Bool b;
    Bool returncorrect() {
        b = false;
        return true;
    }

    Void stmttests() {
        if (a != 5) {
            b = true;
        } else {
            b = false;
        }

        while (b) {
            a = a + 1;
        }

        while (a != 5) {
            a = a + 2;
        }

        readln(b);
        println(this.a);
        return;
    }

    Void caller() { Sub a; a.b = true; return; }
}

class A {
    B a;
    Int c;

    B getB() {
        return a;
    }
}

class B {
    A a;

    A getA() {
        return a;
    }
}

// Stuff that are actually valid in type checking!
class Quirks {
    Int a;
    Bool b;
    String c;
    A d;

    // Tests [If] & [Block] quirkiness
    Void multiReturn() {
        if (b) {
            c = "Tomodachi";
            return;
        } else {
            a = 5;
        }

        b = true;
    }

    // New inline
    Void newInline() {
        (new A()).c = 5;
        return;
    }

    // Tests overloading (bonus)
    String toString(Int a) {
        return "integer";
    }

    String toString(Bool b) {
        return "true";
    }

    // Crazy params
    Void doesNothing(Bool a, Int b, A c, B d, String hey) {
        return;
    }

    B getMulti() {
        return this.d.getB().getA().getB().getA().getB();
    }

    // Call with crazy expressions
    Void callDoesNothing() {
        return doesNothing(true && b || (1 < 2), 1 / 2 + 17 * a, d, new B(), "Heyayhsa");
    }
}