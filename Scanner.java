import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and",    TokenType.AND);
        keywords.put("class",  TokenType.CLASS);
        keywords.put("else",   TokenType.ELSE);
        keywords.put("false",  TokenType.FALSE);
        keywords.put("for",    TokenType.FOR);
        keywords.put("fun",    TokenType.FUN);
        keywords.put("if",     TokenType.IF);
        keywords.put("nil",    TokenType.NIL);
        keywords.put("or",     TokenType.OR);
        keywords.put("print",  TokenType.PRINT);
        keywords.put("return", TokenType.RETURN);
        keywords.put("super",  TokenType.SUPER);
        keywords.put("this",   TokenType.THIS);
        keywords.put("true",   TokenType.TRUE);
        keywords.put("var",    TokenType.VAR);
        keywords.put("while",  TokenType.WHILE);
        keywords.put("do", TokenType.DO);
    }

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '{': addToken(TokenType.LEFT_BRACE); break;
            case '}': addToken(TokenType.RIGHT_BRACE); break;
            case ',': addToken(TokenType.COMMA); break;
            case '.':
                if (match('+')) {addToken(TokenType.DOT_PLUS);}
                else {addToken(TokenType.DOT);}
                break;
            case ':':
                if (match('?')) addToken(TokenType.TERNARY);
                else addToken(TokenType.TERNARY_ELSE);
                break;
            case '-':
                if (match('-')) { addToken(TokenType.MINUS_MINUS);}
                else if (match('=')) {addToken(TokenType.MINUS_EQUAL);}
                else {addToken(TokenType.MINUS);}
                break;
            case '+':
                if (match('+')) { addToken(TokenType.PLUS_PLUS);}
                else if (match('=')) {addToken(TokenType.PLUS_EQUAL);}
                else {addToken(TokenType.PLUS);}
                break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case '*':
                if (match('*')) {addToken(TokenType.STAR_STAR);}
                else if (match('=')) {addToken(TokenType.STAR_EQUAL);}
                else {addToken(TokenType.STAR);}
                break;
            case '%': addToken(TokenType.MODULUS); break;
            case '!': addToken(match('=') ? TokenType.BANG_EQUAL :TokenType.BANG); break;
            case '=': addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL); break;
            case '<': addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS); break;
            case '>': addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER); break;
            case '/':
                if (match('/')) {
                    // A comment goes until the end of the line.
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {
                    multiLineComment();
                } else if (match('=')) {
                    addToken(TokenType.SLASH_EQUAL);
                } else {
                    addToken(TokenType.SLASH);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace.
                break;
            case '\n':
                line++;
                break;
            case '"': string(); break;
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, "Are kidding me? What is this character? Character: '" + c + "'.");
                }
                break;
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        // See if the identifier is a reserved word.
        String text = source.substring(start, current);

        TokenType type = keywords.get(text);
        if (type == null) type = TokenType.IDENTIFIER;
        addToken(type);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void number() {
        while (isDigit(peek())) advance();

        // Look for a fractional part.
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "."
            advance();

            while (isDigit(peek())) advance();
        }

        addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        // Unterminated string.
        if (isAtEnd()) {
            Lox.error(line, "Are you kidding me? Why would you not terminate this string.");
            return;
        }

        // The closing ".
        advance();

        // Trim the surrounding quotes.
        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    private void multiLineComment() {
        // Skip the *
        char c = advance();

        // While we still have at least 2 more chars in front of us
        while (current + 1 <= source.length()) {
            // If the next char is a new line, increment the line counter
            if (c == '\n') line++;

            // If we found the end of the comment
            if (peek() == '*' && peekNext() == '/') {
                // Advance two times for '*' and '/'
                advance();
                advance();

                // Exit the function
                return;
            }
            // Advance to the next char
            c = advance();
        }

        // If we arrived here it means that we are at the end of the program and the comment isn't terminated
        Lox.error(line,"Are you that lazy that you can't even terminate a multi line comment");
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        current++;
        return source.charAt(current - 1);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
} 