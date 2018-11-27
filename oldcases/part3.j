class Main {
    Void main() {
        Sub a;
        Int b;
        Bool c;
        b = a.test(1,2);
        b = 1 * 5;
        c = true;
        c = !c;
        return;
    }
}

class Sub {
    Int test(Int a, Int b) {
        return a * b;
    }
}