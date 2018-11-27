import java.util.ArrayList;
import java.util.Arrays;

class NodeStringify {
  public String toString(Node node) {
    ArrayList<String> children = new ArrayList<String>();
    for (Node e : node.children) {
      children.add(e.toString());
    }

    if (children.size() != 0) {
      return String.format("<%s note='%s'>%s</%s>", node.data, node.note, String.join("\n", children), node.data);
    } else {
      return node.data.toString();
    }
  }
}

class Node {
    public String data;
    public NodeType type;
    public String note = "";
    public String msig = "";
    public Node parent;
    public ArrayList<Node> children;

    public enum NodeType {
        Program(NodeType.getSingleNodeStringify()),
        Id3(NodeType.getSingleNodeStringify()),
        Identifier(NodeType.getSingleNodeStringify()),
        MName(NodeType.getSingleNodeStringify()),
        CName(NodeType.getSingleNodeStringify()),
        Integer(NodeType.getSingleNodeStringify()),
        Str(NodeType.getSingleNodeStringify("\"%s\"")),
        This(NodeType.getSingleNodeStringify()),
        Label(NodeType.getSingleNodeStringify()),
        Null(NodeType.getSingleNodeStringify()),
        Type(NodeType.getSingleNodeStringify()),
        AOp(NodeType.getSingleNodeStringify()),
        BOp(NodeType.getSingleNodeStringify()),
        BGrd(NodeType.getSingleNodeStringify()),
        Block(NodeType.getJoinStringify("\n")),
        Stmts(NodeType.getJoinStringify("\n")),
        FmlList(NodeType.getJoinStringify(", ")),
        ExpList(NodeType.getJoinStringify(", ")),
        CDecl(NodeType.getAllChildrenStringify("class %s {\n%s\n}")),
        MDecl(new NodeStringify() {
          @Override
          public String toString(Node node) {
            ArrayList<String> children = new ArrayList<String>();
            for (Node e : node.children.subList(2, node.children.size() - 1)) {
              children.add(e.toString());
            }

            return String.format("%s %s(%s) {\n%s\n}",
              node.children.get(0).toString(),
              node.children.get(1).toString(),
              String.join(",", children),
              node.children.get(node.children.size() - 1).toString()
            );
          }
        }),
        INeg(NodeType.getAllChildrenStringify("-%s")),
        BNeg(NodeType.getAllChildrenStringify("!%s")),
        Arith(NodeType.getAllChildrenStringify("%s %s %s")),
        BExpr(NodeType.getAllChildrenStringify("%s %s %s")),
        Exp(NodeType.getAllChildrenStringify("%s %s %s")),
        Expr(NodeType.getAllChildrenStringify("(%s)")),
        Rel(NodeType.getAllChildrenStringify("%s %s %s")),
        Field(NodeType.getAllChildrenStringify("%s.%s")),
        VDecl(NodeType.getAllChildrenStringify("%s %s")),
        VarAss(NodeType.getAllChildrenStringify("%s = %s")),
        FdAss(NodeType.getAllChildrenStringify("%s.%s = %s")),
        New(NodeType.getAllChildrenStringify("new %s()")),
        RetT(NodeType.getAllChildrenStringify("return %s")),
        RetV(NodeType.getAllChildrenStringify("return")),
        Print(NodeType.getAllChildrenStringify("println(%s)")),
        Read(NodeType.getAllChildrenStringify("readln(%s)")),
        Call(new NodeStringify() {
          @Override
          public String toString(Node node) {
            ArrayList<String> children = new ArrayList<String>();
            for (Node e : node.children.subList(1, node.children.size())) {
              children.add(e.toString());
            }

            return String.format("%s(%s)",
              node.children.get(0).toString(),
              String.join(",", children)
            );
          }
        }),
        Cond(NodeType.getAllChildrenStringify("if (%s) {\n%s\n} else {\n%s\n}")),
        While(NodeType.getAllChildrenStringify("while (%s) {\n%s\n}"));

        private NodeStringify value;

        private NodeType(NodeStringify value) {
            this.value = value;
        }

        private static NodeStringify getSingleNodeStringify() {
          return new NodeStringify() {
            @Override
            public String toString(Node node) {
              return node.getLabelledValue();
            }
          };
        }

        private static NodeStringify getSingleNodeStringify(String fmt) {
          return new NodeStringify() {
            @Override
            public String toString(Node node) {
              return String.format(fmt, node.getLabelledValue());
            }
          };
        }

        private static NodeStringify getAllChildrenStringify(String fmt) {
          return new NodeStringify() {
            @Override
            public String toString(Node node) {
              return String.format(fmt, node.children.toArray());
            }
          };
        }

        private static NodeStringify getJoinStringify(String delimeter) {
          return new NodeStringify() {
            @Override
            public String toString(Node node) {
              ArrayList<String> strList = new ArrayList<>();

              for (Node child : node.children) {
                strList.add(child.toString());
              }

              return String.join(
                delimeter,
                strList
              );
            }
          };
        }

        public NodeStringify getValue() {
            return value;
        }
    }

    public Node(NodeType type) {
      this.data = type.toString();
      this.type = type;
      this.children = new ArrayList<>();
    }

    public Node(NodeType type, Node... children) {
      this.data = type.toString();
      this.type = type;
      this.children = new ArrayList<>(Arrays.asList(children));
    }

    public Node(NodeType type, ArrayList<Node> children) {
      this.data = type.toString();
      this.type = type;
      this.children = children;
    }

    public Node(NodeType type, String terminal) {
      this.data = terminal;
      this.type = type;
      this.children = new ArrayList<Node>();
    }

    public Node(String terminal) {
      this.data = terminal;
      this.type = Node.NodeType.Label;
      this.children = new ArrayList<Node>();
    }

    public void add(Node child) {
      children.add(child);
    }

    public void addFirst(Node child) {
      children.add(0, child);
    }

    public String getLabelledValue() {
      if (this.children.size() > 0) {
        return this.children.get(0).data;
      } else {
        return this.data;
      }
    }

    public String toString() {
      NodeStringify ns = new NodeStringify();

      try {
        return this.type.getValue().toString(this);
      } catch (Exception e) {
        return ns.toString(this);
      }
    }
}