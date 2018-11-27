class Main {
    Void main() {
        Quirks q;
        q = new Quirks();

        // Test cases
        q.classChaining();
        q.passClass();
        println("\n");
        println("Boolean is ");
        q.manyNot();
        println("\n");
        q.simpleMaths();
        println("\n");
        println("Negation result: ");
        println(q.manyNegate());
        println("\n");
    }
}

/*
 * Tests weird stuff
 */
class Quirks {
    Int y;
    Bool z;

    /*
     * Test classes in classes
     */
    Void classChaining() {
        SelfReferClass a;
        a = new SelfReferClass();
        a.init();

        a.printd();
        println("\n");
        a.b.c.printd();

        println(a.d);
        println("\n");
        println(a.b.c.d);
    }

    /*
     * Test class object passing (arguments and return)
     */
    Void passClass() {
        SelfReferClass foo;
        SelfReferClass bar;

        foo = makeSelfRefer(10);
        bar = makeSelfRefer(20);

        foo.printPass(bar);
        println("\n");
        bar.printPass(foo);
    }

    /*
     * Sets up and returns a new SelfReferClass
     */
    SelfReferClass makeSelfRefer(Int d) {
        SelfReferClass rtn;
        rtn = new SelfReferClass();

        rtn.d = d;

        return rtn;
    }

    /*
     * Test weird negation printing
     */
    Void manyNot() {
        println(!!!!!!!!!!!!false && !!!!!!!!!!!!!!!!!!!!!!!true);
    }

    /*
     * Sanity check maths
     */
    Void simpleMaths() {
        Int a;
        Int b;
        Int c;
        a = 10;
        b = 15;
        c = -12;

        println(a + b);
        println("\n");
        println(c + b);
        println("\n");
        println(a * b);
    }

    /*
     * Tests weird negation
     */
    Int manyNegate() {
        Int z;
        Int b;
        Int c;
        Int d;
        z = 5;
        b = 10;
        c = 15;
        d = 3;
        return ----------------z + --------------b * ---------c;
    }
}

class SelfReferClass {
    SelfReferClass b;
    SelfReferClass c;
    Int d;
    Int f;

    Void init() {
        b = new SelfReferClass();
        b.c = new SelfReferClass();

        // Set integer to some values
        d = 5;
        b.c.d = 15;
    }

    Void printd() {
        println(d);
    }

    /*
     * Test pasing of class objects by reference
     */
    Void printPass(SelfReferClass cls) {
        println(cls.d);
    }
}