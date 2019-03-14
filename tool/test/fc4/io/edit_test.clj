(ns fc4.io.edit-test
  (:require [clojure.java.io :refer [copy delete-file file writer]]
            [clojure.string :refer [split-lines]]
            [clojure.test :refer [deftest is testing]]
            [fc4.files :refer [get-extension remove-extension set-extension]]
            [fc4.io.edit :as e])
  (:import [java.io ByteArrayOutputStream File OutputStreamWriter PrintStream]))

(defn count-substring
  {:source "https://rosettacode.org/wiki/Count_occurrences_of_a_substring#Clojure"}
  [txt sub]
  (count (re-seq (re-pattern sub) txt)))

(defn append
  [f v]
  (with-open [w (writer f :append true)]
    (.write w v)))

(defmacro with-system-out-str
  {:source "https://gist.github.com/jeremyheiler/66ba8ff62650fb0de700"}
  [& body]
  `(let [out# System/out
         buf# (ByteArrayOutputStream.)
         prs# (PrintStream. buf#)
         wtr# (OutputStreamWriter. prs#)]
     (try
       (System/setOut prs#)
       (binding [*out* wtr#]
         ~@body)
       (finally
         (.flush wtr#)
         (.flush prs#)
         (System/setOut out#)))
     (.toString buf#)))

(defn tmp-copy
  "Creates a new tempfile with a very similar name to the input file/path
  and the same contents as the file/path. Returns a File object pointing to
  the tempfile."
  [source-file]
  (let [source (file source-file) ; just in case the source was a string
        base-name (remove-extension source)
        suffix (str "." (get-extension source))
        dir (.getParentFile source)
        tmp-file (File/createTempFile base-name suffix dir)]
    (copy source tmp-file)
    tmp-file))

;; It’s common for tests to be broader than these, and to use `testing` to test
;; various scenarios. These tests are smaller and more specific, using basically
;; one deftest per scenario, because they are very slow, and these test
;; functions are our units of concurrency. By breaking them into multiple
;; top-level deftests, they can run in parallel.

(deftest edit-workflow-single-file-single-change
  (testing "changing a single file once"
    (let [yaml-source "test/data/structurizr/express/diagram_valid_cleaned.yaml"
          yaml-file (tmp-copy yaml-source)
          png-file (file (set-extension yaml-file "png"))
          yaml-file-size-before (.length yaml-file)
          _ (is (or (not (.exists png-file))
                    (.delete png-file)))
          stdout (with-system-out-str
                   (e/start (str yaml-file))
                   (Thread/sleep 1000)
                   (append yaml-file "\n")
                   (Thread/sleep 10000)
                   (e/stop))]
      (is (.exists png-file))
      (is (= (.length yaml-file) yaml-file-size-before))
      (is (> (.length png-file) 50000))
      (is (= (count-substring stdout "✅") 1))
      (is (= (count (split-lines stdout)) 2))
      (delete-file yaml-file)
      (delete-file png-file))))

(deftest edit-workflow-two-files-changed-simultaneously
  (testing "changing two files simultaneously"
    (let [yaml-source "test/data/structurizr/express/diagram_valid_cleaned.yaml"
          yaml-files (repeatedly 2 #(tmp-copy yaml-source))
          png-files (map #(file (set-extension % "png")) yaml-files)
          yaml-file-size-before (.length (first yaml-files)) ; they’re identical
          _ (doseq [png-file png-files]
              (is (or (not (.exists png-file))
                      (.delete png-file))))
          stdout (with-system-out-str
                   (apply e/start yaml-files)
                   (Thread/sleep 1000)
                   (run! #(append % "\n") yaml-files)
                   (Thread/sleep 12000)
                   (e/stop))]
      (is (= (count-substring stdout "✅") 2))
      (is (= (count (split-lines stdout)) 3))
      (doseq [png-file png-files]
        (is (.exists png-file))
        (is (> (.length png-file) 50000))
        (delete-file png-file))
      (doseq [yaml-file yaml-files]
        (is (= (.length yaml-file) yaml-file-size-before))
        (delete-file yaml-file)))))
