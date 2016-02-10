/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package main

import (
	"fmt"

	"chaincode_support"
	"github.com/openblockchain/obc-peer/openchain/chaincode/shim"
)

type ChaincodeExample struct {
	chaincode_support.Transactions
	chaincode_support.Queries
}

// Called to initialize the chaincode
func (t *ChaincodeExample) Init(stub *shim.ChaincodeStub, param *chaincode_support.Init) error {

	var err error

	// Write the state to the ledger
	err = stub.PutState("ProxyAddress", []byte(param.GetAddress()))
	if err != nil {
		return err
	}

	return nil
}

// Transaction makes payment of X units from A to B
func (t *ChaincodeExample) MakePayment(stub *shim.ChaincodeStub, param *chaincode_support.PaymentParams) error {

	var err error

	// Get the state from the ledger
	addr, err := stub.GetState("ProxyAddress")
	if err != nil {
		return err
	}

	return chaincode_support.MakePayment(stub, string(addr), param)
}

// Deletes an entity from state
func (t *ChaincodeExample) DeleteAccount(stub *shim.ChaincodeStub, param *chaincode_support.Entity) error {

	var err error

	// Get the state from the ledger
	addr, err := stub.GetState("ProxyAddress")
	if err != nil {
		return err
	}

	return chaincode_support.DeleteAccount(stub, string(addr), param)
}

// Query callback representing the query of a chaincode
func (t *ChaincodeExample) CheckBalance(stub *shim.ChaincodeStub, param *chaincode_support.Entity) (*chaincode_support.BalanceResult, error) {

	var err error

	// Get the state from the ledger
	addr, err := stub.GetState("ProxyAddress")
	if err != nil {
		return nil, err
	}

	return chaincode_support.CheckBalance(stub, string(addr), param)
}

func main() {
	self := &ChaincodeExample{}
	err := chaincode_support.Start(self, self) // Our one instance implements both Transactions and Queries interfaces
	if err != nil {
		fmt.Printf("Error starting example chaincode: %s", err)
	}
}
