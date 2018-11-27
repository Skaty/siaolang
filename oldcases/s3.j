// Does nothing, only here for reasons
class Main {
    Void main() { return; }
}

class AllTokens {
    Int a;
    Int b;
    Int c;
    Bool d;

    String s;

    Funny makeFun() {
        return new Funny();
    }

    Void convolutedRead() {
        String c;
        Int b;
        while (!true || !false && a < b) {
            readln(c);
            println(c);
        }
    }

    Void recursion() {
        this.recursion();
    }

    Int returnCallSth() {
        return arithmetrics(a, b, c);
    }

    Void validStrings() {
        String escapes;

        s = "";
        escapes = "\\,\n,\r,\t,\b,\74,\x4A";
    }

    Void unaries() {
        a = !a;
        b = -b;
        c = -10;
        d = !(true || false);
    }

    Int arithmetrics(Int prec, Int unary, Int extra) {
        a = a + b * c - prec / extra;
        a = a * b / c;
        a = a / b * c;
        prec = (a + c) * (prec - unary);
        unary = (a - -c) * (-c + b - -b);
        return unary;
    }

    Bool allBop(Int a, Int b) {
        d = a < b;
        d = a > b;
        d = a <= b;
        d = a >= b;
        d = a == b;
        return a != b;
    }
}

class Funny {
    /* an empty class is valid */
}

/*
 A test
 */
class Second {
    Void returnNothing() {
        return;
    }

    String singleArg(String a) {
        return a;
    }

    String multArgs(String a, Int b) {
        return a;
    }
}