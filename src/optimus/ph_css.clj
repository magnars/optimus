(ns optimus.ph-css
  (:import (com.helger.base.system ENewLineMode)
           (com.helger.css.reader CSSReader CSSReaderSettings)
           (com.helger.css.writer CSSWriter CSSWriterSettings)
           (java.nio.charset StandardCharsets)))

(def new-line-modes
  {:default ENewLineMode/DEFAULT
   :mac ENewLineMode/MAC
   :uniw ENewLineMode/UNIX
   :windows ENewLineMode/WINDOWS})

(defn options->writer-settings [options]
  (let [new-line-mode (new-line-modes (:new-line-mode options))]
    (cond-> (-> (CSSWriterSettings.)
                (.setOptimizedOutput (:optimized-output? options true))
                (.setRemoveUnnecessaryCode (:remove-unnecessary-code? options true)))
      new-line-mode
      (.setNewLineMode new-line-mode)

      (number? (:indent options))
      (.setIndent (repeat (:indent options) " "))

      (contains? options :quote-urls?)
      (.setQuoteURLs (:quote-urls? options))

      (contains? options :write-namespace-rules?)
      (.setWriteNamespaceRules (:write-namespace-rules? options))

      (contains? options :write-nested-declarations?)
      (.setWriteNestedDeclarations (:write-nested-declarations? options))

      (contains? options :write-font-face-rules?)
      (.setWriteFontFaceRules (:write-font-face-rules? options))

      (contains? options :write-key-frames-rules?)
      (.setWriteKeyframesRules (:write-key-frames-rules? options))

      (contains? options :write-layer-rules?)
      (.setWriteLayerRules (:write-layer-rules? options))

      (contains? options :write-media-rules?)
      (.setWriteMediaRules (:write-media-rules? options))

      (contains? options :write-page-rules?)
      (.setWritePageRules (:write-page-rules? options))

      (contains? options :write-viewport-rules?)
      (.setWriteViewportRules (:write-viewport-rules? options))

      (contains? options :write-supports-rules?)
      (.setWriteSupportsRules (:write-supports-rules? options))

      (contains? options :write-property-rules?)
      (.setWritePropertyRules (:write-property-rules? options))

      (contains? options :write-unknown-rules?)
      (.setWriteUnknownRules (:write-unknown-rules? options)))))

(defn minify [^String css & [options]]
  (some->> (CSSReader/readFromStringReader
            css
            (-> (CSSReaderSettings.)
                (.setFallbackCharset StandardCharsets/UTF_8)))
           (.getCSSAsString (CSSWriter. (options->writer-settings options)))))
