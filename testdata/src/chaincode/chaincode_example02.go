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
	"errors"
	"fmt"
	"strconv"

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

	fmt.Printf("Aval = %d, Bval = %d\n", param.PartyA.GetValue(), param.PartyB.GetValue())

	// Write the state to the ledger
	err = t.PutState(shim, param.PartyA)
	if err != nil {
		return err
	}

	err = t.PutState(shim, param.PartyB))
	if err != nil {
		return err
	}

	return nil
}

// Transaction makes payment of X units from A to B
func (t *ChaincodeExample) MakePayment(stub *shim.ChaincodeStub, param *chaincode_support.PaymentParams) error {

	var err error

	// Get the state from the ledger
	src, err := t.GetState(stub, param.GetPartySrc())
	if err != nil {
		return err
	}

	dst, err := t.GetState(stub, param.GetPartyDst())
	if err != nil {
		return err
	}

	// Perform the execution
	X := param.GetAmount()
	src = src - X
	dst = dst + X
	fmt.Printf("Aval = %d, Bval = %d\n", src, dst)

	// Write the state back to the ledger
	err = stub.PutState(param.GetPartySrc(), []byte(strconv.Itoa(src)))
	if err != nil {
		return err
	}

	err = stub.PutState(param.GetPartyDst(), []byte(strconv.Itoa(dst)))
	if err != nil {
		return err
	}

	return nil
}

// Deletes an entity from state
func (t *ChaincodeExample) delete(stub *shim.ChaincodeStub, args []string) ([]byte, error) {
	if len(args) != 1 {
		return nil, errors.New("Incorrect number of arguments. Expecting 3")
	}

	A := args[0]

	// Delete the key from the state in ledger
	err := stub.DelState(A)
	if err != nil {
		return nil, errors.New("Failed to delete state")
	}

	return nil, nil
}


// Query callback representing the query of a chaincode
func (t *ChaincodeExample) CheckBalance(stub *shim.ChaincodeStub, param *chaincode_support.BalanceParams) (*chaincode_support.BalanceResult, error) {
	var err error

	// Get the state from the ledger
	val, err := t.GetState(stub, param.GetParty())
	if err != nil {
		return nil, err
	}

	fmt.Printf("Query Response: %d\n", val)
	return &chaincode_support.BalanceResult{Balance: val}, nil
}

func main() {
	self := &ChaincodeExample{}
	err := chaincode_support.Start(self, self) // Our one instance implements both Transactions and Queries interfaces
	if err != nil {
		fmt.Printf("Error starting example chaincode: %s", err)
	}
}

//-------------------------------------------------
// Helpers
//-------------------------------------------------
func (t *ChaincodeExample) PutState(stub *shim.ChaincodeStub, party *chaincode_support.Party) error {
	return stub.PutState(party.GetEntity(), []byte(strconv.Itoa(party.GetValue())))
}

func (t *ChaincodeExample) GetState(stub *shim.ChaincodeStub, entity *string) (int32, error) {
	bytes, err := stub.GetState(entity)
	if err != nil {
		return nil, errors.New("Failed to get state")
	}
	if bytes == nil {
		return nil, errors.New("Entity not found")
	}
	val, _ := strconv.Atoi(string(bytes))

	return val, nil
}
