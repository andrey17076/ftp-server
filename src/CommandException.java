class CommandException extends Exception {
    private int code;
    private String text;

    CommandException(int code, String text) {
        super(code + " " + text);
        this.code = code;
        this.text = text;
    }

    int getCode() {
        return code;
    }

    String getText() {
        return text;
    }

    public String getMessage() {
        return code + " " + text;
    }
}
