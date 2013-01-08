
;(defmulti pretty class)
;
;(defmethod pretty :default [x]
;  [:text (str x)])
;
;TODO use :linear newlines
;(defmethod pretty clojure.lang.IPersistentVector [v]
;  [:span "[" [:nest 1 (map pretty v) ] "]"])
