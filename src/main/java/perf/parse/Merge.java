package perf.parse;

/**
 * Represent the various actions to take when a perf.parse.Exp matches against the input.
 * !! Merge actions are exclusive !!
 */
public enum Merge {
    /**
     * Treat each match Name as a collection of all the values that matched
     */
    Collection,

    /**
     * Treat each match as the start of a new entry under the parent context.
     * If there is not a defined grouping then each named capture group will be treated as a list for appending new matches
     */
    Entry,
    /**
     * Treat each match of the perf.parse.Exp as an extension of the last entry in the grouping.
     * If there is not a defined grouping then this Merge rule is ignored.
     */
    Extend,
    /**
     * Treat each match as the start of a new context
     */
    PreClose,
    /**
     * Treat each match as the end of a new context (after populating match keys)
     */
    PostClose,

    }
