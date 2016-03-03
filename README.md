[![Build Status](https://travis-ci.org/ghaskins/obcc.svg?branch=master)](https://travis-ci.org/ghaskins/obcc)

# obcc - openblockchain compiler (Work In Progress)

## Introduction

obcc is a propsal for a toolchain to assist in various phases of [openblockchain](https://github.com/openblockchain) chaincode development, such as compilation, test, packaging, and deployment.

## Why?

Current chaincode development is rather unstructured.  While some provisions to support various chaincode languages are present, only golang based chaincode is actually supported today.  And within the golang environment, there isn't much structure to an application outside of the coarse-level general callbacks for invoke or query.  Applications are left to manually decode a {function-name, argument-array} string-based tuple.  This means that a user needs to study the chaincode source in order to ascertain its API and hope that the API doesn't change over time in an incongruent manner.  

Consider that some applications may employ OBC confidentiallity to hide their source code.  Also consider that chaincode processes may potentially be long-running.  It starts to become clear that there are some advantages to allowing chaincode to express their API interfaces in a way that is independent from the underlying implementation and supports schema management for forwards/backwards compatiblity.

OBCC helps in this regard by allowing applications to declare/consume one or more language neutral interface-definitions and package it with the project.  It also helps the developer by generating shim/stub code in their chosen programming language that helps them implement and/or consume the interfaces declared.  This means that external parties may learn about the interface(s) supported by a given endpoint in a language neutral manner without requiring access to the underlying code.  It also means that we can use [protobufs](https://developers.google.com/protocol-buffers/) to help with various API features such as managing forwards/backwards compatiblity in a transparent way, endian neutrality, basic type validation, etc.

OBCC provides some other benefits too, such as consistent language-neutral packaging and chaincode hashing, which help to simplify both the obc-peer implementation and developer burden.

## Getting Started

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
### Project Structure

Like many modern build tools, OBCC is opinionated.  It expects a specific structure to your project as follows:

- a file [chaincode.conf](./testdata/example02/chaincode.conf) is in the root directory (discussed below)
- your chaincode files are in ./src/chaincode ([example](./testdata/example02/src/chaincode/chaincode_example02.go))
- your interface files are in ./src/interfaces ([example](./testdata/example02/src/interfaces/com.obc.chaincode.example02.cci))
- your project defines one project-level interface called ./src/interfaces/project.cci ([example](./testdata/example02/src/interfaces/project.cci))


