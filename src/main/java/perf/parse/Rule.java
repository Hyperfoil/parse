package perf.parse;

/**
 * Rules for how an Exp will match to input and where it will place the json entries
 */
public enum Rule {
    /**
     * The Exp will start matching from the beginning of the line. The default behaviour starts matching from where the previous Exp finished
     */
    LineStart,
    /**
     * The Exp will match as many substrings as possible
     */
    Repeat,
    /**
     * The Exp, once matched, will loop over all children as long as one matched each loop
     */
    RepeatChildren,
    /**
     * Push the current target (grouping / extending) to the target stack
     */
    PushTarget,
    /**
     *
     */
    AvoidTarget,
    /**
     * Remove the current target from the stack (if it isn't root)
     */
    PopTarget,
    /**
     *
     */
    ClearTarget
}

