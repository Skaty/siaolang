import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that takes in IR3 and generates
 * ARMv5 assembly.
 */

class CodeGen {
    static class BasicBlockState {
        String[] registerDescriptor = new String[8];
        int[] registerLastUse = new int[8];
        HashMap<String, Integer> addressDescriptor = new HashMap<>();

        public BasicBlockState() {
            for (int i = 0; i < 8; i++) {
                this.registerDescriptor[i] = null;
                this.registerLastUse[i] = -1;
            }
        }
    }

    static class CodeGenState {
        StringBuilder textSection = new StringBuilder();
        ArrayList<String> dataSection = new ArrayList<String>();

        // Per file variables
        int offset = 0, numLabelled = 0;
        HashMap<String, CData3> cdataMapping = new HashMap<>();

        // Per method variables
        ArrayList<String> stackLocations = new ArrayList<>();
        CMtd3 currentMethod = null;

        // Per basic block
        BasicBlockState bbs;

        public CodeGenState(IR3 ir3) {
            for (CData3 cdata : ir3.cdata3s) {
                cdataMapping.put(cdata.cname3, cdata);
            }
            initializeCodeGen(null);
        }

        public String generateDataSection() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < dataSection.size(); i++) {
                sb.append(String.format(
                    "\nL%d:\n\t.asciz \"%s\"\n",
                    i,
                    dataSection.get(i)
                ));
            }

            return sb.toString();
        }

        public void initializeCodeGen(CMtd3 cmtd) {
            stackLocations.clear();
            this.initNewBB();

            for (int i = 0; i < 8; i++) {
                this.bbs.registerDescriptor[i] = null;
                this.bbs.registerLastUse[i] = -1;
            }

            if (cmtd != null) {
                for (int i = 0; i < cmtd.fmllist3.size(); i++) {
                    String var = cmtd.fmllist3.get(i).id;
                    if (i < 4) {
                        this.bbs.registerDescriptor[i] = var;
                        this.bbs.registerLastUse[i] = 0;
                        this.bbs.addressDescriptor.put(var, i);
                    }
                    stackLocations.add(var);
                }

                for (VarDecl3 vdecl : cmtd.vardecl3s) {
                    stackLocations.add(vdecl.id);
                }
            }

            this.currentMethod = cmtd;
        }

        public BasicBlockState initNewBB() {
            this.bbs = new BasicBlockState();
            return this.bbs;
        }

        public BasicBlockState copyBasicBlockState(BasicBlockState oldBBS) {
            BasicBlockState newBBS = new BasicBlockState();

            newBBS.registerDescriptor = Arrays.copyOf(
                oldBBS.registerDescriptor,
                oldBBS.registerDescriptor.length
            );

            newBBS.registerLastUse = Arrays.copyOf(
                oldBBS.registerLastUse,
                oldBBS.registerLastUse.length
            );

            newBBS.addressDescriptor = new HashMap<>(oldBBS.addressDescriptor);

            this.bbs = newBBS;
            return newBBS;
        }
    }

    static String fromIR3(IR3 ir3) {
        CodeGenState cgs = new CodeGenState(ir3);

        for (CMtd3 cm3 : ir3.cmtd3s) {
            CodeGen.fromCMtd3(cgs, cm3);

            // Increment offset
            cgs.offset += cgs.numLabelled;
            cgs.numLabelled = 0;
        }

        return String.format(
            ".data\n\n%s\n\t.text\n\t.global main\n\n%s",
            cgs.generateDataSection(),
            cgs.textSection.toString()
        );
    }

    static void fromCMtd3(CodeGenState cgs, CMtd3 cmtd) {
        BasicBlock first = BasicBlock.fromCMtd3(cmtd);

        cgs.initializeCodeGen(cmtd);

        if (first == null) {
            return;
        }

        // Callee Prologue
        CodeGen.buildPrologue(cgs, cmtd);

        boolean blockFlag = first.recToggle;
        ArrayDeque<BasicBlock> blockQueue = new ArrayDeque<>();
        blockQueue.add(first);
        first.bbsOut = cgs.bbs;

        // TODO: Save register states when branching!
        while (!blockQueue.isEmpty()) {
            BasicBlock curBlock = blockQueue.pollFirst();

            if (curBlock.recToggle != blockFlag) {
                continue;
            }

            if (curBlock.previous.size() > 1) {
                /**
                 * Registers might not be correct!
                 * We should init it out
                 */
                curBlock.bbsOut = cgs.initNewBB();
            } else if (curBlock.previous.size() == 1 && !curBlock.isFirst) {
                // Carry over previous block's state
                curBlock.bbsOut = cgs.copyBasicBlockState(
                    curBlock.previous.get(0).bbsOut
                );
            }

            curBlock.recToggle = !curBlock.recToggle;

            for (Stmt3 stmt : curBlock.stmts) {
                CodeGen.fromStmt(cgs, stmt);
            }

            // Check next block to see if we need to save all registers
            for (BasicBlock next : curBlock.next) {
                if (next.previous.size() > 1) {
                    // Save registers
                    CodeGen.saveAllRegisters(cgs);
                    break;
                }
            }

            blockQueue.addAll(curBlock.next);
        }

        // Callee Epilogue
        CodeGen.buildEpilogue(cgs, cmtd);
    }

    private static void buildPrologue(CodeGenState cgs, CMtd3 cmtd) {
        StringBuilder sb = cgs.textSection;
        sb.append(String.format("%s:\n", cmtd.id));
        sb.append("\tstmfd sp!, {fp,lr,v1,v2,v3,v4,v5}\n");
        sb.append("\tadd fp,sp,#24\n");
        sb.append(
            String.format("\tsub sp,fp,#%d\n",
                24 + ((cmtd.vardecl3s.size() + cmtd.fmllist3.size()) * 4)
            )
        );
    }

    private static void buildEpilogue(CodeGenState cgs, CMtd3 cmtd) {
        StringBuilder sb = cgs.textSection;
        sb.append(String.format("%s_exit:\n", cmtd.id));
        if (cmtd.type.toLowerCase().equals("void")) {
            sb.append("\tmov r0,#0\n");
        }
        sb.append("\tsub sp,fp,#24\n");
        sb.append("\tldmfd sp!, {fp,pc,v1,v2,v3,v4,v5}\n");
    }

    private static String getRegister(CodeGenState cgs, Node varNode, int lifetime) {
        // Check if we loaded it
        if (varNode != null &&
            (varNode.type == Node.NodeType.Identifier
            || varNode.type == Node.NodeType.Id3
            || varNode.type == Node.NodeType.This)) {
            String variable = varNode.getLabelledValue();
            if (cgs.bbs.addressDescriptor.containsKey(variable)) {
                Integer reg = cgs.bbs.addressDescriptor.get(variable);
                cgs.bbs.registerLastUse[reg] = 0;
                return String.format("r%d", reg);
            }
        }

        // Find a free Reigster
        int oldestIdx = -1, lastUse = -1;

        for (int i = 0; i < 8; i++) {
            if (cgs.bbs.registerLastUse[i] == -1) {
                // Found an unwanted register!
                oldestIdx = i;
                break;
            }

            if (lastUse < cgs.bbs.registerLastUse[i]) {
                oldestIdx = i;
                lastUse = cgs.bbs.registerLastUse[i];
            }

            cgs.bbs.registerLastUse[i]++;
        }

        CodeGen.allocateRegister(cgs, oldestIdx, varNode, lifetime);
        return String.format("r%d", oldestIdx);
    }

    private static String getRegister(CodeGenState cgs, Node varNode) {
        return CodeGen.getRegister(cgs, varNode, 2);
    }

    private static void allocateRegister(CodeGenState cgs, int rIdx, Node varNode) {
        CodeGen.allocateRegister(cgs, rIdx, varNode, 2);
    }

    private static void saveAllRegisters(CodeGenState cgs) {
        CodeGen.saveFirstNRegisters(cgs, 8);
    }

    private static void saveCalleeSavedRegisters(CodeGenState cgs) {
        CodeGen.saveFirstNRegisters(cgs, 4);
    }

    private static void saveFirstNRegisters(CodeGenState cgs, int n) {
        for (int i = 0; i < n; i++) {
            if (cgs.bbs.registerLastUse[i] > -1 && cgs.bbs.registerDescriptor[i] != null) {
                int storeLocation = cgs.stackLocations.indexOf(cgs.bbs.registerDescriptor[i]);
                cgs.textSection.append(String.format("\tstr r%d,[sp,#%d]\n", i, 4 * storeLocation));

                CodeGen.clearRegister(cgs, i);
            }
        }
    }

    private static void allocateRegister(CodeGenState cgs, int rIdx, Node varNode, int lifetime) {
        // If -1, it means that the previous value is "unwanted"
        if (cgs.bbs.registerLastUse[rIdx] > -1 && cgs.bbs.registerDescriptor[rIdx] != null) {
            int storeLocation = cgs.stackLocations.indexOf(cgs.bbs.registerDescriptor[rIdx]);
            cgs.bbs.addressDescriptor.remove(cgs.bbs.registerDescriptor[rIdx]);
            cgs.textSection.append(String.format("\tstr r%d,[sp,#%d]\n", rIdx, 4 * storeLocation, cgs.bbs.registerDescriptor[rIdx]));
        }

        if (varNode == null) {
            // Temporary register: consume based on "lifetime"
            cgs.bbs.registerDescriptor[rIdx] = null;
            cgs.bbs.registerLastUse[rIdx] = -1 * lifetime;
            return;
        }

        if (varNode.type == Node.NodeType.Id3
            || varNode.type == Node.NodeType.Identifier
            || varNode.type == Node.NodeType.This) {
            String variable = varNode.getLabelledValue();
            if (!cgs.bbs.addressDescriptor.containsKey(variable)) {
                int loadLocation = cgs.stackLocations.indexOf(variable);
                cgs.textSection.append(String.format("\tldr r%d,[sp,#%d]\n", rIdx, 4 * loadLocation));
            } else {
                // We have a previous record, use it instead!
                int oldIdx = cgs.bbs.addressDescriptor.get(variable);
                CodeGen.clearRegister(cgs, oldIdx);
                cgs.textSection.append(String.format("\tmov r%d, r%d\n", rIdx, oldIdx));
            }

            cgs.bbs.registerDescriptor[rIdx] = variable;
            cgs.bbs.registerLastUse[rIdx] = 0;
            cgs.bbs.addressDescriptor.put(variable, rIdx);
        } else if (varNode.type == Node.NodeType.Integer) {
            cgs.bbs.registerLastUse[rIdx] = -2;
            cgs.textSection.append(String.format("\tmov r%d, #%s\n", rIdx, varNode.getLabelledValue()));
        }
    }

    private static void clearRegister(CodeGenState cgs, int regIdx) {
        cgs.bbs.addressDescriptor.remove(cgs.bbs.registerDescriptor[regIdx]);
        cgs.bbs.registerDescriptor[regIdx] = null;
        cgs.bbs.registerLastUse[regIdx] = -1;
    }

    private static void fromStmt(CodeGenState cgs, Stmt3 stmt) {
        switch (stmt.type) {
            case Label:
                cgs.textSection.append(String.format("\t.%s:\n",
                    Integer.parseInt(stmt.children.get(0).getLabelledValue()) + cgs.offset
                ));
                cgs.numLabelled++;
                break;

            case IfGoto:
                Node cond = stmt.children.get(0);
                Node dest = stmt.children.get(1);

                switch (cond.type) {
                    case BGrd:
                        if (cond.getLabelledValue().equals("true")) {
                            // True
                            cgs.textSection.append(String.format("\tb .%s\n",
                                Integer.parseInt(dest.getLabelledValue())
                                + cgs.offset
                            ));
                        } else {
                            // False -> don't add any instructions!
                            // TODO: Optimize so that we remove the block
                        }
                        break;

                    case Identifier:
                    case Id3:
                        /**
                         * Translates to:
                         * CMP <reg>, #1
                         * BEQ <position>
                         */
                        cgs.textSection.append(String.format("\tcmp %s, #1\n",
                            CodeGen.getRegister(cgs, cond)
                        ));
                        cgs.textSection.append(String.format("\tbeq .%d\n",
                            Integer.parseInt(dest.getLabelledValue())
                            + cgs.offset
                        ));
                        break;

                    default:
                        break;
                }
                break;

            case Goto:
                cgs.textSection.append(String.format("\tb .%s\n",
                    Integer.parseInt(stmt.children.get(0).getLabelledValue()) + cgs.offset
                ));
                break;

            case LAssign:
                /**
                 * a = <Exp> translates to
                 * <do exp stuff>
                 * mov <reg>, <result>
                 */
                String lastExpr = CodeGen.fromExpr(
                    cgs,
                    stmt.children.get(1)
                );

                cgs.textSection.append(String.format(
                    lastExpr,
                    CodeGen.getRegister(cgs, stmt.children.get(0))
                ));
                break;

            case FAssign:
                /**
                 * a.b = <Exp> translates to
                 * <prepare reference to memory>
                 * <do exp stuff>
                 * STR <reg>, <result>
                 */
                String cname = stmt.children.get(0).note;
                String fname = stmt.children.get(1).getLabelledValue();

                String rhsExpr = CodeGen.fromExpr(
                    cgs,
                    stmt.children.get(2)
                );

                String refReg = CodeGen.getRegister(cgs, stmt.children.get(0));
                String resReg = CodeGen.getRegister(cgs, null);

                cgs.textSection.append(String.format(rhsExpr, resReg));

                cgs.textSection.append(
                    String.format("\tstr %s,[%s,#%d]\n",
                        resReg,
                        refReg,
                        4 * cgs.cdataMapping.get(cname).getFieldPosition(fname)
                    )
                );
                break;

            case ReturnT:
                cgs.textSection.append(String.format("\tmov a1,%s\n",
                    CodeGen.getRegister(cgs, stmt.children.get(0))
                ));
            case ReturnV:
                cgs.textSection.append(String.format("\tb %s\n",
                    String.format("%s_exit", cgs.currentMethod.id)
                ));
                break;

            case Call:
                CodeGen.fromCall(cgs, stmt.children);
                CodeGen.clearRegister(cgs, 0);
                break;

            case PrintLn:
                Node printNode = stmt.children.get(0);

                switch (printNode.type) {
                    case Id3:
                    case Identifier:
                        String type = printNode.note;
                        CodeGen.allocateRegister(cgs, 0, null);
                        cgs.textSection.append(
                            String.format("\tldr a1, =L%d\n", CodeGen.getFormatString(cgs, type, false))
                        );
                        CodeGen.allocateRegister(cgs, 1, printNode);
                        break;
                    case Str:
                        cgs.textSection.append(
                            String.format("\tldr a1, =L%d\n", CodeGen.getFormatString(cgs, "String", false))
                        );
                        int dataIdx = CodeGen.addDataType(cgs, stmt.children.get(0).getLabelledValue());
                        CodeGen.allocateRegister(cgs, 1, printNode);
                        cgs.textSection.append(
                            String.format("\tldr a2, =L%d\n", dataIdx)
                        );
                        break;
                    case BGrd:
                        cgs.textSection.append(
                            String.format("\tldr a1, =L%d\n", CodeGen.getFormatString(cgs, "BGrd", printNode.getLabelledValue().equals("true")))
                        );
                        break;
                }

                CodeGen.saveCalleeSavedRegisters(cgs);
                cgs.textSection.append("\tbl printf(PLT)\n");
                break;

            case ReadLn:
                break;

            case NAssign:
                // Unused
                break;
        }
    }

    private static String fromExpr(CodeGenState cgs, Node exp) {
        /**
         * TODO: Write something here
         */
        switch (exp.type) {
            case Identifier:
            case Id3:
                return String.format("\tmov %%1$s,%s\n",
                    CodeGen.getRegister(cgs, exp)
                );

            case BGrd:
                return String.format("\tmov %%1$s,#%d\n",
                    (exp.getLabelledValue().equals("true")) ? 1 : 0
                );

            case BNeg:
                /**
                 * Performs a check and sets the value as required
                 */
                return String.format(
                    "\tcmp %s,#0\n\tmoveq %%1$s,#1\n\tmovne %%1$s,#0\n",
                    CodeGen.getRegister(cgs, exp.children.get(0))
                );

            case INeg:
                String tempReg = CodeGen.getRegister(cgs, null);
                return String.format(
                    "\tmov %s, #-1\n\tmul %%1$s, %s, %s\n",
                    tempReg,
                    CodeGen.getRegister(cgs, exp.children.get(0)),
                    tempReg
                );

            case Exp:
                /**
                 * BOp3 type: <idc3> <op> <idc3>
                 * Translates to
                 * <Load first id> -> first temp [lifetime = 2]
                 * <Load second id> -> second temp [lifetime = 1]
                 * <Do oper on first and second>
                 */
                String lhsReg = CodeGen.fromId3c(cgs, exp.children.get(0), 3);
                String rhsReg = CodeGen.fromId3c(cgs, exp.children.get(2), 2);

                String operInstr = "";

                // Select appropriate operation
                switch (exp.children.get(1).getLabelledValue()) {
                    case "<":
                        operInstr = String.format(
                            "\tcmp %s, %s\n\tmovlt %%1$s, #1\n\tmovge %%1$s, #0\n",
                            lhsReg, rhsReg
                        );
                        break;

                    case ">":
                        operInstr = String.format(
                            "\tcmp %s, %s\n\tmovgt %%1$s, #1\n\tmovle %%1$s, #0\n",
                            lhsReg, rhsReg
                        );
                        break;

                    case "<=":
                        operInstr = String.format(
                            "\tcmp %s, %s\n\tmovle %%1$s, #1\n\tmovgt %%1$s, #0\n",
                            lhsReg, rhsReg
                        );
                        break;

                    case ">=":
                        operInstr = String.format(
                            "\tcmp %s, %s\n\tmovge %%1$s, #1\n\tmovlt %%1$s, #0\n",
                            lhsReg, rhsReg
                        );
                        break;

                    case "==":
                        operInstr = String.format(
                            "\tcmp %s, %s\n\tmoveq %%1$s, #1\n\tmovne %%1$s, #0\n",
                            lhsReg, rhsReg
                        );
                        break;

                    case "!=":
                        operInstr = String.format(
                            "\tcmp %s, %s\n\tmovne %%1$s, #1\n\tmoveq %%1$s, #0\n",
                            lhsReg, rhsReg
                        );
                        break;

                    case "+":
                        operInstr = String.format(
                            "\tadd %%1$s, %s, %s\n",
                            lhsReg, rhsReg
                        );
                        break;

                    case "-":
                        operInstr = String.format(
                            "\tsub %%1$s, %s, %s\n",
                            lhsReg, rhsReg
                        );
                        break;

                    case "*":
                        operInstr = String.format(
                            "\tmul %%1$s, %s, %s\n",
                            lhsReg, rhsReg
                        );
                        break;

                    case "/":
                        break;

                    case "&&":
                        operInstr = String.format(
                            "\tand %%1$s,%s,%s\n\tcmp %%1$s,#0\n\tmoveq %%1$s,#0\n\tmovne %%1$s,#1\n",
                            lhsReg, rhsReg
                        );
                        break;

                    case "||":
                        operInstr = String.format(
                            "\tor %%1$s,%s,%s\n\tcmp %%1$s,#0\n\tmoveq %%1$s,#0\n\tmovne %%1$s,#1\n",
                            lhsReg, rhsReg
                        );
                        break;
                }

                return operInstr;

            case Integer:
                return String.format("\tmov %%1$s,#%s\n",
                    exp.getLabelledValue()
                );

            case Call:
                CodeGen.fromCall(cgs, exp.children);
                return "\tmov %1$s, a1\n";

            case New:
                CodeGen.allocateRegister(cgs, 0, null, 2);
                cgs.textSection.append(
                    String.format(
                        "\tmov a1, #%s\n",
                        4 * cgs.cdataMapping.get(exp.getLabelledValue()).variables.size()
                    )
                );
                CodeGen.saveCalleeSavedRegisters(cgs);
                cgs.textSection.append("\tbl malloc(PLT)\n");
                return "\tmov %1$s, a1\n";

            case Field:
                String cname = exp.children.get(0).note;
                String fname = exp.children.get(1).getLabelledValue();

                String refReg = CodeGen.getRegister(cgs, exp.children.get(0));

                return String.format("\tldr %%1$s,[%s,#%d]\n",
                    refReg,
                    4 * cgs.cdataMapping.get(cname).getFieldPosition(fname)
                );

            default:
                return "";
        }
    }

    /**
     * Converts an id3c node to assembly
     * Difference between this and fromExpr is that
     * this returns the temporary variable.
     */
    private static String fromId3c(CodeGenState cgs, Node exp, int lifetime) {
        switch (exp.type) {
            case Identifier:
            case Id3:
                return CodeGen.getRegister(cgs, exp);

            case Integer:
                String outputRegister = CodeGen.getRegister(cgs, null, lifetime);
                cgs.textSection.append(String.format("\tmov %s,#%s\n",
                    outputRegister,
                    exp.getLabelledValue()
                ));
                return outputRegister;

            default:
                return "";
        }
    }

    private static void fromCall(CodeGenState cgs, ArrayList<Node> children) {
        ArrayList<Node> fmlNodes = children.get(1).children;

        for (int i = fmlNodes.size() - 1; i >= 0; i--) {
            if (i < 4) {
                // Store in register
                CodeGen.allocateRegister(cgs, i, fmlNodes.get(i));
            } else {
                // push on stack
                cgs.textSection.append(String.format("\tldr %s,[sp,#-4]\n",
                    CodeGen.getRegister(cgs, fmlNodes.get(i))
                ));
            }
        }

        CodeGen.saveCalleeSavedRegisters(cgs);
        cgs.textSection.append(String.format("\tbl %s(PLT)\n", children.get(0).getLabelledValue()));

        // Reserve a1, so that it won't get consumed by getRegister
        cgs.bbs.registerLastUse[0] = 0;
    }

    private static int getFormatString(CodeGenState cgs, String type, boolean isTrue) {
        switch (type) {
            case "Int":
            case "Bool":
                return CodeGen.addDataType(cgs, "%i");

            case "BGrd":
                if (isTrue) {
                    return CodeGen.addDataType(cgs, "1");
                } else {
                    return CodeGen.addDataType(cgs, "0");
                }

            case "String":
                return CodeGen.addDataType(cgs, "%s");

            default:
                return -1;
        }
    }

    private static int addDataType(CodeGenState cgs, String data) {
        cgs.dataSection.add(data);
        return cgs.dataSection.size() - 1;
    }
}