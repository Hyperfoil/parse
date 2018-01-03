package perf.parse;

/**
 * Created by wreicher
 */
public enum Eat {
    /**
     * Eat a fixed with of the input each time the perf.parse.Exp matches
     */
    Width(1),
    /**
     * Do not consume any of the input line (Default behavior)
     */
    None(0),
    /**
     * Consume the matched part of the line (including non-captured sections)
     */
    Match(-1),
    /**
     * If the perf.parse.Exp matches any part of the line then consume the entire line
     * preventing other perf.parse.Exp from matching
     */
    Line(-2);
    private int id;

    private Eat(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static Eat from(int value) {
        switch (value) {
            case -2:
                return Line;
            case -1:
                return Match;
            case 0:
                return None;
            default:
                return Width;
        }
    }
}
