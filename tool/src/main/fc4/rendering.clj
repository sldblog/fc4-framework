(ns fc4.rendering
  (:require [clojure.spec.alpha :as s]
            [cognitect.anomalies :as anom]))

(s/def ::png-bytes (s/and bytes? #(not (zero? (count %)))))
(s/def ::success-result (s/keys :req [::png-bytes]))
(s/def ::failure-result ::anom/anomaly)

(defprotocol Renderer
  "A potentially resource-intensive abstraction that can render Structurizr
  Express diagrams. Implementations must also implement java.io.Closeable."
  (render [renderer diagram-yaml]
    "diagram-yaml must be a string containing the YAML source of a Structurizr
    Express diagram. Implementations may or may not support concurrent calls to
    render; see their docs. Blocks. Returns either ::success-result or
    ::failure-result."))
