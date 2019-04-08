package perf.parse;

/**
 * Rules for how an ExpOld will match to input and where it will place the json entries
 */
public enum Rule {
    /**
     * The ExpOld will start matching from the beginning of the line. The default behaviour starts matching from where the previous ExpOld finished
     */
    LineStart,
    /**
     * The ExpOld will match as many substrings as possible
     */
    Repeat,
    /**
     * The ExpOld, once matched, will loop over all children as long as one matched each loop
     */
    RepeatChildren,
    /**
     * The children ExpOld will be applied to the characters that precede the match region this ExpOld
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
     * Remove the current or named target from the stack (if it isn't root) after populating the match
     */
    PostPopTarget,
    /**
     * Clear all the targets back to the root or up to and including the named target before populating the match
     */
    PreClearTarget,
    /**
     * Clear all the targets from the stack (go back to root) or up to and include the named target after populating the match
     */
    PostClearTarget,
    /**
     * Populate the matches on/from the Root target.
     *
     */
    TargetRoot


}

