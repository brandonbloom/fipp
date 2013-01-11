# fipp

A Clojure library designed to ... well, that part is up to you.

## Usage

```clojure
;; Refer with a rename to avoid your REPL's pprint.
(require '[bbloom.fipp.edn :refer (pprint) :rename {pprint fipp}])

(fipp [1 2 3])
(fipp (range 50))
(fipp (range 20))
(fipp (range 20) {:width 10})

Currently, `:width` is the only option and defaults to 70.
```

## Implementation Notes

lazy v yield
reducers == yield (sorta)
bounded space & linear runtime! (hence the fast part)

nest & align operators

## Extensiblity

Data!! (hence the idiomatic part)

## TODO

- in progress: nest operator (relative indentation)
- limit nesting depth
- limit lines printed
- limit list lengths
- cycle detection
- align operator (absolute indentation)

## License

Copyright © 2013 Brandon Bloom

Distributed under the Eclipse Public License, the same as Clojure.
