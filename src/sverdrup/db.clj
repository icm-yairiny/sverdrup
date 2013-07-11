(ns sverdrup.db
  (:use [korma.db])
  (:use [korma.core]))

;;this database definition is temporary
(defdb wflow (postgres {:db "bpolsp"
                        :user "postgres"
                        :password "postgres"}))

(declare task_audit)

(defentity document)

(defentity task
  (has-many task_audit))

(defentity task_audit
  (belongs-to task))

(defn load-task
  "given the id of a task, loads it from the database, or returns nil"
  [id]
  (first (select task (where {:id id}))))

(defn- write-audit
  "writes an audit record to the database"
  [task-id initial-state transition executed-by new-state assignee]
  (insert task_audit (values {:task_id task-id
                              :initial_state initial-state
                              :transition transition
                              :new-state new-state
                              :executed_by executed-by
                              :assigned_to assignee})))

(defn- create-task-record
  "writes a task record to the database"
  [workflow-name assignee document-type-id document-id]
  (insert task (values {:workflow workflow-name
                        :state "initial"
                        :assigned_to assignee
                        :document_id document-id})))

(defn create-task [workflow-name document-type-id document-id assigned-user]
  (transaction
   (let [task (create-task-record workflow-name assigned-user document-type-id document-id)]
     (write-audit  (:id task) "initial" "creation" "initial" assigned-user assigned-user)
     task)))

(defn transition-task [activities-fn task-id initial-state transition-name new-state initial-user new-user]
  (transaction
   (activities-fn)
   (write-audit task-id initial-state transition-name new-state
                initial-user new-user)
   (update task (set-fields {:state new-state :assigned_to new-user}))))
