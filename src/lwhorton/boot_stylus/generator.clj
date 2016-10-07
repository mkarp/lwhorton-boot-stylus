(ns lwhorton.boot-stylus.generator
  (:require [clojure.data.json :as json]))

(defn- create-namespace [name]
  (str "(ns " name " (:require [lwhorton.boot-stylus.runtime]))"))

(defn- str-initializer [id css]
  (let [update-fn (symbol "lwhorton.boot-stylus.runtime" "update-stylesheet!")
        full-name (str (symbol (str *ns*) (name id)))
        sheet {:name full-name :source css}]
    `(do
       (~update-fn ~sheet))))

(defn- create-class [classname value]
  (str "(def " (name classname) " \"" value "\")"))

(defn- create-module-contents [namespace edn css]
  (let [head (create-namespace namespace)
        body (reduce-kv #(str %1 (create-class %2 %3) "\n") "" edn)
        footer (str-initializer namespace css)]
    (str head "\n" body "\n" footer)))

(defn parse-stdout
  "Convert the stdout generated by run_postcss.js into separate json (edn) and
  css (string). The stdout is delimited by ~json~{...json...}~json~ for json and
  likewise for css."
  [stdout]
  ;; stdout is a string, so it's harder to get errors, but we know if the very
  ;; first characters dont match ~json~, there's some sort of error
  (if (not= (subs stdout 0 6) "~json~")
    {:err (first (clojure.string/split stdout #"~json~"))}
    (let [split (clojure.string/split stdout #"~(json|css)~")
          edn (json/read-str (second split) :key-fn keyword)
          css (last split)]
      {:edn edn :css css})))

(defn create-module [namespace stdout]
  (let [parsed (parse-stdout stdout)]
    (if (:err parsed)
      parsed
      (create-module-contents namespace (:edn parsed) (:css parsed)))))

