#!/bin/bash

set -e

lein test

java -cp $(lein classpath) clojure.main build_cljs.clj

node ./out/main.js
