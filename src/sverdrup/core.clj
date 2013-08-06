(ns sverdrup.core
  (:require [sverdrup.db :as db]))

;;business rules
;;
;; only one active workflow per document
;;

;;missing functionality
;;
;;exception handling
;;completing a task



(defn- execute-activities
  "takes a sequence of activities, each one is a map of :name and :params where :name is the symbol that resolves into a function that takes as a first parameter the context of the task and the :params vector"
  [activities context]
  (doseq [activity activities]
    (apply (resolve (:name activity)) context (:params activity))))

(defn is-any-active
  "takes a list of tasks for a given document, and returns a flag indicating whether any are still active"
  [document-tasks]
  (let [is-active? (fn [task]
                     (not (task :complete)))]
    (some is-active? document-tasks)))

(defn create-task [workflow document-type-id document-id assigned-user]
  "creates a task using the given workflow definition, the document identification, and starts it up assigned to the given user"
  ;; Get all existing tasks for given document-type-id and document-id
  ;; and determine if any are still active
  (if (is-any-active (db/find-tasks-for-document document-type-id document-id))
    (throw (IllegalStateException. "There is an existing active task. Cannot create a new task"))
    (db/create-task (:name workflow) document-type-id document-id assigned-user)))

(defn transition-task
  "transitions a task from one state to the other, given the workflow definition, the task id, a set of black box parameters and the new user to which the task will be assigned in its new state"
  [workflow task-id transition parameters new-user]
  (let [the-task (db/load-task task-id)
        context {:task the-task :params parameters :new-user new-user :transition transition}
        state-kw (keyword (:state the-task))
        the-transition (-> workflow :states state-kw :transitions transition)
        activities (:activities the-transition)
        initial-state-str (:state the-task)
        new-state-kw (:transition-to the-transition)
        new-state (-> workflow :states new-state-kw)
        initial-user (:assigned-to the-task)]
    (db/transition-task
     #(execute-activities activities context)
     (:id the-task) initial-state-str (name transition) (name new-state-kw) (or (:final new-state) false) initial-user new-user)))

(defn reassign-task
  "reassigns the task to another user without making a transition"
  [workflow task-id new-user]
  (let [the-task (db/load-task task-id)
        initial-state (:state the-task)
        initial-user (:assigned_to the-task)
        context {:task the-task :new-user new-user :transition :reassign}
        activities (-> workflow :reassign :activities)]
    (db/transition-task
     #(execute-activities activities context)
     task-id initial-state "reassign" initial-state initial-user new-user)))


  