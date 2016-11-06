# Fast Idiomatic Pretty-Printer

[![Join the chat at https://gitter.im/brandonbloom/fipp](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/brandonbloom/fipp?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Fipp is a better pretty printer for Clojure and ClojureScript.

Like clojure.pprint, this pretty printer has a *linear runtime* and uses
*bounded space*. However, unlike clojure.pprint, Fipp's implementation is
tuned for great performance and has a functional, data-driven API.

The data interface is agnostic to the source language. Printers are included
for Edn data and Clojure code, but it is easy to create a pretty printer for
your own language or documents: Even if they're not made out of Clojure data!

Fipp is great for printing large data files and debugging macros, but it is
not suitable as a code reformatting tool. ([explanation][4])


## Installation

Fipp artifacts are [published on Clojars](https://clojars.org/fipp).

To depend on this version with Lein, add the following to your `project.clj`:

```clojure
[fipp "0.6.7"]
```

This version of Fipp works with Clojure 1.7 or newer.

See [the v0.5 branch](https://github.com/brandonbloom/fipp/tree/v0.5) for
a version of Fipp that works with Clojure 1.6.

ClojureScript is supported from build 3269 and up. There are several known
issues on both the Fipp and CLJS sides that are in the process of being fixed.
While I wouldn't yet trust Fipp to serialize CLJS data durably, it should be
more than usable interactively. Please let me know if it isn't for you.


### Colorization & REPL Integration

[Puget][2] uses Fipp's engine to provide an alternative, colorizing printer.

[Whidbey][3] integrates Puget in to nREPL via Leinigen, so that every
evaluation pretty prints in color.


## Printer Usage

```clojure
;; Refer with a rename to avoid collision with your REPL's pprint.
(require '[fipp.edn :refer (pprint) :rename {pprint fipp}])

(fipp [1 2 3])
(fipp (range 50))
(fipp (range 20))
(fipp (range 20) {:width 10})

(require '[fipp.clojure])
(fipp.clojure/pprint '(let [foo "abc 123"
                            bar {:x 1 :y 2 :z 3}]
                        (do-stuff foo (assoc bar :w 4)))
                    {:width 40})
```

The available options are:

- `:width` defaults to `70`.
- `:print-length` behaves as and defaults to `clojure.core/*print-length*`.
- `:print-level` behaves as and defaults to `clojure.core/*print-level*`.
- `:print-meta` behaves as and defaults to `clojure.core/*print-meta*`.

Any other supported/hidden options are subject to change.


## Fast!

In my non-scientific testing, it has proven to be at least five times as fast
as `clojure.pprint`.  It also has the nice property of printing no later than
having consumed the bounded amount of memory, so you see your first few lines
of output instantaneously.

The core algorithm is described by Swierstra and Chitil in
[Linear, Bounded, Functional Pretty-Printing][5].

Swierstra and Chitil's implementation uses lazy evaluation and requires
[tying the knot](http://www.haskell.org/haskellwiki/Tying_the_Knot) to
interleave the measuring and printing phases to achieve the bounded space goal.

However, this implementation is instead a port of the strict evaluation
strategy as described by Kiselyov, Peyton-Jones, and Sabry in
[Lazy v. Yield: Incremental, Linear Pretty-printing][6].

Clojure's transducers are used to simulate generators and their `yield`
operator. Unlike lazy reduction, transducers interleave execution of
multi-phase transformations by function composition. This enables preservation
of the bounded-space requirement and eases reasoning about the program's
behavior. Additionally, it avoids a lot of intermediate object allocation.


## Idiomatic!

Clojure's included pretty printer supports pluggable dispatch tables and
provides an API for controlling the printing process. The programming model
is side-effectual. For example, to print a breaking newline, you execute
`(pprint-newline :linear)`. This means that it's a difficult and tricky
process to write or compose new pretty printers.

Fipp, on the other hand, accepts a "pretty print document" as input. This
document is similar to HTML markup using [hiccup][7].

Here are some examples:

```clojure
(require '[fipp.engine :refer (pprint-document)])

(defn ppd [doc]
  (pprint-document doc {:width 10}))

(ppd [:span "One" :line "Two" :line "Three"])

(ppd [:group "(do" [:nest 2 :line "(step-1)" :line "(step-2)"] ")"])
```

If you want to write your own printer, see
[doc/primitives.md](doc/primitives.md) for details.


## License

Copyright © 2015 Brandon Bloom

Distributed under the Eclipse Public License, the same as Clojure.


## Acknowledgements

Fipp is fast in part thanks to [YourKit's Java Profiler][1].


[1]: http://www.yourkit.com/java/profiler/index.jsp
[2]: https://github.com/greglook/puget
[3]: https://github.com/greglook/whidbey
[4]: https://github.com/brandonbloom/fipp/issues/21#issuecomment-64693415
[5]: http://kar.kent.ac.uk/24041/1/LinearOlaf.pdf
[6]: http://www.cs.indiana.edu/~sabry/papers/yield-pp.pdf
[7]: https://github.com/weavejester/hiccup
