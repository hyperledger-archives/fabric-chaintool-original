[![Build Status](https://travis-ci.org/ghaskins/chaintool.svg?branch=master)](https://travis-ci.org/ghaskins/chaintool)

# chaintool - Hyperledger Chaincode Compiler (Work In Progress)

## Introduction

_chaintool_ is a proposal for a toolchain to assist in various phases of [hyperledger](https://github.com/hyperledger) chaincode development, such as compilation, test, packaging, and deployment.  A chaincode app developer may express the interface in a highlevel protobuf structure and _chaintool_ will generate (1) the chaincode with appropriate methods stub and (2) package it for the user so it can be directly deployed.

### Why?

Current chaincode development is rather unstructured outside of the coarse-level callbacks for invoke or query passing a {function-name, argument-array} string-based tuple.  The result of this is that input translation/validation is a manual, explicit, and likely fragile process in each chaincode function.  Additionally, any potential chaincode consumer needs to study the chaincode source in order to ascertain its API.

Consider that some chaincode applications may employ confidentiality to hide their source, while others may wish to employ alternative programming languages.  This aside, chaincode deployment lifecycles may be long enough to require us to be aware of managing potential API incompatibilities between the chaincode and its clients.  It starts to become clear that there are some advantages to allowing chaincode to express its API interfaces in a way that is independent from the underlying implementation/language and in a manner that supports some form of schema management.

_chaintool_ helps in this regard by allowing applications to declare/consume one or more language neutral interface-definitions and package it with the project.  It helps the developer by generating shim/stub code in their chosen programming language that helps them implement and/or consume the interfaces declared.  This means that external parties may introspect a given instance for its interface(s) in a language neutral manner without requiring access to and/or an ability to decipher the underlying code.  It also means that we can use [protobufs](https://developers.google.com/protocol-buffers/) to help with various API features such as managing forwards/backwards compatibility, endian neutrality, basic type validation, etc in a largely transparent manner.

_chaintool_ provides some other benefits too, such as consistent language-neutral packaging and chaincode hashing, which help to simplify both the hyperledger fabric implementation and developer burden.

## Getting Started

### Prerequisites

- A Java JRE/JDK v1.7 (or higher)

### Installation

   $ make install

### Usage

```
$ chaintool -h
chaintool version: v0.7

Usage: chaintool [general-options] action [action-options]

General Options:
  -v, --version  Print the version and exit
  -h, --help

Actions:
  build -> Build the chaincode project
  buildcar -> Build the chaincode project from a CAR file
  clean -> Clean the chaincode project
  package -> Package the chaincode into a CAR file for deployment
  unpack -> Unpackage a CAR file
  ls -> List the contents of a CAR file
  proto -> Compiles a CCI file to a .proto

(run "chaintool <action> -h" for action specific help)
```
### Working with _chaintool_

The idiomatic way to use _chaintool_ is to treat it similar to other build tools such as Make, Maven, or Leiningen.  That is, by default it expects to be executed from within your [project root](#project-structure).  Subcommands such as _build_, _clean_, and _package_ fall into this category.  You can run it outside of a project root by using the "-p" switch to these commands to inform _chaintool_ where your project root is when it is not the current directory.

Other commands such as _buildcar_, _unpack_, and _ls_ are designed to operate against a Chaincode Archive (CAR) from a previous _package_ operation.  These commands expect a path to a CAR file.

In all cases, you may obtain subcommand specific help by invoking "chaintool _$subcommand_ -h".  For example:

```
$ chaintool package -h
chaintool version: v0.7

Description: chaintool package - Package the chaincode into a CAR file for deployment

Usage: chaintool package [options]

Command Options:
  -o, --output NAME          path to the output destination
  -c, --compress NAME  gzip  compression algorithm to use
  -p, --path PATH      ./    path to chaincode project
  -h, --help
```

### Typical Workflow

![typical-worflow](images/typical-workflow.png)

### Subcommand Details

#### chaintool build

Builds your chaincode project into a binary ready for execution on a blockchain.  Various artifacts are emitted to ./build, depending on the platform.  For [org.hyperledger.chaincode.golang](./documentation/platforms/golang/README.md):

- ./build/src: shim, protobufs, etc
- ./build/deps: direct and transitive dependencies of your chaincode, as retrieved by "go get".  NOTE: this option is likely to default to disabled in the future, since it is not a good idea for a validating peer to be pulling dependencies down.  Rather, there should be some fixed number of dependencies that are implicitly included with the platform.  For now, we pull things in dynamically.
- ./build/bin: the default location for the binary generated (override with -o)

#### chaintool clean

Cleans a chaincode project.  This typically translates to removing the ./build directory, but platforms are free to define this as they see fit and may perform additional or alternative operations.

#### chaintool package

Packages the sourcecode, interfaces, chaincode.yaml, and other project data into a .car file suitable for deployment.  Note that any artifacts generated by commands such as _build_ and _buildcar_ are _not_ included but rather will be rebuilt locally by each validating peer in the network.

#### chaintool ls

Displays the contents of an existing .car file
```
$ chaintool ls ./build/org.hyperledger.chaincode.example02-0.1-SNAPSHOT.car
|------+------------------------------------------+--------------------------------------------------------|
| Size |                   SHA1                   |                          Path                          |
|------+------------------------------------------+--------------------------------------------------------|
| 438  | d28b22c7c30506af926dcb5bc8b946ac35ddac7f | chaincode.yaml                                         |
| 3856 | 542d088197e1a46bc21326e67e5d84d2d2807283 | src/chaincode/chaincode_example02.go                   |
| 143  | 7305f65e18e4aab860b201d40916bb7adf97544f | src/interfaces/appinit.cci                             |
| 375  | 9492a1e96f380a97bba1f16f085fc70140154c65 | src/interfaces/org.hyperledger.chaincode.example02.cci |
|------+------------------------------------------+--------------------------------------------------------|
Platform:            org.hyperledger.chaincode.golang version 1
Digital Signature:   none
Raw Data Size:       4812 bytes
Archive Size:        2371 bytes
Compression Alg:     gzip
Chaincode SHA3:      f7026e0675b22a9d78b9f7f0cb97c93165bdefedc86de97f00e76b506c707b4ddbdfe97ad702ad600eae518891b9f0f1c8cb9a8b29b83908c2f6d46a6bcf4ecd
```

#### chaintool unpack

Unpacks a .car archive into the filesystem as a chaincode project.

#### chaintool buildcar

Combines _unpack_ with _build_ by utilizing a temporary directory.  This allows a project to be built from a .car file without explicitly unpacking it first, as a convenience.

#### chaintool proto

Compiles a .cci file into a .proto file, suitable for developing clients using standard protobuf-based tooling.

## Project Structure

Like many modern build tools, _chaintool_ is opinionated.  It expects a specific structure to your project as follows:

- [chaincode.yaml](./examples/example02/app/chaincode.yaml) in the top-level directory of your project (discussed below)
- a chaincode entry-point in ./src/chaincode ([example](./examples/example02/app/src/chaincode/chaincode_example02.go))
- interface files in ./src/interfaces ([example](./examples/example02/app/src/interfaces/org.hyperledger.chaincode.example02.cci))
   - every project must define an appinit interface ./src/interfaces/appinit.cci ([example](./examples/example02/app/src/interfaces/appinit.cci))

### chaincode.yaml

_chaincode.yaml_ is the central configuration file for a given chaintool-managed chaincode project.  An example looks like this:

```
# ----------------------------------
# chaincode example02
# ----------------------------------
#
# Copyright (C) 2016 - Hyperledger
# All rights reserved
#

Schema:  1
Name:    org.hyperledger.chaincode.example02
Version: 0.1-SNAPSHOT

Platform:
        Name: org.hyperledger.chaincode.golang
        Version: 1

Provides: [self] # 'self' is a keyword that means there should be $name.cci (e.g. org.hyperledger.chaincode.example02.cci)
```

All chaincode.yaml should minimally contain:

- schema
- project name/version
- platform
- interface declarations (provides/consumes)

#### Schema
This helps to relay compatibility with the structures used in the chaincode.yaml itself.  At the time of writing, it should be "1".

#### Project name/version

This is something that should uniquely identify your chaincode project for human/UX consumption.  It is generally advised that a DNS name of some kind be incorporated to promote global uniqueness.  Note that the Hyperledger subsystem in general does not interpret these names in any meaningful way other than for display purposes.

#### Platform

It is here that a chaincode may declare the compatibility/conformity to a specific platform.  The idea is to promote extensibility (e.g. other platforms may be added in the future) and also compatility (e.g. platform X, version Y may mean something very specifically about the type of chaincode language supported, the ABI for any peripheral libraries, etc).  It is analogous to the notion that java 1.7 is a different ABI than java 1.8, etc.  At the time of writing, the only supported platform is org.hyperledger.chaincode.golang version 1.  More platforms may be added in the future.

##### Adding platforms

The only core requirement is that both _chaintool_ and the chosen Hyperledger network are in agreement to support said platform.  The details of implementing this are "coming soon".

#### Interface Declarations

Interfaces (as included in ./src/interfaces) may be in one or two categories: Provided or Consumed.  _Provided_ means that the chaincode implements the interface and supports having clients or other chaincode invoke methods as declared.  Likewise, _consumed_ indicates that the chaincode expects to perform inter-chaincode invoke/query operations to a disparate chaincode instance that provides the interface.  It is perfectly fine (though perhaps uncommon) for a chaincode to both provide and consume a given interface (such as for proxy contracts which may accept operations in a polymorphic manner before passing operations on to a concrete instance).

Both Provides and Consumes are expressed as an array of 1 or more entries.  For example:

```
Provides: [org.hyperledger.chaincode.example02, org.hyperledger.chaincode.example03]
Consumes: [org.hyperledger.chaincode.example02]
```

If there aren't any interfaces in a particular category, the entry may be omitted.  Note that a chaincode that doesn't provide any interfaces doesn't sound particularly useful, however.  Therefore, it is expected that every project will include at least a Provides clause.

##### "self"

The keyword _self_ may be used as shorthand for an interface that shares the same name as the project (for instance, the org.hyperledger.chaincode.example02 project surfacing the org.hyperledger.chaincode.example02.cci interface), as a convenience.  It is idiomatic for a project to name its primary interfaces after itself, and therefore this shortcut is expected to be commonly used.  Example:

```
Provides: [self]
```

### Chaincode

The opinionated portion of chaincode path solely applies to the entry-point for your application.  Other paths for non-entry point code are generally fine if you are using a language that supports namespaces, etc.  For instance, the org.hyperledger.chaincode.golang platform assumes a $GOPATH of ./src and tries to build "chaincode" (via $GOPATH/src/chaincode).  However, if your chaincode uses go imports such as:

```golang
import (
   "foo"
   "bar/baz"
)
```

placed in ./src/foo and ./src/bar/baz respectively, they will be discovered perfectly fine.

### Interfaces

An interface is a file ending in .cci (Chaincode Interface) that defines a language neutral definition for various RPC-like functions that a given chaincode instance supports.  An chaincode instance may in fact support many different interfaces at one time.  This is convenient for creating a type of polymorphism within a network of chaincode instances.

Each .cci file is meant to represent an interface contract for compatibility.  Items declared within a .cci file have provisions (similar to protobuf indices) for mutating structures over time that do not break forwards or backwards compatibility.  Changes to a given interface should only be done in a manner which exploits this compatibility mechanism.  If for some reason it is mandated that compatibility _must_ be broken, the name of the interface should be changed.

#### Interface names

The name of the .cci file has direct meaning to the ABI: the name of the file will be translated into ABI tokens on the wire.  This was intentionally chosen so that the filesystem itself (under ./src/interfaces) takes a role in ensuring that only one interface of a particular type is in use within a project at any given time.  Likewise, if a project wishes to import and consume an interface from a different project, it is imperative that the filename be retained across both projects or the endpoints will be inadvertently namespace-isolated from one another.  To put it another way, do not rename .cci files on import!

Perhaps even more importantly, interface ABI needs to be globally managed.  Therefore it is advised to name .cci files in a way that is globally unique.  A UUID would suffice, at the expense of being somewhat difficult to humans to deal with.  Therefore, it is advised to name interfaces using DNS names as in the examples provided here.

#### Interface namespaces

Given the potential for multiple interfaces to use conflicting names, there is a need to let a project place specific interfaces in a unique namespace when necessary.  However, it was felt that the default mode should make it as easy as possible for a chaincode developer to work with the system.  Therefore, the default import of an arbitrary .cci file will emit tokens in the global namespace for ease of use.  A future mechanism will be added to the chaincode.yaml that will allow a developer to assign arbitrary interfaces to specific namespaces w.r.t. emitted tokens without affecting the wire ABI.  TBD.  For now, note that tokens declared within interfaces files within the same project may not conflict.

#### Definition

Each interface definition loosely adheres to a protobuf-ish syntax.  This was intentional, as the .cci file is actually translated into an intermediate .proto file before being handed to protoc to do the real work.  The reason we did not just use protobuf syntax directly was because it was felt there were a few areas of the protobuf grammar that were suboptimal w.r.t. chaincode definition.  Consider an example .cci:

```
message PaymentParams {
        string partySrc = 1;
        string partyDst = 2;
        int32  amount   = 3;
}

message Entity {
        string id = 1;
}

message BalanceResult {
        int32 balance = 1;
}

transactions {
        void MakePayment(PaymentParams) = 1;
        void DeleteAccount(Entity) = 2;
}

queries {
        BalanceResult CheckBalance(Entity) = 1;
}
```

The _message_ definitions are almost 1:1 with protobuf grammar.  The largest divergence is w.r.t. the _transactions_ and _queries_ sections.  These two are similar to one another as well as to the notion of service/rpc in protobuf grammar.  The reason we diverged is for a few different reasons:

- Chaincode has a strong delineation between and invoke and a query, and it was important for the parser to be able to understand the breakdown so that the proper code could be emitted
- It was felt that the lack of "field indices" in the protobuf service/rpc grammar was a large shortcoming in ABI compatibility.  Therefore, the grammar used here retains the notion of indices even for function calls.

The main purpose of the grammar is to define RPC functions.  For reasons of ABI stability, it was decided that all RPCs will have the following properties:
- Be indexed (e.g. ABI depends on index stability, not function name)
- Accept only 0 or 1 _message_ as input and return only 0 (via _void_) or 1 message as output
- We rely on the message definitions for further ABI stability.

#### "Appinit" interface

Every project has an implicit interface: appinit.cci.  This interface is intended to define the "init" or constructor function for a given chaincode.  It is also generally assumed to be not something that needs to be shared with other projects in the same manner that application-level interfaces might, thus we are not concerned about "appinit.cci" name conflicting in the way we care about other interfaces.

The interface expected to define a message "Init" with no RPCs.  This message will be assumed to be the argument to the chaincode constructor.

# Protocol

_chaintool_ tunnels its protobuf-based protocol into the standard chaincode protocol.  This standard protocol consists of a single "function" string, and an array of string "args".  This protobuf schema for the standard chaincode protocol is:
```
message ChaincodeInput {

    string function = 1;
    repeated string args  = 2;

}
```

## Function Naming

Function naming follows the convention *interface-name/method-type/method-index*.  For instance, invoking *MakePayment* from our [example](./examples/example02/app/src/interfaces/org.hyperledger.chaincode.example02.cci) would be *org.hyperledger.chaintool.example02/txn/1*.  Because its transaction #1 in the org.hyperledger.chaintool.example02 interface.

### Method Types

There are two types of methods: transactions and queries.  We therefore have two values in the function name that correspond to the underlying method type:

- "txn" - transactions
- "query" - queries

## Data encoding

Every function takes 0 or 1 input message, and 0 (via "void") or 1 output message.  These messages are standard protobuf *Message* structures using base64 encoding.  Input parameters are passed via arg[0] of the standard chaincode protocol.

## Protobuf "hints"

The .proto file generated from *chaintool* (such as *chaintool proto*) contains hints to help developers understand the protocol.  For instance, see the comments at the bottom of this .proto generated from example02:

```
//
// Generated by chaintool.  DO NOT EDIT!!
//

syntax = "proto3";

package org.hyperledger.chaincode.example02;

message BalanceResult {
         int32 balance = 1;
}

message Entity {
         string id = 1;
}

message PaymentParams {
         string partySrc = 1;
         string partyDst = 2;
         int32 amount = 3;
}

//
// Available RPC functions exported by this interface
//
// void MakePayment(PaymentParams) -> org.hyperledger.chaincode.example02/txn/1
// void DeleteAccount(Entity) -> org.hyperledger.chaincode.example02/txn/2
// BalanceResult CheckBalance(Entity) -> org.hyperledger.chaincode.example02/query/1
```


# Platforms

The following is a list of the currently supported chaincode platforms
* [org.hyperledger.chaincode.golang](./documentation/platforms/golang/README.md)
