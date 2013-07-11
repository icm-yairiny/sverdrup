(ns sverdrup.core-test
  (:use midje.sweet)
  (:require [sverdrup.core :as sv]))

(defn do-something
  "this is a sample activity function for testing purposes"
  [workflow-context what-to-do]
  (clojure.pprint/pprint [:message (str "doing " what-to-do)
                          :context workflow-context]))

(def workflow-definition
  {:name "document workflow"
   :states
   {:initial
    {:transitions
     {:create-document
      {:activities [{:name 'sverdrup.core-test/do-something :params ["create the document"]}]
       :transition-to :in-progress}}}
    :in-progress
    {:transitions
     {:cancel
      {:activities [{:name 'sverdrup.core-test/do-something :params ["cancel the document"]}
                    {:name 'sverdrup.core-test/do-something :params ["notify the original owner"]}]
       :transition-to :cancelled}
      :ask-for-review
      {:activities [{:name 'sverdrup.core-test/do-something :params ["notify reviewer"]}]
       :transition-to :waiting-for-review}}}
    :waiting-for-review
    {:transitions
     {:approve {:activities [] :transition-to :complete}
      :reject {:activities [] :transition-to :in-progress}}}
    :cancelled {:final true}
    :complete {:final true}}
   :reassign
   {:activities [{:name 'sverdrup.core/do-something :params ["reassigned to you"]}]}})

(fact "creating a task passes right values to database creation function and returns the same object"
  (sv/create-task {:name ..name..} ..type-id.. ..id.. ..user..)=> ..returned-task..
  (provided (sverdrup.db/create-task ..name.. ..type-id.. ..id.. ..user..) => ..returned-task..))

(fact "transitioning a task finds the right new state"
  (sv/transition-task workflow-definition ..task-id.. :ask-for-review [] "thomas") =>
  ..transition-ret-value..
  (provided (sverdrup.db/load-task ..task-id..) =>
            {:id ..task-id.. :state "in-progress" :assigned_to "george"}
            (sverdrup.db/transition-task
             anything ..task-id.. "in-progress" "ask-for-review" "waiting-for-review"
             "george" "thomas")
            => ..transition-ret-value..))

(fact "reassigning a task leaves everything intact except for the assignee"
  (sv/reassign-task workflow-definition ..task-id.. "thomas") =>
  ..transition-ret-value..
  (provided (sverdrup.db/load-task ..task-id..) =>
            {:id ..task-id.. :state "in-progress" :assigned_to "george"}
            (sverdrup.db/transition-task
             anything ..task-id.. "in-progress" "reassign" "in-progress"
             "george" "thomas")
            => ..transition-ret-value..))
