(ns factui.rules-test
  (:require
    #?(:cljs [factui.api :as api :include-macros true]
       :clj [factui.api :as api])
    [factui.facts :as f]
    #?(:clj [clara.rules :as cr]
       :cljs [clara.rules :as cr :include-macros true])
   #?(:clj [factui.rules :as r]
      :cljs [factui.rules :as r :include-macros true])
    [clojure.pprint :refer [pprint]]
   [clara.rules.memory :as mem]
   #?(:clj [clojure.test :as t :refer [deftest is testing run-tests]]
      :cljs [cljs.test :as t :refer-macros [deftest is testing run-tests]])))

(def test-schema
  [{:db/ident :person/id
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :person/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :person/likes
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many}
   {:db/ident :person/age
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}])

(cr/defquery person-by-pid
  [:?pid]
  [::f/datom [{:keys [e a v]}] (= e ?p) (= a :person/id) (= v ?pid)]
  [::f/datom [{:keys [e a v]}] (= e ?p) (= a ?a) (= v ?v)])

#?(:cljs (enable-console-print!))

(api/defsession base* 'factui.rules-test)

(def base (api/transact-all base* test-schema))

(deftest tempids-resolve-to-same-entity
  (println "running test...")
  (let [tid -42
        sesh (api/transact-all base [{:db/id tid
                                      :person/name "Luke"}
                                     {:db/id tid
                                      :person/id 42
                                      :person/age 32}])

        results (cr/query sesh person-by-pid :?pid 42)]
    (is (= (set (map #(select-keys % [:?a :?v]) results))
          #{{:?a :person/name :?v "Luke"}
            {:?a :person/age :?v 32}
            {:?a :person/id :?v 42}}))))

(deftest identity-attribute-resolution
  (let [sesh1 (api/transact-all base [{:person/id 42
                                       :person/name "Luke"}
                                      {:person/id 42
                                       :person/age 32}])
        sesh2 (api/transact-all base [{:person/id 42
                                       :person/name "Luke"}]
                                     [{:person/id 42
                                       :person/age 32}])
        results1 (cr/query sesh1 person-by-pid :?pid 42)
        results2 (cr/query sesh2 person-by-pid :?pid 42)]
    (is (= (set (map #(select-keys % [:?a :?v]) results1))
           (set (map #(select-keys % [:?a :?v]) results2))
           #{{:?a :person/name :?v "Luke"}
             {:?a :person/age :?v 32}
             {:?a :person/id :?v 42}}))))

(deftest card-one-overwrites
  (let [sesh (api/transact-all base [{:person/id 42
                                       :person/name "Luke"}]
                                    [{:person/id 42
                                       :person/name "Luke VanderHart"}])

        results (cr/query sesh person-by-pid :?pid 42)]
    (is (= (set (map #(select-keys % [:?a :?v]) results))
           #{{:?a :person/name :?v "Luke VanderHart"}
             {:?a :person/id :?v 42}}))))