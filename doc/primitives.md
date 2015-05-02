# Pretty Document Primitives

Fipp's pretty printer engine accepts a "pretty document" which is a tree of
these primitive nodes. Each node is a vector tagged with a keyword as its
first-element. Common patterns have shorthand representations.  The behavior
of and shorthands for each primitive are described below.

See the `serialize` function in `src/fipp/engine.clj` for the full details of
how input documents are expanded and interpretered.

See `src/fipp/edn.clj` for example usage.


## [:text "..."]

Text nodes are measured and printed verbatim.

Typically you don't use `:text` nodes explicitly, since strings are
automatically expanded to `[:text "..."]`.

You should not include `\n` characters in text.


## [:escaped "..."]

Escaped nodes are printed verbatim, but measure as one character.

Useful for cases where characters need to be escaped in the printed
output, but will eventually be rendered to occupy only one column.


## [:pass "..."]

Passthrough nodes are printed verbaitm, but *not* measured.

Use Passthrough nodes for zero-width control sequences, such as ANSI escape
sequences for colors.


## [:span children...]

Span nodes concatenate zero or more children nodes. Lazy sequences are
treated the same as spans, so you can often omit them when concatenating
document fragments with standard Clojure sequence functions.


## [:line "inline"]

Line nodes print as either a line break or their inline string value. See
`:group` nodes for how to determine which.

The inline argument is optional. If omitted, a single space will be used.

A `:line` keyword by itself is shorthand for `[:line " "]`.


## [:group children...]

Group nodes indicate a subdocument which should be laid out either
horizontally or vertically. Horizontal means that all `:line` nodes in the
group will be printed as their `inline` arguments. Vertical means that all
`:line` nodes in the group will be printed as line breaks and subsequent
lines will be indented to the appropriate tab stop.

Fipp attempts to print groups horizontally whenever possible. If a group is
measured to be past the right margin, it will be printed vertically. Groups
can also be printed vertically if they are nested more deeply than the width.
Inner groups are always printed vertically before outer groups.

Children are concatenated as if by `:span`.


## [:nest indent children...]

Nest nodes indicate a subdocument wherein vertically laid out `:line` nodes
should produce an increased indent. A new "tab stop" is pushed on to a stack
relative to the most recent tab stop.

The `indent` argument is optional and defaults to 2.

Children are concatenated as if by `:span`.

Nest nodes do not introduce a new layout group.


## [:align offset children...]

Alignment nodes indicate a subdocument wherein vertically laid out `:line`
nodes should produce a matching indent. A new "tab stop" is pushed on to a
stack relative to the most recently printed column.

The offset argument is optional and defaults to 0.

Children are concatenated as if by `:span`.

Alignment nodes do not introduce a new layout group.


## [:break]

Break nodes force a newline. The tab stop stack is unchanged.

The vector wrapper around the `:break` keyword is optional.
