(defproject link-shortener "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [http-kit "2.3.0"]
                 [ring "1.7.1"]
                 [compojure "1.6.1"]
                 [ring/ring-mock "0.4.0"]
                 [ring/ring-json "0.4.0"]
                 [commons-validator "1.6"]
                 [environ "1.1.0"]
                 [metosin/spec-tools "0.4.0"]
                 [metosin/compojure-api "2.0.0-alpha30"]
                 [org.clojure/spec.alpha "0.2.176"]
                 [cheshire "5.8.1"]
                 [metosin/spec-tools "0.9.2-alpha1"]]
  :ring {:handler link-shortener.core/app}
  :min-lein-version "2.0.0"
  :uberjar-name "link-shortener.jar"
  :main ^:skip-aot link-shortener.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev     {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]]
                       :plugins      [[lein-ring "0.12.0"]]}})
