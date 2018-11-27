import java_cup.runtime.ComplexSymbolFactory.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages type checking errors for JLite
 */
public class ErrorManager {
    public class Error {
        public String className;
        public String methodName;

        public Location left;
        public Location right;
        public String errorMessage;
        public Object[] errorArgs;

        public Error(
            String className,
            String methodName,
            Location left,
            Location right,
            String errorMessage,
            Object... errorArgs
        ) {
            this.className = className;
            this.methodName = methodName;
            this.left = left;
            this.right = right;
            this.errorMessage = errorMessage;
            this.errorArgs = errorArgs;
        }

        private String getErrorMessage() {
            return String.format(
                this.errorMessage,
                this.errorArgs
            );
        }

        private String getTrace() {
            return String.format(
                "    in %s:%s, line(s) %d - %d",
                this.className,
                this.methodName,
                this.left.getLine(),
                this.right.getLine()
            );
        }

        public String toString() {
            return String.join(
                System.lineSeparator(),
                this.getErrorMessage(),
                this.getTrace()
            );
        }
    }

    private ArrayList<Error> errors;

    public ErrorManager() {
        this.errors = new ArrayList<>();
    }

    public void addError(String cname, String mname, String fmt, Location left, Location right, Object... args) {
        this.errors.add(
            new Error(cname, mname, left, right, fmt, args)
        );
    }

    public boolean hasErrors() {
        return this.errors.size() > 0;
    }

    public void printAll() {
        for (Error err : this.errors) {
            System.out.println(err.toString());
        }
    }
}