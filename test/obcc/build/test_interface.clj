;; Licensed to the Apache Software Foundation (ASF) under one
;; or more contributor license agreements.  See the NOTICE file
;; distributed with this work for additional information
;; regarding copyright ownership.  The ASF licenses this file
;; to you under the Apache License, Version 2.0 (the
;; "License"); you may not use this file except in compliance
;; with the License.  You may obtain a copy of the License at
;;
;;   http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing,
;; software distributed under the License is distributed on an
;; "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
;; KIND, either express or implied.  See the License for the
;; specific language governing permissions and limitations
;; under the License.

(ns obcc.build.test_interface
  (:require [clojure.test :refer :all]
            [obcc.build.interface :refer :all]
            [slingshot.slingshot :as slingshot])
  (:refer-clojure :exclude [compile]))

(def example1-cci
  "
  # This is a comment
  // So is this

  message ActiveMessage {
     // mid-message comment
     string param1 = 1; # trailing comment
     int32  param2 = 2;
     int64  param3 = 3;
  }

  //message CommentedMessage {
  //   int32 param1 = 1;
  //}
  "
  )

(def example1-expected-result
  [[:interface [:message "ActiveMessage" [:field [:type [:scalar "string"]] [:fieldName "param1"] [:index "1"]] [:field [:type [:scalar "int32"]] [:fieldName "param2"] [:index "2"]] [:field [:type [:scalar "int64"]] [:fieldName "param3"] [:index "3"]]]] nil])

(def example2-cci
  "
  message NestedMessage {
     message Entry {
        string key = 1;
        int32  value = 2;
     }

    repeated Entry entries = 1;
  }

  "
  )

(def example2-expected-result
  [[:interface [:message "NestedMessage" [:message "Entry" [:field [:type [:scalar "string"]] [:fieldName "key"] [:index "1"]] [:field [:type [:scalar "int32"]] [:fieldName "value"] [:index "2"]]] [:field [:modifier "repeated"] [:type [:userType "Entry"]] [:fieldName "entries"] [:index "1"]]]] nil])

(deftest test-parser-output
  (is (= example1-expected-result (parse example1-cci)))
  (is (= example2-expected-result (parse example2-cci))))

(def example-undefined-type-cci
  "
  message BadMessage {
     message Entry {
        string key = 1;
        UnknownType value = 2;
     }

    repeated Entry entries = 1;
  }

  "
  )

(deftest test-parser-validation
  (let [intf (parse example-undefined-type-cci)]
    (is (some? (verify-intf intf)))))
