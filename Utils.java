/**
 * Extremely useful Pair class
 */
class Pair<X, Y> {
    public X head;
    public Y tail;

    public Pair(X head, Y tail) {
        this.head = head;
        this.tail = tail;
    }
}

class Utils {
    /**
     * A symbolic representation of the various
     * primitive types supported by JLite.
     */
    public enum ParserTypes {
        INVALID("_invalid_"),
        METHOD("_method_"),
        BOOL("Bool"),
        INT("Int"),
        STRING("String"),
        VOID("Void");

        private String value;

        private ParserTypes(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public String getValue() {
            return value;
        }
    }

    public static String getMsigFromFmlNode(Node fmls) {
        String[] fmlTypes = new String[fmls.children.size()];
        for (int i = 0; i < fmls.children.size(); i++) {
            fmlTypes[i] = fmls.children.get(i).children.get(0).toString();
        }

        return Utils.getLocalMsig(fmlTypes);
    }

    public static String getLocalMsig(String... fmlTypes) {
        return String.join(",", fmlTypes);
    }

    public static String addIndent(String str) {
        return "  " + str.replaceAll("\n", "\n  ");
    }
}