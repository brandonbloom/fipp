(ns fipp.macros)

(defmacro defprinter [name document-fn defaults]
  `(defn ~name
     ([~'document] (~name ~'document ~defaults))
     ([~'document ~'options]
       (fipp.printer/pprint-document (~document-fn ~'document) ~'options))))
