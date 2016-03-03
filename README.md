[![Build Status](https://travis-ci.org/ghaskins/obcc.svg?branch=master)](https://travis-ci.org/ghaskins/obcc)

# obcc - openblockchain compiler (Work In Progress)

## Introduction

obcc is a propsal for a toolchain to assist in various phases of [openblockchain](https://github.com/openblockchain) chaincode development, such as compilation, test, packaging, and deployment.

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

The idiomatic way to use obcc is to treat it similar to other build tools such as Make, Maven, or Leiningen.  That is, by default it expects to be executed from within your project root.  Subcommands such as _build_, _clean_, and _package_ fall into this category.  You can run it outside of a project root by using the "-p" switch to these commands to inform OBCC where your project root is when it is not the current directory.

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

