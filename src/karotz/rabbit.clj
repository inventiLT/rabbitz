(ns karotz.rabbit
  (:require
   [compojure.handler]
   [clojure.java.io :as io]
   [karotz.api :as api]
   [karotz.tts :as tts]))

(def tokens (ref {}))

(defn load-props [file]
  (let [props (java.util.Properties. )]
    (with-open [reader (io/reader (io/resource file))]
    (.load props reader)
    (into {} (for [[k v] props] [(keyword k) v])))))

(def props (load-props "api.properties"))

(defn store [id token]
  (dosync
   (alter tokens #(assoc % id token))))

(defn receive-token [id]
  (let [old-token ((deref tokens) id)
        token (api/sign-in [id old-token] props)]
    (store id token)
    token))

(defn speech-url [id text]
  (let [file (str id ".wav")
        local-file (str (:file-root props) "/" file)]
    (tts/to-file "en" text (io/file local-file))
    (str (:file-url props) "/" file)))

(defn move-ears [id left right]
  (let [token (receive-token id)]
    (api/move-ears token left right)))

(defn speek [id text]
  (let [token (receive-token id)
        speech (speech-url id text)]
    (api/play-media token speech)))
