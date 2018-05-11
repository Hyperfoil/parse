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
     * The children Exp will be applied to the characters that precede the match region this Exp
     */
    ChildrenLookBehind,
    /**
     * Push the current target (grouping / extending) to the target stack
     */
    PushTarget,
    /**
     * Remove the current target from the stack (if it isn't root) before populating the match
     */
    PrePopTarget,
    /**
     * Remove the current target from the stack (if it isn't root) after populating the match
     */
    PostPopTarget,
    /**
     * Clear all the targets back to the root before populating the match
     */
    PreClearTarget,
    /**
     * Clear all the targets from the stack (go back to root) after populating the match
     */
    PostClearTarget,
    /**
     * Populate the matches on the Root target only.
     * TODO If the Exp has a group or key then grouping starts from the Root target.
     */
    OnRootTarget

}

