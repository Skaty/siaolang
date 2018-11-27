import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

class VarDecl3 {
    public String type;
    public String id;

    public VarDecl3(Node vdecl) {
        this.type = vdecl.children.get(0).data;
        this.id = vdecl.children.get(1).getLabelledValue();
    }

    public VarDecl3(String type, String id) {
        this.type = type;
        this.id = id;
    }

    public String toString() {
        return this.toString(true);
    }

    public String toString(boolean hasSemicolon) {
        if (hasSemicolon) {
            return String.format("%s %s;", this.type, this.id);
        } else {
            return String.format("%s %s", this.type, this.id);
        }
    }
}

class CData3 {
    public String cname3;
    public List<VarDecl3> variables = new ArrayList<VarDecl3>();

    // Scratch space to track methods in class
    public List<Node> mdecls = Collections.<Node>emptyList();

    public CData3(Node cdecl) {
        this.cname3 = cdecl.children.get(0).getLabelledValue();
        ArrayList<Node> content = cdecl.children.get(1).children;

        for (int i = 0; i < content.size(); i++) {
            if (content.get(i).type != Node.NodeType.VDecl) {
                this.mdecls = content.subList(i, content.size());
                break;
            }

            this.variables.add(new VarDecl3(content.get(i)));
        }
    }

    public int getFieldPosition(String fieldName) {
        for (int i = 0; i < variables.size(); i++) {
            VarDecl3 var = variables.get(i);
            if (var.id.equals(fieldName)) {
                return i;
            }
        }

        return -1;
    }

    public String toString() {
        ArrayList<String> vars = new ArrayList<String>();
        for (VarDecl3 v : this.variables) {
            vars.add(v.toString());
        }

        return String.format("class %s {\n%s\n}",
            this.cname3,
            Utils.addIndent(String.join("\n", vars))
        );
    }
}

class Stmt3 {
    public enum StmtType {
        Label("Label %s:"),
        IfGoto("if (%s) goto %s;"),
        Goto("goto %s;"),
        ReadLn("readln(%s);"),
        PrintLn("println(%s);"),
        NAssign("%s %s = %s;"),
        LAssign("%s = %s;"),
        FAssign("%s.%s = %s;"),
        Call("%s(%s);"),
        ReturnT("return %s;"),
        ReturnV("return;");

        private String value;

        private StmtType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public StmtType type;
    public ArrayList<Node> children = new ArrayList<Node>();

    public Stmt3(StmtType type) {
        this.type = type;
    }

    public String toString() {
        return String.format(type.getValue(), children.toArray());
    }
}

class CMtd3 {
    public String type;
    public String id;

    public List<VarDecl3> fmllist3 = new ArrayList<>();

    public List<VarDecl3> vardecl3s = new ArrayList<>();
    public List<Stmt3> stmts = new ArrayList<>();

    private int numTemporaries = 0;
    private int numLabels = 0;

    public CMtd3(String cname, Node cmtd) {
        this.type = cmtd.children.get(0).data;
        this.id = cmtd.children.get(1).getLabelledValue();

        this.fmllist3.add(new VarDecl3(cname, "this"));

        for (Node fml : cmtd.children.get(2).children) {
            this.fmllist3.add(new VarDecl3(fml));
        }

        // Mangle the name if it's not main
        if (cmtd.note != "_main_") {
            this.id = IR3Gen.getMangledName(cname, this.id, cmtd.note);
        }

        this.parseBody(cmtd.children.get(cmtd.children.size() - 1));
    }

    public void parseBody(Node mdbody) {
        for (int i = 0; i < mdbody.children.size(); i++) {
            Node curNode = mdbody.children.get(i);
            if (curNode.type != Node.NodeType.VDecl) {
                // Parse statements
                for (Node child : curNode.children) {
                    this.stmts.addAll(IR3Gen.fromStmt(this, child));
                }
            } else {
                // Parse variable declarations
                this.vardecl3s.add(new VarDecl3(curNode));
            }
        }
    }

    public String addTemporary(String type) {
        String tempVar = String.format("_t%s", this.numTemporaries++);
        VarDecl3 temp = new VarDecl3(type, tempVar);
        vardecl3s.add(temp);
        return tempVar;
    }

    public String getFreeLabel() {
        return Integer.toString(numLabels++);
    }

    public String toString() {
        ArrayList<String> fmls = new ArrayList<>();
        for (VarDecl3 f : this.fmllist3) {
            fmls.add(f.toString(false));
        }

        ArrayList<String> vars = new ArrayList<String>();
        for (VarDecl3 v : this.vardecl3s) {
            vars.add(v.toString());
        }

        ArrayList<String> stmtStrs = new ArrayList<String>();
        for (Stmt3 s : this.stmts) {
            stmtStrs.add(s.toString());
        }

        return String.format("%s %s(%s) {\n%s\n%s\n}",
            this.type,
            this.id,
            String.join(", ", fmls),
            Utils.addIndent(String.join("\n", vars)),
            Utils.addIndent(String.join("\n", stmtStrs))
        );
    }
}

class IR3 {
    public List<CData3> cdata3s;
    public List<CMtd3> cmtd3s;

    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (CData3 cd3 : this.cdata3s) {
            sb.append(cd3);
            sb.append(System.lineSeparator());
        }

        for (CMtd3 cm3 : this.cmtd3s) {
            sb.append(cm3);
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }
}

class IR3Gen {
    public static enum ParsingFlag {
        Recursive,
        Id3Only;
    }

    private static boolean optimize = false;

    static IR3 fromAST(Node ast, boolean optimize) {
        IR3 ir3 = new IR3();
        ir3.cdata3s = ast.children.stream().map(x -> new CData3(x)).collect(Collectors.toList());
        ir3.cmtd3s = new ArrayList<>();

        // Set global setting
        IR3Gen.optimize = optimize;

        for (CData3 cd : ir3.cdata3s) {
            for (Node mdecl : cd.mdecls) {
                ir3.cmtd3s.add(new CMtd3(cd.cname3, mdecl));
            }
        }

        return ir3;
    }

    static List<Stmt3> fromStmts(CMtd3 mtd, List<Node> stmts) {
        ArrayList<Stmt3> res = new ArrayList<>();

        for (Node stmt : stmts) {
            res.addAll(IR3Gen.fromStmt(mtd, stmt));
        }

        return res;
    }

    static List<Stmt3> fromStmt(CMtd3 mtd, Node stmt) {
        ArrayList<Stmt3> stmt3s = new ArrayList<>();

        if (stmt.type == Node.NodeType.Cond) {
            // if (<Exp>) { <Stmt1>+ } else { <Stmt2>+ }
            // converts to
            //    if <Exp> goto b
            //    <Stmt2>
            //    goto end
            // b: <Stmt1>
            // end:

            Stmt3 ifgoto = new Stmt3(Stmt3.StmtType.IfGoto);
            Stmt3 stmt1Label = new Stmt3(Stmt3.StmtType.Label);
            Stmt3 endLabel = new Stmt3(Stmt3.StmtType.Label);
            Stmt3 endGoto = new Stmt3(Stmt3.StmtType.Goto);

            stmt1Label.children.add(new Node(mtd.getFreeLabel()));
            endLabel.children.add(new Node(mtd.getFreeLabel()));

            Pair<Node, List<Stmt3>> condExp = IR3Gen.fromExp(mtd, stmt.children.get(0), EnumSet.of(ParsingFlag.Recursive));
            stmt3s.addAll(condExp.tail);

            ifgoto.children.add(condExp.head);
            ifgoto.children.add(
                stmt1Label.children.get(0)
            );

            endGoto.children.add(
                endLabel.children.get(0)
            );

            // Stmt 2
            List<Stmt3> stmt1 = IR3Gen.fromStmts(mtd, stmt.children.get(1).children);
            List<Stmt3> stmt2 = IR3Gen.fromStmts(mtd, stmt.children.get(2).children);

            stmt3s.add(ifgoto);
            stmt3s.addAll(stmt2);
            stmt3s.add(endGoto);
            stmt3s.add(stmt1Label);
            stmt3s.addAll(stmt1);
            stmt3s.add(endLabel);
        } else if (stmt.type == Node.NodeType.While) {
            /*
             * while (<Exp>) {<Stmt>*}
             * converts to
             * start:
             * <any additional variables>
             * if <Exp> goto doloop
             * goto end
             * doloop:
             * <Stmt>*
             * goto start
             * end
             */

            Stmt3 startLabel = new Stmt3(Stmt3.StmtType.Label);
            startLabel.children.add(new Node(mtd.getFreeLabel()));

            // Due to how exp works, we need to insert this directly
            mtd.stmts.add(startLabel);

            Stmt3 startGoto = new Stmt3(Stmt3.StmtType.Goto);
            startGoto.children.add(
                startLabel.children.get(0)
            );

            Stmt3 endLabel = new Stmt3(Stmt3.StmtType.Label);
            endLabel.children.add(new Node(mtd.getFreeLabel()));

            Stmt3 endGoto = new Stmt3(Stmt3.StmtType.Goto);
            endGoto.children.add(
                endLabel.children.get(0)
            );

            Stmt3 doLoopLabel = new Stmt3(Stmt3.StmtType.Label);
            doLoopLabel.children.add(new Node(mtd.getFreeLabel()));

            Stmt3 ifgoto = new Stmt3(Stmt3.StmtType.IfGoto);
            Pair<Node, List<Stmt3>> condExp = IR3Gen.fromExp(mtd, stmt.children.get(0), EnumSet.of(ParsingFlag.Recursive));

            stmt3s.addAll(condExp.tail);
            ifgoto.children.add(condExp.head);
            ifgoto.children.add(
                doLoopLabel.children.get(0)
            );

            endGoto.children.add(
                endLabel.children.get(0)
            );

            // Stmt 2
            List<Stmt3> whileStmts = IR3Gen.fromStmts(mtd, stmt.children.get(1).children);

            stmt3s.add(ifgoto);
            stmt3s.add(endGoto);
            stmt3s.add(doLoopLabel);
            stmt3s.addAll(whileStmts);
            stmt3s.add(startGoto);
            stmt3s.add(endLabel);
        } else if (stmt.type == Node.NodeType.Read) {
            // readln (<id>);
            Stmt3 stmt3 = new Stmt3(Stmt3.StmtType.ReadLn);
            stmt3s.add(stmt3);

            stmt3.children.add(stmt.children.get(0));
        } else if (stmt.type == Node.NodeType.Print) {
            // println(<Exp>) ;
            Stmt3 stmt3 = new Stmt3(Stmt3.StmtType.PrintLn);
            Pair<Node, List<Stmt3>> ir3exp = IR3Gen.fromExp(mtd, stmt.children.get(0), EnumSet.of(ParsingFlag.Recursive));
            stmt3s.addAll(ir3exp.tail);
            stmt3s.add(stmt3);
            stmt3.children.add(ir3exp.head);
        } else if (stmt.type == Node.NodeType.VarAss) {
            // <id> = <Exp>;
            // Can also be <id>.<id> = <Exp> if this is involved!
            Stmt3 stmt3 = new Stmt3(Stmt3.StmtType.LAssign);
            Pair<Node, List<Stmt3>> ir3exp = IR3Gen.fromExp(mtd, stmt.children.get(1), EnumSet.noneOf(ParsingFlag.class));
            stmt3s.addAll(ir3exp.tail);
            stmt3s.add(stmt3);

            stmt3.children.add(stmt.children.get(0));
            stmt3.children.add(ir3exp.head);
        } else if (stmt.type == Node.NodeType.FdAss) {
            // <Atom>.<id> = <Exp>;
            Stmt3 stmt3 = new Stmt3(Stmt3.StmtType.FAssign);

            Pair<Node, List<Stmt3>> firstExpr = IR3Gen.fromExp(mtd, stmt.children.get(0), EnumSet.of(ParsingFlag.Recursive));
            Pair<Node, List<Stmt3>> secondExpr = IR3Gen.fromExp(mtd, stmt.children.get(2), EnumSet.noneOf(ParsingFlag.class));

            stmt3s.addAll(firstExpr.tail);
            stmt3s.addAll(secondExpr.tail);

            stmt3s.add(stmt3);
            stmt3.children.add(firstExpr.head);
            stmt3.children.add(
                stmt.children.get(1)
            );
            stmt3.children.add(secondExpr.head);
        } else if (stmt.type == Node.NodeType.Call) {
            // <Atom>(<ExpList>) ;
            Stmt3 stmt3 = new Stmt3(Stmt3.StmtType.Call);
            Pair<List<Node>, List<Stmt3>> ir3exp = IR3Gen.fromCall(mtd, stmt);

            stmt3s.addAll(ir3exp.tail);
            stmt3s.add(stmt3);
            stmt3.children.addAll(ir3exp.head);
        } else if (stmt.type == Node.NodeType.RetT) {
            // return <Exp>;
            Stmt3 stmt3 = new Stmt3(Stmt3.StmtType.ReturnT);

            Pair<Node, List<Stmt3>> ir3exp = IR3Gen.fromExp(mtd, stmt.children.get(0), EnumSet.of(ParsingFlag.Recursive, ParsingFlag.Id3Only));
            stmt3s.addAll(ir3exp.tail);
            stmt3s.add(stmt3);
            stmt3.children.add(ir3exp.head);
        } else if (stmt.type == Node.NodeType.RetV) {
            // return ;
            stmt3s.add(new Stmt3(Stmt3.StmtType.ReturnV));
        }

        return stmt3s;
    }

    /**
     * Parses an Expression into an IR3 Expression
     * @param mtd
     * @param exp
     * @param flags
     * @return a pair containing an IR3 statement
     * and a list of statements, when there's a need to create temporary variables
     */
    static Pair<Node, List<Stmt3>> fromExp(CMtd3 mtd, Node exp, EnumSet<ParsingFlag> flags) {
        ArrayList<Stmt3> varDecls = new ArrayList<>();

        Node newExp = null;
        if (exp.type == Node.NodeType.Identifier) {
            return new Pair<>(exp, varDecls);
        } else if (exp.type == Node.NodeType.Str
        || exp.type == Node.NodeType.Integer
        || exp.data == "true"
        || exp.data == "false"
        || exp.type == Node.NodeType.This) {
            if (!flags.contains(ParsingFlag.Id3Only)) {
                return new Pair<>(exp, varDecls);
            }

            newExp = exp;
        } else if (exp.type == Node.NodeType.New) {
            newExp = exp;
        } else if (exp.type == Node.NodeType.Expr) {
            // Just unwrap
            Pair<Node, List<Stmt3>> subExpr = IR3Gen.fromExp(mtd, exp.children.get(0), EnumSet.of(ParsingFlag.Recursive));
            varDecls.addAll(subExpr.tail);
            newExp = subExpr.head;
        } else if (exp.type == Node.NodeType.INeg || exp.type == Node.NodeType.BNeg) {
            // Unary style operators
            newExp = new Node(exp.type);
            Node rhs = exp.children.get(0);
            Pair<Node, List<Stmt3>> subExpr = IR3Gen.fromExp(mtd, rhs, EnumSet.of(ParsingFlag.Recursive));

            varDecls.addAll(subExpr.tail);
            newExp.children.add(subExpr.head);
        } else if (exp.type == Node.NodeType.BExpr
            || exp.type == Node.NodeType.Rel
            || exp.type == Node.NodeType.Arith) {
            // Bop3 type
            newExp = new Node(Node.NodeType.Exp);

            Pair<Node, List<Stmt3>> firstSubExpr = IR3Gen.fromExp(mtd, exp.children.get(0), EnumSet.of(ParsingFlag.Recursive));
            varDecls.addAll(firstSubExpr.tail);
            newExp.children.add(firstSubExpr.head);
            newExp.children.add(
                exp.children.get(1)
            );

            Pair<Node, List<Stmt3>> secondSubExpr = IR3Gen.fromExp(mtd, exp.children.get(2), EnumSet.of(ParsingFlag.Recursive));
            varDecls.addAll(secondSubExpr.tail);
            newExp.children.add(secondSubExpr.head);

            newExp.note = exp.note;
        } else if (exp.type == Node.NodeType.Field) {
            newExp = new Node(Node.NodeType.Field);
            Pair<Node, List<Stmt3>> subExpr = IR3Gen.fromExp(mtd, exp.children.get(0), EnumSet.of(ParsingFlag.Recursive));
            varDecls.addAll(subExpr.tail);

            newExp.children.add(subExpr.head);
            newExp.children.add(exp.children.get(1));
            newExp.note = exp.note;
        } else if (exp.type == Node.NodeType.Call) {
            newExp = new Node(Node.NodeType.Call);
            Pair<List<Node>, List<Stmt3>> callExpr = IR3Gen.fromCall(mtd, exp);
            newExp.children.addAll(callExpr.head);
            newExp.note = exp.note.split("_")[0];
            varDecls.addAll(callExpr.tail);
        }

        if (!flags.contains(ParsingFlag.Recursive)) {
            return new Pair<>(newExp, varDecls);
        } else {
            // Temporary variable
            String newTemp = mtd.addTemporary(exp.note.split("_")[0]);

            Stmt3 tempAss = new Stmt3(Stmt3.StmtType.LAssign);
            tempAss.children.add(new Node(Node.NodeType.Id3, newTemp));
            tempAss.children.add(newExp);
            varDecls.add(tempAss);
            tempAss.children.get(0).note = newExp.note;

            return new Pair<>(tempAss.children.get(0), varDecls);
        }
    }

    static Pair<List<Node>, List<Stmt3>> fromCall(CMtd3 mtd, Node exp) {
        ArrayList<Node> newCallChildren = new ArrayList<>();
        ArrayList<Node> exprList = new ArrayList<>(exp.children.subList(1, exp.children.size()));
        Pair<Node, List<Stmt3>> fieldPair = IR3Gen.fromExp(mtd, exp.children.get(0), EnumSet.noneOf(ParsingFlag.class));
        Node field = fieldPair.head;
        String ctx = field.children.get(0).getLabelledValue();
        String mdname = field.children.get(1).getLabelledValue();

        ArrayList<Stmt3> varDecls = new ArrayList<>();
        varDecls.addAll(fieldPair.tail);

        newCallChildren.add(
            new Node(
                Node.NodeType.Id3,
                IR3Gen.getMangledName(field.note, mdname, exp.msig)
            )
        );

        Node expList = new Node(Node.NodeType.ExpList);
        newCallChildren.add(expList);

        // add context
        expList.children.add(
            new Node(Node.NodeType.Id3, ctx)
        );

        for (Node param : exprList) {
            Pair<Node, List<Stmt3>> child = IR3Gen.fromExp(mtd, param, EnumSet.of(ParsingFlag.Recursive));
            varDecls.addAll(child.tail);
            expList.children.add(child.head);
        }

        return new Pair<>(newCallChildren, varDecls);
    }

    static String getMangledName(String cname, String mdname, String msig) {
        String mangledMsig = String.join("_", msig.split(","));

        return String.format("_%s", String.join("_", cname, mdname, mangledMsig));
    }
}