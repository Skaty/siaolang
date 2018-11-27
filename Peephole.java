/**
 * Peephole optimizer for ARM
 * Runs till there are no changes to the code
 */
class Peephole {
    public static String run(String asm) {
        String lastPass = "";
        String curPass = asm;

        String[][] peepholes = new String[][] {
            // Removes two moves to one if possible
            new String[] {
                "\\tmov(.*?),(.*?)(?:[\\r\\n]|\\r\\n)\\tmov(.*?),\\1",
                "\tmov$3,$2"
            },
            // Remove store to same location as an immediately preceding load
            new String[] {
                "\\tldr(.*?),\\[(.*?),(.*?)\\](?:[\\r\\n]|\\r\\n)\\tstr\\1,\\[\\2,\\3\\]",
                "\tldr$1,[$2,$3]"
            },
            // Remove load to same location as an immediately preceding store
            new String[] {
                "\\tstr(.*?),\\[(.*?),(.*?)\\](?:[\\r\\n]|\\r\\n)\\tldr\\1,\\[\\2,\\3\\]",
                "\tstr$1,[$2,$3]"
            },
        };

        while (!lastPass.equals(curPass)) {
            lastPass = curPass;

            for (String[] peephole : peepholes) {
                curPass = curPass.replaceAll(peephole[0], peephole[1]);
            }
        }

        return lastPass;
    }
}