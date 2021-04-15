package ast.exceptions;

public class WrongArgumentType extends Exception {
    public WrongArgumentType(int index) {
        super(String.valueOf(index));
    }
}
