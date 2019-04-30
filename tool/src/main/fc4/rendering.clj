(ns fc4.rendering
  (:require [clojure.spec.alpha :as s]
            [cognitect.anomalies :as anom]))

(s/def ::png-bytes (s/and bytes? #(not (zero? (count %)))))
(s/def ::error (s/keys :req [::anom/message]))
(s/def ::errors (s/coll-of ::error))
(s/def ::success-result (s/keys :req [::png-bytes]))
(s/def ::failure-result (s/merge ::anom/anomaly (s/keys :req [::errors])))

(defprotocol Renderer
  "A potentially resource-intensive abstraction that can render Structurizr
  Express diagrams. Implementations must also implement java.io.Closeable."
  (start [renderer]
    "Must be called before render. Users are recommended to call this in a with-open block. Returns
    renderer so that the renderer can be created and started in a single expression in a with-open
    block.")
  (render [renderer diagram-yaml]
    "Start must be called first. diagram-yaml must be a string containing the YAML source of a
    Structurizr Express diagram. Currently no implementations support concurrent calls to render.
    Blocks. Returns either ::success-result or ::failure-result.")
  (stop [renderer]
    "Must be called to avoid leaking substantial resources. Returns nil. Some implementations may
    support calling start to restart the renderer after stop has been called. Users are recommended
    to use with-open rather than calling this method directly."))
