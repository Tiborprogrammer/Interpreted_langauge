import javax.xml.stream.FactoryConfigurationError;
import java.io.*;
import java.lang.Math;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    final Environment globals = new Environment();
    private Environment environment = globals;
    private boolean isInREPL = false;

    Interpreter() {
        globals.define("newFile", new LoxCallable() {
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                try {
                    File myFile = new File(arguments.get(0).toString());
                    return myFile.createNewFile();
                } catch (IOException e) {
                    return e.getCause().toString();
                }
            }

            @Override
            public int arity() {
                return 1;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

        globals.define("getFile", new LoxCallable() {
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                String fileName = arguments.get(0).toString();
                try {
                    byte[] bytes = Files.readAllBytes(Paths.get(fileName));
                    return new String(bytes, Charset.defaultCharset());
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            public int arity() {
                return 1;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

        globals.define("deleteFile", new LoxCallable() {
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                File myObj = new File(arguments.get(0).toString());
                return myObj.delete();
            }

            @Override
            public int arity() {
                return 1;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

        globals.define("writeFile", new LoxCallable() {
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                try {
                    String fileName = arguments.get(0).toString();
                    FileWriter fileWriter = new FileWriter(fileName);
                    fileWriter.write(arguments.get(1).toString());
                    fileWriter.close();
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }

            @Override
            public int arity() {
                return 2;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

        globals.define("runCommand", new LoxCallable() {
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                try {
                    Process process = Runtime.getRuntime().exec("ls /home/mkyong/");
                    StringBuilder output = new StringBuilder();

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));

                    System.out.println(reader.readLine());
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line + "\n");
                    }

                    return output;
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            public int arity() {
                return 1;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

        globals.define("time", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

        globals.define("input", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                Scanner scanner = new Scanner(System.in);
                return scanner.nextLine();
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

        globals.define("type", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                //System.out.println(arguments.get(0).toString());
                if (arguments.get(0) instanceof Double) {
                    return "Number";
                } else if (arguments.get(0) instanceof Boolean) {
                    return "Bool";
                } else if (arguments.get(0).toString().startsWith("<fn")) {
                    return arguments.get(0).toString();
                }

                return "String";
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

        globals.define("number", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                try {
                    return Double.parseDouble(arguments.get(0).toString());
                } catch (Exception exception) {
                    return null;
                }
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

        globals.define("string", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                return stringify(arguments.get(0));
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

        globals.define("bool", new LoxCallable() {
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                String value = arguments.get(0).toString();

                return Boolean.parseBoolean(value);
            }

            @Override
            public int arity() {
                return 1;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

        globals.define("sleep", new LoxCallable() {
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                try {
                    Double time = Double.parseDouble(arguments.get(0).toString());
                    TimeUnit.SECONDS.sleep(time.longValue());
                    return true;
                } catch (NumberFormatException | InterruptedException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            public int arity() {
                return 1;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

    }

    void setREPL(boolean isInREPL) {
        this.isInREPL = isInREPL;
    }
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        // Hack. Work around Java adding ".0" to integer-valued doubles.
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if ((Double)right == 0.0) {
                    return null;
                }
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");
            case STAR_STAR:
                checkNumberOperands(expr.operator, left, right);
                return Math.pow((Double) left, (Double) right);
            case MODULUS:
                checkNumberOperands(expr.operator, left, right);
                return (Double)left % (Double) right;
            case DOT_PLUS:
                String rightValue = right.toString();
                String leftValue = left.toString();
                if (right instanceof Double && right.toString().endsWith(".0")) {
                    rightValue = right.toString().substring(0, right.toString().length() - 2);
                }
                if (left instanceof Double && left.toString().endsWith(".0")) {
                    leftValue = left.toString().substring(0, left.toString().length() - 2);
                }
                return leftValue + rightValue;
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (Double)left > (Double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (Double)left >= (Double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return  (Double)left < (Double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return  (Double)left <= (Double)right;
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);

        }

        // Unreachable.
        return null;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }


    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            case BANG:
                return !isTruthy(right);
        }

        // Unreachable.
        return null;
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator,
                                     Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;

        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private Boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        if (object instanceof Double) {
            return (Double) object != 0;
        }
        return true;
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        Object result = evaluate(stmt.expression);
        if (isInREPL) {
            System.out.println(stringify(result));
        }
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitTernaryStmt(Stmt.Ternary stmt) {
        Object condition = evaluate(stmt.condition);
        if (isTruthy(condition)) {
            execute(stmt.ifTrue);
        } else {
            execute(stmt.ifFalse);
        }
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitAssignShorthandExpr(Expr.AssignShorthand expr) {
        Object rightHandSide = evaluate(expr.value);
        Object value = environment.get(expr.name);
        TokenType operator = expr.operator.type;
        if (value instanceof Double && rightHandSide instanceof Double) {
            Double finalValue = null;
            if (operator == TokenType.SLASH_EQUAL) {
                finalValue = (Double)value / (Double) rightHandSide;
            } else if (operator == TokenType.STAR_EQUAL) {
                finalValue = (Double)value * (Double)rightHandSide;
            } else if (operator == TokenType.PLUS_EQUAL) {
                finalValue = (Double)value + (Double)rightHandSide;
            } else if (operator == TokenType.MINUS_EQUAL) {
                finalValue = (Double)value - (Double)rightHandSide;
            } else if (operator == TokenType.PLUS_PLUS) {
                finalValue = (Double)value + (Double)rightHandSide;
            } else if (operator == TokenType.MINUS_MINUS) {
                finalValue = (Double)value + (Double)rightHandSide;
            }
            if (finalValue != null) environment.assign(expr.name, finalValue);
            return finalValue;
        } else {
            throw new RuntimeError(expr.name, "Types must match.");
        }
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Void visitDoWhileStmt(Stmt.DoWhile stmt) {
        do {
            execute(stmt.body);
        } while (isTruthy(evaluate(stmt.condition)));
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren,
                    "Can only call functions and classes.");
        }

        LoxCallable function = (LoxCallable)callee;

        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                    function.arity() + " arguments but got " +
                    arguments.size() + ".");
        }


        return function.call(this, arguments);
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new Return(value);
    }
}