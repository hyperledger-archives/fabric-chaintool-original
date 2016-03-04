[![Build Status](https://travis-ci.org/ghaskins/obcc.svg?branch=master)](https://travis-ci.org/ghaskins/obcc)

# obcc - openblockchain compiler (Work In Progress)

## Introduction

obcc is a proposal for a toolchain to assist in various phases of [openblockchain](https://github.com/openblockchain) chaincode development, such as compilation, test, packaging, and deployment.

### Why?

Current chaincode development is rather unstructured.  While some provisions to support various chaincode languages are present, only golang based chaincode is actually supported today.  And within the golang environment, there isn't much structure to an application outside of the coarse-level general callbacks for invoke or query.  Applications are left to manually decode a {function-name, argument-array} string-based tuple.  This means that a user needs to study the chaincode source in order to ascertain its API and hope that the API doesn't change over time in an incongruent manner. It also means that field translation/tranform/validation are manual and explicit processes in each chaincode function. 

Consider that some applications may employ OBC confidentiallity to hide their source code.  Also consider that chaincode processes may potentially be long-running and the API may evolve.  It starts to become clear that there are some advantages to allowing chaincode to express their API interfaces in a way that is independent from the underlying implementation and supports schema management for forwards/backwards compatiblity.

OBCC helps in this regard by allowing applications to declare/consume one or more language neutral interface-definitions and package it with the project.  It also helps the developer by generating shim/stub code in their chosen programming language that helps them implement and/or consume the interfaces declared.  This means that external parties may introspect a given instance for its interface(s) in a language neutral manner without requiring access to and/or an ability to decipher the underlying code.  It also means that we can use [protobufs](https://developers.google.com/protocol-buffers/) to help with various API features such as managing forwards/backwards compatiblity, endian neutrality, basic type validation, etc in a largely transparent manner.

OBCC provides some other benefits too, such as consistent language-neutral packaging and chaincode hashing, which help to simplify both the obc-peer implementation and developer burden.

## Getting Started

### Prerequisites

- A Java JRE/JDK v1.7 (or higher)

### Installation

   $ make install

### Usage

```
$ obcc -h
obcc version: v0.4-SNAPSHOT

Usage: obcc [general-options] action [action-options]

General Options:
  -v, --version  Print the version and exit
  -h, --help

Actions:
  build -> Build the chaincode project
  buildcca -> Build the chaincode project from a CCA file
  clean -> Clean the chaincode project
  package -> Package the chaincode into a CCA file for deployment
  unpack -> Unpackage a CCA file
  lscca -> List the contents of a CCA file

(run "obcc <action> -h" for action specific help)
```
### Working with OBCC

The idiomatic way to use obcc is to treat it similar to other build tools such as Make, Maven, or Leiningen.  That is, by default it expects to be executed from within your [project root](./PROJECTSTRUCTURE.md).  Subcommands such as _build_, _clean_, and _package_ fall into this category.  You can run it outside of a project root by using the "-p" switch to these commands to inform OBCC where your project root is when it is not the current directory.

Other commands such as _buildcca_, _unpack_, and _lscca_ are designed to operate against a Chaincode Archive (CCA) from a previous _package_ operation.  These commands expect a path to a CCA file.

In all cases, you may obtain subcommand specific help by invoking "obcc _$subcommand_ -h".  For example:

```
$ obcc  package -h
obcc version: v0.4-SNAPSHOT

Description: obcc package - Package the chaincode into a CCA file for deployment

Usage: obcc package [options]

Command Options:
  -o, --output NAME          path to the output destination
  -c, --compress NAME  gzip  compression algorithm to use
  -p, --path PATH      ./    path to chaincode project
  -h, --help
```
Please see [this guide](./PROJECTSTRUCTURE.md) for details of the project structure that OBCC expects

### Typical Workflow

- edit -> obcc build -> repeat until satisified
- obcc package -> generates a .cca file for upload
- obc-peer chaincode deploy -lcca path/to/file.cca

### Subcommand Details

#### obcc build

Builds your chaincode project into a binary ready for execution on a blockchain.  Various artifacts are emitted to ./build, depending on the platform.  For com.obc.chaincode.golang:

- ./build/src: shim, protobufs, etc
- ./build/deps: direct and transitive dependencies of your chaincode, as retrieved by "go get".  NOTE: this option is likely to default to disabled in the future, since it is not a good idea for a validating peer to be pulling dependenices down.  Rather, there should be some fixed number of dependencies that are implicitly included with the platform.  For now, we pull things in dynamically.
- ./build/bin: the default location for the binary generated (override with -o)

#### obcc clean

Cleans a chaincode project.  This typically translates to removing the ./build directory, but platforms are free to define this as they see fit and may perform additional or alternative operations.

#### obcc package

Packages a project into a .cca file suitable for deployment.  Implicitly runs the "lscca" command on the result to display details about the package.

```
vagrant@obc-devenv:v0.0.7-2ec7137:~ $ obcc package -p /opt/gopath/src/github.com/openblockchain/obc-peer/openchain/example/chaincode/cca/example02/
Writing CCA to: /opt/gopath/src/github.com/openblockchain/obc-peer/openchain/example/chaincode/cca/example02/build/com.obc.chaincode.example02-0.1-SNAPSHOT.cca
Using path /opt/gopath/src/github.com/openblockchain/obc-peer/openchain/example/chaincode/cca/example02/ ["src" "chaincode.conf"]
|------+------------------------------------------+------------------------------------------------|
| Size |                   SHA1                   |                      Path                      |
|------+------------------------------------------+------------------------------------------------|
| 456  | fc4ad46e08416ffde454b7dfeeba4270a075ef7f | chaincode.conf                                 |
| 3630 | 55d1754539a3c25033faae53b00022494c70939b | src/chaincode/chaincode_example02.go           |
| 375  | 9492a1e96f380a97bba1f16f085fc70140154c65 | src/interfaces/com.obc.chaincode.example02.cci |
| 143  | 7305f65e18e4aab860b201d40916bb7adf97544f | src/interfaces/project.cci                     |
|------+------------------------------------------+------------------------------------------------|
Platform:            com.obc.chaincode.golang version 1
Digital Signature:   none
Raw Data Size:       4604 bytes
Archive Size:        2315 bytes
Compression Alg:     gzip
Chaincode SHA3:      98493521a198d0a93cc07c4451616efebbe96eb2655dd38357c4e0c495bcae3ff3de14bc2287c70bc6c67968669871dc32164bb48e9765c97fb843a2cd7a0f78
```

#### obcc lscca

Displays the contents of an existing .cca file (see 'obcc package' for an output example)

#### obcc unpack

Unpacks a .cca archive into the filesystem as a chaincode project.

#### obcc buildcca

Combines _unpack_ with _build_ by utilizing a temporary directory.  This allows a project to be built from a .cca file without explicitly unpacking it first, as a convenience.

