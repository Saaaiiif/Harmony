package services.ForumServices;

public class SpellError {

    private int offset;
    private int length;
    private String suggestion;

    public SpellError(int offset, int length, String suggestion) {
        this.offset = offset;
        this.length = length;
        this.suggestion = suggestion;
    }

    public int getOffset() { return offset; }
    public int getLength() { return length; }
    public String getSuggestion() { return suggestion; }
}