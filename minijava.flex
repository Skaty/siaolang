import java_cup.runtime.Symbol;
import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.ComplexSymbolFactory.Location;

%%
%public
%class Lexer
%cup
%implements sym
%char
%line
%column

%{
    StringBuffer string = new StringBuffer();
    public Lexer(java.io.Reader in, ComplexSymbolFactory sf){
    	this(in);
	    symbolFactory = sf;
    }
    ComplexSymbolFactory symbolFactory;

  private Symbol symbol(String name, int sym) {
       return symbolFactory.newSymbol(name, sym, new Location(yyline+1,yycolumn+1,yychar), new Location(yyline+1,yycolumn+yylength(),yychar+yylength()));
  }

  private Symbol symbol(String name, int sym, Object val) {
      Location left = new Location(yyline+1,yycolumn+1,yychar);
      Location right= new Location(yyline+1,yycolumn+yylength(), yychar+yylength());
      return symbolFactory.newSymbol(name, sym, left, right,val);
  }

  private Symbol symbol(String name, int sym, Object val,int buflength) {
      Location left = new Location(yyline+1,yycolumn+yylength()-buflength,yychar+yylength()-buflength);
      Location right= new Location(yyline+1,yycolumn+yylength(), yychar+yylength());
      return symbolFactory.newSymbol(name, sym, left, right,val);
  }

  private void error(String message) {
    System.out.println("Error at line "+(yyline+1)+", column "+(yycolumn+1)+" : "+message);
  }
%}

%eofval{
     return symbolFactory.newSymbol("EOF", EOF, new Location(yyline+1,yycolumn+1,yychar), new Location(yyline+1,yycolumn+1,yychar+1));
%eofval}

Identifier = [a-z][A-Za-z0-9_]*
ClassName = [A-Z][A-Za-z0-9_]*

IntLiteral = 0 | [1-9][0-9]*
new_line = \r|\n|\r\n;
white_space = {new_line} | [ \t\f]

// Comments
Comment = {SingleLineComment} | {MultiLineComment}
SingleLineComment = "//" ~{new_line}
MultiLineComment = "/*" ~"*/"

%state STRING

%%

<YYINITIAL>{
    /* keywords */
    "class"           { return symbol("CLASS", CLASS); }
    "main"            { return symbol("MAIN", MAIN); }
    "return"          { return symbol("RETURN", RETURN); }
    "this"            { return symbol("THIS", THIS); }
    "new"             { return symbol("NEW", NEW); }
    "null"            { return symbol("NULL", NULL); }
    "println"         { return symbol("PRINTLN", PRINTLN); }
    "readln"          { return symbol("READLN", READLN); }

    /* types */
    "Int"             { return symbol("TINT", TINT); }
    "Bool"            { return symbol("TBOOL", TBOOL); }
    "String"          { return symbol("TSTRING", TSTRING); }
    "Void"     { return symbol("TVOID", TVOID); }

    /* conditionals */
    "if"|"If"         { return symbol("IF", IF); }
    "else"            { return symbol("ELSE", ELSE); }
    "while"|"While"   { return symbol("WHILE", WHILE); }
    "true"            { return symbol("TRUE", TRUE); }
    "false"           { return symbol("FALSE", FALSE); }

    {Identifier}      { return symbol("Ident", IDENTIFIER, yytext()); }
    {ClassName}       { return symbol("CNAME", CNAME, yytext()); }
    /* literals */
    {IntLiteral}      { return symbol("Intconst",INTCONST, new Integer(Integer.parseInt(yytext()))); }
    /* boolean */
    "<="               { return symbol("LEQ", LEQ); }
    ">="               { return symbol("GEQ", GEQ); }
    "<"                { return symbol("LT",LT); }
    ">"                { return symbol("GT",GT); }
    "=="               { return symbol("DEQ",DEQ); }
    "!="               { return symbol("NEQ",NEQ); }

    /* logic operators */
    "&&"              { return symbol("BAND", BAND); }
    "||"              { return symbol("BOR", BOR); }

    /* characters */
    "!"               { return symbol("!", NOT); }
    ","               { return symbol(",", COMMA); }
    ";"               { return symbol(";", SEMICOLON); }
    "."               { return symbol(".", DOT); }
    "="               { return symbol("=", EQ); }

    /* string literals */
    \"                { string.setLength(0); yybegin(STRING); }

    /* separators */
    "{"               { return symbol("{", LBRACE); }
    "}"               { return symbol("}", RBRACE); }
    "("               { return symbol("(",LPAREN); }
    ")"               { return symbol(")",RPAREN); }
    "+"               { return symbol("+",PLUS); }
    "-"               { return symbol("+",MINUS); }
    "*"               { return symbol("+",TIMES); }
    "/"               { return symbol("+",DIV); }

    {white_space}     { /* ignore */ }
    {Comment}         { /* ignore */ }
}

<STRING> {
    \"                             { yybegin(YYINITIAL);
                                     return symbol(
                                        "STRINGLITERAL",
                                        STRINGLITERAL,
                                        string.toString()
                                     );
                                   }
    [^\n\r\"\\]+                   { string.append( yytext() ); }
    \\[0-9]+                { string.append((char)Integer.parseInt(yytext().substring(1)));  }
    \\x[0-9a-fA-F]+                { string.append((char)Integer.parseInt(yytext().substring(2), 16));  }
    \\t                            { string.append('\t'); }
    \\n                            { string.append('\n'); }
    \\b                            { string.append('\b'); }
    \\r                            { string.append('\r'); }
    \\\"                           { string.append('\"'); }
    \\                             { string.append('\\'); }
}

/* error fallback */
[^] {
    error("Illegal character <"+ yytext()+">");
}