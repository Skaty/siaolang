class Main {
    Void main() {
        return;
    }
}

/*
 * File tests weird cases that may or may not fail!
 * Minimally, the file should pass the Parser but not the
 * typechecker!
 */
class Quirks {
    Int y;
    Bool z;

    /*
     * This must fail because:
     * - (a < b) is not an object.
     * - (a > b || c) is not callable!
     */
    Void exprAsAtom() {
        Int a;
        Int b;
        Bool c;
        return;
        (a < b).yes = 7;
        (a > b || c)(a < b);
    }

    /*
     * These should compile to IR3!
     */
    Void atomChaining() {
        SelfReferClass a;
        Bool test;

        a.b.c.d = 5;
        a.b.c.e();
        test = a.b.c.d < a.b.c.f;
    }

    /*
     *  Should fail at typechecking stage!
     */
    Void emptyWhile() {
        Int a;
        Int b;

        while(a>b) { }
    }

    /*
     * Should compile to IR3
     */
    Bool manyNot() {
        return !!!!!!!!!!!!false && !!!!!!!!!!!!!!!!!!!!!!!true;
    }

    /*
     * Tests weird negation + variable shadowing.
     */
    Int manyNegate() {
        Int z;
        Int b;
        Int c;
        Int d;
        y =  ----------------z + --------------b * ---------c / -------d;
        return ----------------z + --------------b * ---------c / -------d;
    }
}

class SelfReferClass {
    SelfReferClass b;
    SelfReferClass c;
    Int d;
    Int f;

    Void e() {
        println("Hey there!");
    }
}