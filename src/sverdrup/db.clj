(ns sverdrup.db
  (:use [korma.db]
        [korma.core]
        [korma.config]))

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

(defn underscores->dashes [n]
  (-> n clojure.string/lower-case (.replaceAll "_" "-") keyword))

(defn dashes->underscores [n]
  (-> n name (.replaceAll "-" "_")))

(set-naming {:keys underscores->dashes :fields dashes->underscores})

(defn load-task
  "given the id of a task, loads it from the database, or returns nil"
  [id]
  (first (select task (where {:id id}))))

(defn- write-audit
  "writes an audit record to the database"
  [task-id initial-state transition executed-by new-state assignee]
  (insert task_audit (values {:task-id task-id
                              :initial-state initial-state
                              :transition transition
                              :new-state new-state
                              :executed-by executed-by
                              :assigned-to assignee})))

(defn- create-task-record
  "writes a task record to the database"
  [workflow-name assignee document-type-id document-id]
  (insert task (values {:workflow workflow-name
                        :state "initial"
                        :assigned-to assignee
                        :document-type-id document-type-id
                        :document-id document-id})))

(defn create-task [workflow-name document-type-id document-id assigned-user]
  "creates a task record and audit record"
  (transaction
   (let [task (create-task-record workflow-name assigned-user document-type-id document-id)]
     (write-audit  (:id task) "initial" "creation" "initial" assigned-user assigned-user)
     task)))

(defn transition-task [activities-fn task-id initial-state transition-name new-state initial-user new-user]
  "creates the audit record and modifies the task informatio, after running the given activities inside the same transaction"
  (transaction
   (activities-fn)
   (write-audit task-id initial-state transition-name new-state
                initial-user new-user)
   (update task (set-fields {:state new-state :assigned-to new-user}))))

(defn find-tasks-for-document
  "given a document type and document ID, returns all the tasks associated with the document, ordered by ID"
  [document-type-id document-id]
  (select task (where {:document-type-id document-type-id :document-id document-id})))
