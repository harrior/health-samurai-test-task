(ns patients.validate
  "This module contains functions for validating patient data."
  (:require [clojure.spec.alpha :as s]))

;;
;; Common specs
;;

(s/def ::non-empty-string (s/and string?
                                 not-empty))

(s/def ::date (s/and #(re-matches #"\d{4}-\d{2}-\d{2}" %)
                     #(let [data-pattern (java.text.SimpleDateFormat. "yyyy-MM-dd")
                            parsed-date (.parse data-pattern %)]
                        (= %
                           (.format data-pattern parsed-date)))))

(s/def :period/start ::date)
(s/def :period/end ::date)

(s/def ::period (s/keys :opt-un [:period/start
                                 :period/end]))

;;
;; Specs for patient name
;;

(s/def :name/use #{"usual" "official" "temp" "nickname" "anonymous" "old" "maiden"})
(s/def :name/text ::non-empty-string)
(s/def :name/family ::non-empty-string)
(s/def :name/given (s/coll-of ::non-empty-string))
(s/def :name/prefix (s/coll-of ::non-empty-string))
(s/def :name/suffix (s/coll-of ::non-empty-string))
(s/def :name/period ::period)

(s/def ::name (s/coll-of
               (s/keys :req-un [:name/use
                                :name/text
                                :name/family
                                :name/given]
                       :opt-un [:name/suffix
                                :name/prefix
                                :name/period])))
;;
;; Specs for address
;;

(s/def :address/use #{"home" "work" "temp" "old" "billing"})
(s/def :address/type #{"postal" "physical" "both"})
(s/def :address/text ::non-empty-string)
(s/def :address/line ::non-empty-string)
(s/def :address/city ::non-empty-string)
(s/def :address/district ::non-empty-string)
(s/def :address/state ::non-empty-string)
(s/def :address/postal-code ::non-empty-string)
(s/def :address/country ::non-empty-string)
(s/def :address/period ::period)
(s/def ::address (s/coll-of
                  (s/keys :req-un [:address/use
                                   :address/type
                                   :address/text
                                   :address/line
                                   :address/city
                                   :address/country]
                          :opt-un [:address/postal-code
                                   :address/state
                                   :address/district
                                   :address/period])))

;;
;; Specs for patient
;;

(s/def :patient/address ::address)
(s/def :patient/name ::name)
(s/def :patient/identifier uuid?)
(s/def :patient/gender #{"male" "female" "other" "unknown"})
(s/def :patient/birth-date ::date)
(s/def :patient/insurance-number (s/and ::non-empty-string
                                        #(re-matches #"\d{16}" %)))

(s/def ::patient
  (s/keys :req-un [:patient/name
                   :patient/insurance-number
                   :patient/gender
                   :patient/birth-date
                   :patient/address]
          :opt-un [:patient/identifier]))

;;
;; Validation functions
;;

(defn generate-error-path-for-explain-data
  "Generates error path for explain-data."
  [explain-data]
  (let [{:keys [in path pred]} explain-data
        is-missed-key? (or (empty? in)
                           (not= (last in) (last path)))]
    (if is-missed-key?
      (->> pred
           last
           last
           (conj in))
      in)))

(defn get-patient-validation-error-paths
  "Retrieves validation error paths for a patient record."
  [patient]
  (->> (s/explain-data ::patient patient)
       :clojure.spec.alpha/problems
       (map generate-error-path-for-explain-data)))

(defn patient-is-valid?
  "Checks whether a patient record is valid."
  [patient]
  (s/valid? ::patient patient))

(defn patient-identifier-valid?
  "Returns true if the given patient-identifier is a valid UUID, false otherwise."
  [patient-identifier]
  (uuid? patient-identifier))
