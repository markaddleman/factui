(ns factui.ui.dev
  (:require [factui.api :as f :include-macros true]
            [factui.rum :as fr :refer [*results*]]
            [clara.rules :as cr :include-macros true]
            [rum.core :as rum :include-macros true]
            [cljs.core.async :as a])
  (:require-macros [clojure.core.async]))

(enable-console-print!)

(def schema
  [{:db/ident :task/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :task/completed
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one}])

(defn rand-string
  []
  (apply str (repeatedly (+ 5 (rand-int 10))
               #(rand-nth (seq "abcdefghijklmnopqrstuvwxyz")))))

(defn new-tasks
  "Generate txdata for N new tasks"
  [n]
  (repeatedly n (fn []
                  {:task/title (str "Task " (rand-string))
                   :task/completed false})))

(f/defquery task-q
  [:find [?title ?completed]
   :in ?task
   :where
   [?task :task/title ?title]
   [?task :task/completed ?completed]])

(rum/defc Task < {:key-fn (fn [_ id] id)}
                 (fr/query task-q ::session)
                 rum/static
  [app-state ?task]
  [:li (str "task:" ?task)]
  (let [[title completed] *results*]
    [:li
     [:span {:style {:cursor "pointer"
                     :font-weight "bold"}
             :on-click (fn []
                         (fr/transact! app-state
                           [{:db/id ?task
                             :task/completed (not completed)}]))}
      (if completed "DONE:" "TODO:")]
     " "
     title]))

(f/defquery tasklist-q
  [:find ?t ?title
   :where
   [?t :task/title ?title]])

(rum/defc TaskList < (fr/query tasklist-q ::session)
                     rum/static
  [app-state title]
  [:div
   [:h1 title]
   [:button {:on-click (fn []
                         (fr/transact! app-state (new-tasks 1)))}
    "Add Task"]
   [:button {:on-click (fn []
                         (fr/transact! app-state (new-tasks 10)))}
    "Add 10 Tasks"]
   [:button {:on-click (fn []
                         (fr/transact! app-state (new-tasks 500)))}
    "Add 500 Tasks"]
   [:br]
   [:br]
   [:div "Results:" (count *results*)]
   [:ul (for [[t _] *results*]
          (Task app-state t))]
   ])

(f/defsession base ['factui.ui.dev] schema ::session)

(def initial-data
  [{:task/title "Task A"
    :task/completed false}
   {:task/title "Task B"
    :task/completed false}
   {:task/title "Task C"
    :task/completed false}])

(defn ^:export main
  []


  (let [app-state (fr/app-state base)
        root (.getElementById js/document "root")]

    (fr/transact! app-state initial-data)

    (rum/mount (TaskList app-state "Tasks") root)

    ))