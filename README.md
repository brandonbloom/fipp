# Fast Idiomatic Pretty-Printer

Fipp is a better pretty printer for Clojure.

Like clojure.pprint, this pretty printer has a *linear runtime* and uses
*bounded space*. However, unlike clojure.pprint, Fipp's implementation is
functionally pure and its interface is powered by data, not method calls.

The data interface is agnostic to the source language. An EDN printer is
included with Fipp, but it is easy to create a pretty printer for your
own language or documents.


## Usage

```clojure
;; Refer with a rename to avoid collision with your REPL's pprint.
(require '[fipp.edn :refer (pprint) :rename {pprint fipp}])

(fipp [1 2 3])
(fipp (range 50))
(fipp (range 20))
(fipp (range 20) {:width 10})
```

Currently, `:width` is the only option and defaults to 70.


## Fast!

In my non-scientific testing, it has proven to be at least five times as fast
as `clojure.pprint`.  It also has the nice property of printing no later than
having consumed the bound amount of memory, so you see your first few lines of
output instantaneously.

The core algorithm is described by Swierstra and Chitil in
[Linear, Bounded, Functional Pretty-Printing](http://kar.kent.ac.uk/24041/1/LinearOlaf.pdf).

Swierstra and Chitil's implementation uses lazy evaluation and requires
[tying the knot](http://www.haskell.org/haskellwiki/Tying_the_Knot) to
interleave the measuring and printing phases to achieve the bounded space goal.

However, this implementation is instead a port of the strict evaluation
strategy as described by Kiselyov, Peyton-Jones, and Sabry in
[Lazy v. Yield: Incremental, Linear Pretty-printing](http://www.cs.indiana.edu/~sabry/papers/yield-pp.pdf).

Clojure's Reducers framework is used to simulate generators and their `yield`
operator. Unlike lazy reduction, reducers interleave execution of multi-phase
reductions by composition of reducer functions. This enables preservation of
the bounded-space requirement and eases reasoning about the program's behavior.


## Idiomatic!

Clojure's included pretty printer supports pluggable dispatch tables and
provides an API for controlling the printing process. The programming model
is side-effectual. For example, to print a breaking newline, you execute
`(pprint-newline :linear)`. This means that it's a difficult and tricky
process to write or compose new pretty printers.

Fipp, on the other hand, accepts a "pretty print document" as input. This
document is similar to HTML markup using [hiccup](https://github.com/weavejester/hiccup).

Here are some examples:

```clojure
(require '[fipp.printer :refer (pprint-document)])

(defn ppd [doc]
  (pprint-document doc {:width 10}))

(ppd [:span "One" :line "Two" :line "Three"])

(ppd [:group "(do" [:nest 2 :line "(step-1)" :line "(step-2)"] ")"])
```

There is a small set of primitive document elements, but the set of keywords
is extensible via expansion.  See `doc/extensibility.md` for details.


## Installation

Fipp artifacts are [published on Clojars](https://clojars.org/fipp).

To depend on this version with Lein, add the following to your `project.clj`:

```clojure
[fipp "0.4.0"]
```

Please note that Fipp requires Clojure 1.5.1 for access to the latest Reducers bits.

### nREPL Integration

If you want Fipp at your fingertips for all of your lein projects,
you can merge the following into your `~/.lein/profile.clj`

```clojure
{:user {:repl-options {:custom-eval (require '[fipp.edn :refer (pprint) :rename {pprint fipp}])}
        :dependencies [[fipp "0.5.0"]]}}
```


## TODO

#### No Brainers

- Tests!! I've only really tried this on a limited number of simple forms.
- Clojure code-dispatch (right now we're only EDN)
- Limit print depth
- Limit lines printed
- Limit list lengths
- Cycle detection

#### Crazy Ideas

- Macro to compile fast, non-pretty printers for toString impls.
- Document stylesheets (think CSS/XSLT/DSSSL) for customizing printing


## License

Copyright © 2013 Brandon Bloom

Distributed under the Eclipse Public License, the same as Clojure.
