(require 'cljs.build.api)

(def srcs (cljs.build.api/inputs "src" "test"))

(cljs.build.api/build srcs
  {:main 'fipp.cljs-tests
   :output-to "out/main.js"
   :output-dir "out"
   :target :nodejs
   :verbose true})
