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

(ns chaintool.platforms.golang.test_system
  (:require [clojure.test :refer :all]
            [chaintool.platforms.golang.system :refer :all])
  (:refer-clojure :exclude [compile]))

(deftest test-standalone-gopath-computation
  (let [gopath (get-gopath "/local-dev/git/chaintool/testdata/sample_syscc"
                           "_/local-dev/git/chaintool/testdata/sample_syscc")]
    (is (= gopath "/"))))

(deftest test-gopath-computation
  (let [gopath (get-gopath "/opt/gopath/src/github.com/hyperledger/fabric/core/system_chaincode/sample"
                           "github.com/hyperledger/fabric/core/system_chaincode/sample")]
    (is (= gopath "/opt/gopath/src"))))
