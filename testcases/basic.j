class Main {
    Void main() {
        Int a;
        Shadowing s;

        s = new Shadowing();

        println(s.returnBool());
        println(s.returnGlobalBool());
    }
}

class Shadowing {
    Bool a;

    Void setGlobalBool() {
        a = false;
    }

    Bool returnBool() {
        Bool a;
        a = true;
        return a;
    }

    Bool returnGlobalBool() {
        return a;
    }
}