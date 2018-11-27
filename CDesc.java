import java_cup.runtime.ComplexSymbolFactory.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Representation of the Class Descriptor
 * for a specific class
 */
class CDesc {
    public HashMap<String, String> fds = new HashMap<String, String>();
    public HashMap<String, HashMap<String, String>> msigs = new HashMap<String, HashMap<String, String>>();

    // Performs distinct-name checking too!
    // Parts (a), (c), (d)
    public void fromBlock(Node bnode, Location left, Location right, String cname) {
        List<Node> parts = bnode.children;
        for (Node part : parts) {
            if (part.type == Node.NodeType.VDecl) {
                String vname = part.children.get(1).getLabelledValue();
                String vtype = part.children.get(0).data;
                if (fds.containsKey(vname)) {
                    Parser.addError("Class field %s is declared twice!", left, right, vname);
                }

                fds.putIfAbsent(vname, vtype);
            } else {
                // method block
                String rtype = part.children.get(0).data;
                String mname = part.children.get(1).getLabelledValue();
                HashSet<String> inames = new HashSet<String>();
                ArrayList<String> itypes = new ArrayList<String>(part.children.size());

                for (Node mpart : part.children.get(2).children) {
                    String iname = mpart.children.get(1).getLabelledValue();
                    if (!inames.add(iname)) {
                        Parser.addError("Parameter %s is defined more than once in method signature %s", left,
                                right, iname, mname);
                    }
                    itypes.add(mpart.children.get(0).data);
                }

                String isig = String.join(",", itypes);
                this.addMsig(cname, mname, isig, rtype);
            }
        }
    }

    public void addMsig(String cname, String mname, String minput, String mreturn) {
        HashMap<String, String> msig = this.msigs.getOrDefault(mname, new HashMap<String, String>());

        if (msig.containsKey(minput)) {
            System.out.format("Method %s with type signature %s is declared twice!\n", mname, minput);
        }

        msig.putIfAbsent(minput, mreturn);
        this.msigs.putIfAbsent(mname, msig);
    }

    public String toString() {
        return fds.toString() + msigs.toString();
    }
}
