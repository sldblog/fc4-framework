(ns fc4.io.edit
  "CLI workflow that watches Structurizr Express diagram files; when changes
  are observed to a YAML file, the YAML in the file is cleaned up and rendered
  to an image file."
  (:require [fc4.io.render :refer [render-diagram-file]]
            [fc4.io.util :refer [beep print-now]]
            [fc4.io.yaml :refer [process-diagram-file yaml-file?]]
            [hawk.core :as hawk])
  (:import [java.io File OutputStreamWriter]
           [java.time LocalTime]
           [java.time.temporal ChronoUnit]
           [java.util.concurrent Executors ExecutorService]))

;; Mutable state 😱😱😱
(def executor
  "Contains the ExecutorService used to process files in the background. An atom
  is used because during development and testing we need to be able to stop and
  then “restart” the ExecutorService. Because the ExecutorService implementation
  we’re using (ThreadPoolExecutor) can’t actually be restarted, we “restart” it
  by replacing it with a new one."
  (atom nil))

(def active-set
  "The set of files that are being processed or are enqueued to be processed.
  This is used to discard subsequent file modification events that occur while a
  file is being processed or is enqueued to be processed — this is crucial
  because there’s always _at least_ one subsequent modification event, because
  the files are modified by process-file 😵!"
  (atom #{}))

(def current-watch
  "Useful during development and testing."
  (atom nil))

(defn secs-since
  [inst]
  (.between (ChronoUnit/SECONDS) inst (LocalTime/now)))

(defn process-fs-event?
  [_context {:keys [kind file] :as _event}]
  (and (#{:create :modify} kind)
       (yaml-file? file)
       (not (contains? @active-set file))))

(def event-kind->past-tense
  {:create "created"
   :modify "modified"
   :delete "deleted"})

(def ^:private secs-threshold-to-print-event-time 10)

(defn ^:private remove-nanos
  [instant]
  (.withNano instant 0))

(defn process-file
  [event-ts event-kind ^File file]
  (let [elapsed-secs (secs-since event-ts)]
    (print-now (remove-nanos (LocalTime/now)) " "
               (.getName file) " "
               (event-kind->past-tense event-kind)
               (when (> elapsed-secs secs-threshold-to-print-event-time)
                 (str " at " (remove-nanos event-ts)))
               ";"))
  (try
    (print-now " formatting...")
    (process-diagram-file file)

    (print-now "✅ rendering...")
    (render-diagram-file file)
    (println "✅")

    (swap! active-set disj file)

    (catch Exception e
      (beep) ; good chance the user’s terminal is in the background
      (println "🚨" (or (.getMessage e) e)))))

(defn process-fs-event
  [_context {:keys [kind file] :as _event}]
  (swap! active-set conj file)
  (let [event-ts (LocalTime/now)]
    (.execute @executor
              (fn []
                ;; I don’t know why, but for some reason when this function is
                ;; called by Hawk in its background thread, *out* appears to be
                ;; bound to a writer that isn’t writing to System.out. We need
                ;; it to be System.out so the tests can capture the text written
                ;; to System.out for use in assertions.
                (binding [*out* (OutputStreamWriter. System/out)]
                  (process-file event-ts kind file))))))

(defn stop
  "Useful during development and testing."
  ([] (when @current-watch
        (stop @current-watch)))
  ([watch]
   (hawk/stop! watch)
   (when @executor
     (.shutdownNow @executor))))

(defn start
  [& paths]
  (stop)
  (reset! active-set #{})
  (reset! executor (. Executors newSingleThreadExecutor))
  (let [watch (hawk/watch! [{:paths   paths
                             :filter  process-fs-event?
                             :handler process-fs-event}])]
    (println "📣 Now watching for changes to YAML files under specified paths...")
    (reset! current-watch watch)))
