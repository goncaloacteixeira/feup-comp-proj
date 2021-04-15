package ast.exceptions;

public class WrongNumberOfArguments extends Exception {
    public WrongNumberOfArguments(int nArgs) {
        super(String.valueOf(nArgs));
    }
}
