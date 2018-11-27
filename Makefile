TESTCASES := $(wildcard testcases/*.j)
TESTFILES := $(patsubst %.j,%.asm,$(TESTCASES))
TESTOPTFILES := $(patsubst %.j,%-opt.asm,$(TESTCASES))

SRCFILES := $(wildcard ta_testcase/*.j)
OBJFILES := $(patsubst %.j,%.asm,$(SRCFILES))
OBJOPTFILES := $(patsubst %.j,%-opt.asm,$(TESTCASES))

.PHONY: all compile run clean

all: compile $(OBJFILES) $(OBJOPTFILES) $(TESTFILES) $(TESTOPTFILES)

compile:
	jflex minijava.flex
	java -jar java-cup-11b.jar -locations -interface -parser Parser minijava.cup
	javac -cp java-cup-11b-runtime.jar:. *.java

%.asm: %.j
	java -cp java-cup-11b-runtime.jar:. Parser $< > $@ 2> $@.ir3

%-opt.asm: %.j
	java -cp java-cup-11b-runtime.jar:. Parser -O $< > $@ 2> $@.ir3

clean:
	rm testcases/*.asm testcases/*.ir3
	rm ta_testcase/*.asm ta_testcase/*.ir3
	rm *.class
	rm Lexer.java Parser.java sym.java
