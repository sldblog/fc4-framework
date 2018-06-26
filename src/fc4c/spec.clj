(ns fc4c.spec
  "For specs that are used in multiple namespaces."
  (:require [clojure.java.io :as io] ; used to generate File "values" not do IO
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :refer [blank? ends-with? includes? join]]
            [com.gfredericks.test.chuck.generators :as gen']))

(s/def ::non-blank-str (s/and string? (complement blank?)))
(s/def ::no-linebreaks  (s/and string? #(not (includes? % "\n"))))
(s/def ::non-blank-simple-str (s/and ::non-blank-str ::no-linebreaks))

(defn- str-gen
  [min-length max-length]
  ;; Technique found here: https://stackoverflow.com/a/35974064/7012
  (gen/fmap (partial apply str)
            (gen/vector (gen/char-alphanumeric) min-length max-length)))

(s/def ::short-non-blank-simple-str
  (let [min 1 max 50] ;; inclusive
    (s/with-gen
      (s/and ::non-blank-simple-str
             #(<= min (count %) max))
      #(str-gen min max))))

(s/def ::unqualified-keyword
  (s/with-gen
    (s/and keyword? (complement qualified-keyword?))
    #(gen/fmap keyword (s/gen ::non-blank-simple-str))))

(def coord-pattern-base "(\\d{1,4}), ?(\\d{1,4})")

(s/def ::coord-string
  (s/with-gen string?
    #(gen'/string-from-regex (re-pattern coord-pattern-base))))

(s/def ::file-path-str
  (s/with-gen
    (s/and ::non-blank-simple-str #(includes? % "/"))
    #(gen/fmap
      (fn [s] (str (->> (repeat 5 s) (join "/"))))
      (s/gen ::short-non-blank-simple-str))))

(s/def ::dir-path-str
  (s/with-gen
    (s/and ::file-path-str #(ends-with? % "/"))
    #(gen/fmap
      (fn [file-path] (str file-path "/"))
      (s/gen ::file-path-str))))

(s/def ::dir-path-file
  (s/with-gen
    (partial instance? java.io.File)
    #(gen/fmap io/file (s/gen ::dir-path-str))))

(s/def ::dir-path
  (s/or ::dir-path-str ::dir-path-file))
