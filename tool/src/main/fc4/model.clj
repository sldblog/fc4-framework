(ns fc4.model
  (:require [clj-yaml.core           :as yaml]
            [clojure.set                       :refer [union]]
            [clojure.spec.alpha      :as s]
            [clojure.spec.gen.alpha  :as gen]
            [clojure.string                    :refer [includes? split]]
            [fc4.files                        :refer [relativize]]
            [fc4.spec               :as fs]
            [fc4.util               :as fu]))

(load "model_specs")

(defn- get-tags-from-path
  "Given a path to a file (as a String) and a path to an ancestor root directory
  (as a String), extracts a set of tags from set of directories that are
  descendants of the ancestor root dir. If the file path includes “external”
  then the tag :external will be added to the returned set; if not then the tag
  :internal will be added.

  For example:
  => (get-tags-from-path
       \"/docs/fc4/model/systems/uk/compliance/panopticon.yaml\"
       \"/docs/fc4/model/systems/\")
  #{:uk :compliance :internal}"
  [file-path relative-root]
  (as-> (or (relativize file-path relative-root)
            (str file-path)) v
    (split v #"/")
    (map keyword v)
    (drop-last v)
    (set v)
    (conj v (if (includes? file-path "external")
              :external
              :internal))))

;; TODO: for this spec to be truly useful in QA terms, it really needs an fspec
;; and better generators (the generators will need to create two paths that are
;; usefully and realistic related).
(s/fdef get-tags-from-path
  :args (s/cat :file-path     ::fs/file-path
               :relative-root ::fs/dir-path)
  :ret  ::tags)

(defn- to-set-of-keywords
  [xs]
  (-> (map keyword xs) set))

(s/fdef to-set-of-keywords
  :args (s/cat :xs (s/coll-of string?))
  :ret  (s/coll-of keyword? :kind set?)
  :fn (fn [{{:keys [xs]} :args, ret :ret}]
        (= (count (distinct xs)) (count ret))))

;; An element just after it’s parsed from the YAML, before any fixup.
;; TODO: this file uses a mix of “element” and “entity” to refer to pretty much
;; the same thing. Choose one and stick with it!
(s/def ::proto-entity
  (s/with-gen
    (s/map-of ::fs/unqualified-keyword (s/or :name    ::name
                                             :strings ::simple-strings))
    #(gen/hash-map :name  (s/gen ::name)
                   :repos (s/gen ::simple-strings)
                   :tags  (s/gen ::simple-strings))))

; We have to capture this at compile time in order for it to have the value we
; want it to; if we referred to *ns* in the body of a function then, because it
; is dynamically bound, it would return the namespace at the top of the stack,
; the “currently active namespace” rather than what we want, which is the
; namespace of this file, because that’s the namespace all our keywords are
; qualified with.
(def ^:private this-ns-name (str *ns*))

(defn- fixup-container
  [container sys-name]
  (-> (update container :tags to-set-of-keywords)
      (update :repos to-set-of-keywords)
      (update :tags to-set-of-keywords)
      ;; Container references in the YAML files don’t have to specify the target
      ;; system; if ommitted then the target system is implicitly the same
      ;; system; the container in that case is targeting a sibling container.
      ;; (This is optional in the YAML files to make the files easier to read
      ;; and write (by humans, manually).) In our in-memory data structure,
      ;; however, the target system must be specified, for uniformity. So we
      ;; just add it in right here.
      (update :uses (fn [sys-refs]
                      (into #{}
                            (map #(if (:system %)
                                    %
                                    (assoc % :system sys-name))
                                 sys-refs))))
      (fu/qualify-keys this-ns-name)))

(s/fdef fixup-container
  :args (s/cat :container ::proto-entity
               :sys-name  ::name)
  :ret  ::container-map
  :fn   (fn [{{in :container} :args, out :ret}]
          (= (count (::uses in)) (count (::uses out)))))

(defn- fixup-element
  [entity-type tags-from-path {:keys [name] :as elem}]
  (-> elem
      (assoc ::type entity-type)
      (update :repos to-set-of-keywords)
      (update :tags to-set-of-keywords)
      (update :tags (partial union tags-from-path))
      (update :uses set)
      (update :containers #(into #{}
                                 (map (fn [container]
                                        (fixup-container container name))
                                      %)))
      (fu/qualify-keys this-ns-name)))

(s/fdef fixup-element
  :args (s/cat :entity-type    ::entity-type
               :tags-from-path ::tags
               :proto-entity   ::proto-entity)
  :ret  ::element
  :fn   (fn [{{elem-in :proto-entity} :args, elem-out :ret}]
          (every? #(>= (count (get elem-out %)) (count (get elem-in %)))
                  [::repos ::tags ::uses ::containers])))

;; A file might contain a single element (as a map), or an array containing
;; multiple elements.
(defn elements-from-file
  "Parses the contents of a YAML file, then processes those contents such that
  each element conforms to ::element. entity-type is needed because the files
  on disk don’t include the `type` key — it’s implicit in the file’s path. The
  file-path and root-path are used to generate tags from the file’s path
  relative to the root path."
  [file-contents entity-type file-path root-path]
  (let [parsed (yaml/parse-string file-contents)
        elems (if (associative? parsed) [parsed] parsed)
        tags-from-path (get-tags-from-path file-path root-path)]
    (map (partial fixup-element entity-type tags-from-path)
         elems)))

(s/fdef elements-from-file
  :args (s/cat :file-contents ::yaml-file-contents
               :entity-type   ::entity-type
               :file-path     ::fs/file-path
               :root-path     ::fs/dir-path)
  :ret  (s/coll-of ::element))
