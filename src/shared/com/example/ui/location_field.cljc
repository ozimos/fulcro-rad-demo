(ns com.example.ui.location-field
  (:require
   #?@(:cljs
       [["react" :as react]
        ["react-tag-input" :rename {WithContext ReactTags}]
        [com.fulcrologic.fulcro.algorithms.react-interop :as interop]
        [goog.object :as gobj]])
   [clojure.string :as str]
   [com.fulcrologic.fulcro.dom.inputs :as d.inputs]
   [com.fulcrologic.rad.rendering.semantic-ui.field :refer [render-field-factory]]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]))

(defn db->val
  "Convert db representation to react tag input repr"
  [db-val]
  (if (seq db-val)
    (mapv (fn [v] {:id v :text v}) db-val)
    []))

(defn val->db
  "Convert tagged input val to db representation"
  [input-val]
  (if (seq input-val) (vec (keep (fn [v] (:text v)) input-val)) []))


(defn propagate-changes [onChange ntv]
  (when onChange
    (onChange (val->db ntv))))


#?(:cljs
   (def react-tags (interop/react-factory ReactTags)))

(let [keycode-delims [10 13 188]]



  (defsc TaggedInput [this {:keys [value suggestions onBlur] :as props}]
    {:initLocalState (fn [this _] {:handle-delete #?(:cljs (fn [deleted-val]
                                                             (let [{:keys [value onChange]} (comp/props this)
                                                                   tags (db->val value)
                                                                   ntv (keep-indexed
                                                                        (fn [i v] (when-not (= deleted-val i) v))
                                                                        tags)]
                                                               (propagate-changes onChange ntv))))
                                   :handle-addition #?(:cljs (fn [added-val]
                                                               (let [{:keys [value onChange]} (comp/props this)
                                                                     tags (db->val value)
                                                                     ntv (conj tags (js->clj added-val :keywordize-keys true))]
                                                                 (propagate-changes onChange ntv))))
                                   :handle-drag #?(:cljs (fn [new-val curr-pos new-pos]
                                                           (let [{:keys [value onChange]} (comp/props this)
                                                                 tags (db->val value)
                                                                 ntv (transduce
                                                                      (map-indexed (fn [i v] [i v]))
                                                                      (completing (fn [a [i v]]
                                                                                    (cond
                                                                                      (= i new-pos) (conj a (js->clj
                                                                                                             new-val
                                                                                                             :keywordize-keys true) v)
                                                                                      (= i curr-pos) a
                                                                                      :else (conj a v))))
                                                                      [] tags)]
                                                             (propagate-changes onChange ntv))))})}

    #?(:cljs
       (let [{:keys [handle-delete handle-addition handle-drag]} (comp/get-state this)]
         (react-tags
          (merge props
                 (cond->
                  {:tags (db->val value)
                   :delimiters keycode-delims
                   :suggestions (db->val suggestions)
                   :handleDelete handle-delete
                   :handleAddition handle-addition
                   :handleDrag handle-drag}
                   onBlur (assoc :onBlur (fn [_]
                                           (onBlur value))))))))))

(def ui-tag-input
  "A vector input using the React Tag Library"
  (comp/factory TaggedInput))

(def ui-vector-input
  "A vector input. Used just like a DOM input, but requires you supply nil or a vector of strings for `:value`, and
   will send a vector to `onChange` and `onBlur`. Any other attributes in props are passed directly to the
   underlying `dom/input`."
  (comp/factory (d.inputs/StringBufferedInput ::VectorInput {:model->string #(str/join ", " %)
                                                             :string->model #(str/split % #", ")})))


(def tag-render-field (render-field-factory ui-tag-input))
(def vector-render-field (render-field-factory ui-vector-input))