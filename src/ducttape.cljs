(ns ducttape)

(defn delegate [el binds]
  (doseq [[k f arg] binds]
    (let [[_ dom-event-name selector] (re-find #"^(\S+)\s*(.*)$" k)]
      (if (= selector "")
        (.bind (js/jQuery el)
               dom-event-name
               (fn [e] (f (assoc arg :e e))))
        (.delegate (js/jQuery el)
                   selector dom-event-name
                   (fn [e] (f (assoc arg :e e))))))))
