# Extensibility

## Primitives

TODO: More docs here! Until then, compare to Swierstra and Chitil.

See also the `serialize` function in `src/bbloom/fipp/printer.clj`

The primitives are as follows:

### :text

Just like Swierstra and Chitil.

Strings expand to [:text ...] nodes.

### :span

A variadic version of binary document composition.

Spans and seqs (not vectors) are equivalent.

### :line

Just like Swierstra and Chitil.
Supports alternative "inline" text besides a single space.

`:line` is shorthand for `[:line " "]`.

### :group

Just like Swierstra and Chitil.

### :nest

Just like Swierstra and Chitil.

Accepts an integer indentation argument:

`[:nest 2 ...]`

### :align (not-yet implemented)

Just like Swierstra and Chitil, but see their code, not their paper.


## Custom Dispatch

Write a `pretty` function and use `defprinter`.

TODO: More docs here!  Until then, see `src/bbloom/fipp/edn.clj`.

## Custom Document Nodes

```clojure
(defmethod bbloom.fipp.printer/serialize-node ...)
```

TODO: More docs here!
