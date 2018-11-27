import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * An object that holds the state of
 * our parser (e.g. for typechecker use)
 */
class TypecheckerState {
    public HashMap<String, CDesc> classdescriptor;
    public HashMap<String, String> localenv;

    // Points to the current class that's being processed
    // Works because of leftmost derivation
    public int classptr = 0;
    public ArrayList<String> cnames;
    public int methodptr = 0;
    public ArrayList<Pair<String, String>> mnames;

    // Stack of types that are awaiting resolution
    public ArrayDeque<String> typestack;
    public ArrayDeque<HashMap<String, String>> msigstack;

    // State indicators
    public boolean secondrun = false;
    public boolean isWithinClass = false;

    // Temporaries
    public ArrayList<String> fmlTypes;
    public HashMap<String, String> curFml = new HashMap<String, String>();
    public HashMap<String, String> curVDecls = new HashMap<String, String>();

    public TypecheckerState() {
        this.classdescriptor = new HashMap<String, CDesc>();
        this.localenv = new HashMap<String, String>();

        this.cnames = new ArrayList<>();
        this.mnames = new ArrayList<>();
        this.initTypeStacks();
    }

    public void initTypeStacks() {
        this.typestack = new ArrayDeque<String>();
        this.msigstack = new ArrayDeque<HashMap<String, String>>();
    }

    public void initTypecheckPhase() {
        this.secondrun = true;
        this.localenv = new HashMap<String, String>();
        this.classptr = 0;
        this.methodptr = 0;

        this.initTypeStacks();
    }

    public String getCurrentClass() {
        return this.cnames.get(this.classptr);
    }

    public String getCurrentMethod() {
        return (methodptr < this.mnames.size()) ?
            this.mnames.get(this.methodptr).head : "";
    }

    public String getCurrentMethodReturnType() {
        return (methodptr < this.mnames.size()) ?
            this.mnames.get(this.methodptr).tail : "";
    }
}