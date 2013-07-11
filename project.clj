(defproject sverdrup "0.1.0-SNAPSHOT"
  :description "A lightweight clojure workflow engine"
  :url "http://www.icm-consulting.com.au"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [postgresql "9.1-901.jdbc4"]
                 [korma "0.3.0-RC5"]
                 [midje "1.5.1" :scope "test"]]
  :plugins [[lein-marginalia "0.7.1"]
            [lein-midje "3.0.1"]])
