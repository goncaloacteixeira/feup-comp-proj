# Compilers Project

|Name | UP Number | Grade | Contribution
|----|------|-----|-----------|
André Daniel Alves Gomes | 201806224 | 20 | 25%
André Filipe Meireles do Nascimento | 201806461 | 20 | 25%
Gonçalo André Carneiro Teixeira | 201806562 | 20 | 25%
Luís Filipe Sousa Teixeira Recharte | 201806743| 20 | 25%

GLOBAL Grade of the project: 18

## Summary

This project's main goal was to apply the theoretical principals of the course Compilers. This was achieved by building
a compiler for programs written in the Java-- language. The main parts of the project are Syntactic error controller,
Semantic analysis, and Code generation.

## Dealing with syntatic errors

As requested in the project specification, we only recover from errors in the conditional statement of a while cycle. We
keep a counter of the '(' encountered in the while conditional, decrementing everytime we encounter a ')', and if an
error is found in the conditional statement of a while cycle, the compiler ignores every token until the next ')', while
the counter is above 0 , showing an error message indicating which tokens were expected and the line and column where
the error occurred.

## Semantic Analysis

### Type Verification

- Operations must be between elements of the same type (e.g. int + boolean is not allowed)
- Doesn't allow operations between arrays (e.g. array1 + array2)
- Array access is only allowed to arrays (e.g. 1[10] is not allowed)
- Only int values to index accessing array (e.g. a[true] is not allowed)
- Only int values to initialize an array
- Assignment between the same type (a_int = b_boolean is not allowed)
- Boolean operation (&&, < ou !) only with booleans
- Conditional expressions only accepts variables, operations or function calls that return boolean
- Raises an error if variable has not been initialized
- Assumes parameters are initialized

### Method Verification

- Check if the method "target" exists and if it contains the corresponding method (e.g. a.foo, check if 'a' exists and
  if it has a method 'foo')
- Check if method exists in the declared class (e.g. a usar o this), else: error if no extends is declared, if extends
  assume that the method is from the super class
- If the method is not from the declared class (meaning its from an imported class), assume that the method exists and
  assume the expected types (e.g. a = Foo.b(), if 'a' is an int, and 'Foo' an imported class, assume 'b' method as
  static, that has no arguments and returns an int)
- Verify that the number of arguments in an invoke is the same as the number of parameters of the declaration (method
  overload)
- Verify that the type of the parameters matches the type of the arguments (method overload)

### Fields

- Checks if the variable is initialized, throwing an error if the variable is used without being initialized;
- The `initialized` flag is saved on the symbol table.

## Code Generation

Starting with the Java-- code file, we developed a parser for this language, taking into account the furnished grammar.
With the code exempt of lexical and syntactic errors, we perform the generation of the syntax tree while annotating some
nodes and leafs with extra information. With this AST we can create the Symbol Table and perform a Semantic Analysis.
The next step needed is to generate OLLIR code derived from the AST and Symbol table and the final step is to generate
the set of JVM instructions to be accepted by jasmin.

All the necessary code is generated in the Main and the AST, SymbolTable, OLLIR code, Jasmin code and class files are
saved inside a folder in the root of the project with the name of the jmm file. The class file is also saved under
test/fixtures/libs/compiled/ so that other files that extend those classes can use it. For example, running the
HelloWorld.jmm will generate a HelloWorld folder in the root of the projct, and within that folder the HelloWorld.json,
HelloWorld.symbols.txt, HelloWorld.ollir, HelloWorld.j, HelloWorld.class, and we went for the extra mile adding an
"enhanced" symbol table, which features the method overloading, and every field regarding each method (fields will also
be marked as initialized or not).

## Task Distribution

| Task                                    | Member                                                  | 
|-----------------------------------------|-------------------------------------------------------- |
| Translation of the grammar into tokens  | André Gomes, André Nascimento, Gonçalo Teixeira, Luís Recharte |
| Syntatic Analysis                       | André Gomes, André Nascimento, Gonçalo Teixeira, Luís Recharte |
| Error Handling                          | André Gomes, André Nascimento, Gonçalo Teixeira, Luís Recharte |
| Abstract Syntax Tree                    | André Gomes, André Nascimento, Gonçalo Teixeira, Luís Recharte | 
| Symbol Tables                           | Gonçalo Teixeira |
| Semantic Analysis                       | André Gomes, André Nascimento, Gonçalo Teixeira, Luís Recharte |
| Ollir code Generation                   | André Gomes, Gonçalo Teixeira |
| Jasmin code Generation                  | André Nascimento, Luís Recharte |

## Pros

As the most positive aspects of our project we highlight:

- Very complete and detailed AST
- Robust semantic analysis, with method overload

## Cons

Sadly we didn't get to implement the optimizations, which would have made this an even better project.

## Project setup

For this project, you need to [install Gradle](https://gradle.org/install/)

Copy your `.jjt` file to the `javacc` folder. If you change any of the classes generated by `jjtree` or `javacc`
, you also need to copy them to the `javacc` folder.

Copy your source files to the `src` folder, and your JUnit test files to the `test` folder.

## Compile

To compile the program, run `gradle build`. This will compile your classes to `classes/main/java` and copy the JAR
file to the root directory. The JAR file will have the same name as the repository folder.

### Run

To run you have two options: Run the `.class` files or run the JAR.

### Run `.class`

To run the `.class` files, do the following:

```cmd
java -cp "./build/classes/java/main/" <class_name> <arguments>
```

Where `<class_name>` is the name of the class you want to run and `<arguments>` are the arguments to be passed
to `main()`.

### Run `.jar`

To run the JAR, do the following command:

```cmd
java -jar <jar filename> <arguments>
```

Where `<jar filename>` is the name of the JAR file that has been copied to the root folder, and `<arguments>` are
the arguments to be passed to `main()`.

## Test

To test the program, run `gradle test`. This will execute the build, and run the JUnit tests in the `test` folder.
If you want to see output printed during the tests, use the flag `-i` (i.e., `gradle test -i`). You can also see a
test report by opening `build/reports/tests/test/index.html`.

### Custom tests

We created tests that extend and import other classes, thus in order to run those tests you need to first compile the
imported/extended classes. Afterwards all the class files should be under `test/fixtures/libs/compiled/`directory and
ready to run.