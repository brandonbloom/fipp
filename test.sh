#!/bin/bash

set -e

lein test

java -cp $(lein classpath) clojure.main build_cljs.clj

node --stack-size=1024 ./out/main.js
