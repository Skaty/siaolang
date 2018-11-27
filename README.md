# CS4212: JLite (siaolang) Compiler
> compiles some JLite thing to ARM assembly

## Warning

This is a cancerous compiler. I only had in total 12 weeks to do this.

### Etymology

I call it siaolang because I refer to JLite as siao (hence siao lang).

Alternatively, it can also mean 嬲人 (siáu-lâng), which refers to:
- the person who created this compiler
- the person who uses this compiler. Come on, really?

## What you need

* [JFlex](http://www.jflex.de/) -- the lexer
* [CUP](http://www2.cs.tum.edu/projects/cup/) -- the parser
* Java 8

## How to run

`make` runs all the enclosed test suites and redirects the
outputs to the respective files:
- `*.asm` contains the ARM assembly
- `*.ir3` is the IR3 printed out for debugging purposes. Any `STDERR` is also placed here.

`make clean` cleans all of the output asm and ir3 files

### To compile a specific file

To compile without optimizations:
```
java -cp java-cup-11b-runtime.jar:. Parser [input file] > [output assembly file] 2> [errors + IR3]
```

To compile **with** optimizations:
```
java -cp java-cup-11b-runtime.jar:. Parser -O [input file] > [output assembly file] 2> [errors + IR3]
```

## Conditions of Use

You really want to use this? You must be a *siao* lang.