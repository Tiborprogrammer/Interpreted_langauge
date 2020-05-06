import java.util.List;

abstract class Expr {
  interface Visitor<R> {
    R visitBinaryExpr(Expr.Binary expr);
    R visitGroupingExpr(Expr.Grouping expr);
    R visitLiteralExpr(Expr.Literal expr);
    R visitUnaryExpr(Expr.Unary expr);
    R visitVariableExpr(Expr.Variable expr);
    R visitAssignExpr(Expr.Assign expr);
    R visitAssignShorthandExpr(Expr.AssignShorthand expr);
    R visitLogicalExpr(Expr.Logical expr);
    R visitCallExpr(Expr.Call expr);
  }

  static class Call extends Expr {
    Call(Expr callee, Token paren, List<Expr> arguments) {
      this.callee = callee;
      this.paren = paren;
      this.arguments = arguments;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitCallExpr(this);
    }

    final Expr callee;
    final Token paren;
    final List<Expr> arguments;
  }

  static class Assign extends Expr {
    Assign(Token name, Expr value) {
      this.name = name;
      this.value = value;
    }

      @Override
    <R> R accept(Visitor<R> visitor) {
        return visitor.visitAssignExpr(this);
      }

      final Token name;
      final Expr value;
  }

  static class AssignShorthand extends Expr {
    AssignShorthand(Token name, Token operator,Expr value) {
      this.name = name;
      this.operator = operator;
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitAssignShorthandExpr(this);
    }

    final Token name;
    final Expr value;
    final Token operator;
  }

  static class Grouping extends Expr {
    Grouping(Expr expression) {
      this.expression = expression;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGroupingExpr(this);
    }

    final Expr expression;
  }

  static class Literal extends Expr {
    Literal(Object value) {
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteralExpr(this);
    }

    final Object value;
  }

  static class Unary extends Expr {
    Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpr(this);
    }

    final Token operator;
    final Expr right;
  }

  static class Logical extends Expr {
    Logical(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLogicalExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }

  static class Binary extends Expr {
    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }

  static class Variable extends Expr {
    Variable(Token name) {
      this.name = name;
    }

      @Override
    <R> R accept(Visitor<R> visitor) {
        return visitor.visitVariableExpr(this);
      }

      final Token name;
    }

  abstract <R> R accept(Visitor<R> visitor);
}