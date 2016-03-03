[![Build Status](https://travis-ci.org/ghaskins/obcc.svg?branch=master)](https://travis-ci.org/ghaskins/obcc)

# obcc - openblockchain compiler (Work In Progress)

## Introduction

obcc is a propsal for a toolchain to assist in various phases of [openblockchain](https://github.com/openblockchain) chaincode development, such as compilation, test, packaging, and deployment.

### Why?

Current chaincode development is rather unstructured.  While some provisions to support various chaincode languages are present, only golang based chaincode is actually supported today.  And within the golang environment, there isn't much structure to an application outside of the coarse-level general callbacks for invoke or query.  Applications are left to manually decode a {function-name, argument-array} string-based tuple.  This means that a user needs to study the chaincode source in order to ascertain its API and hope that the API doesn't change over time in an incongruent manner.  

Consider that some applications may employ OBC confidentiallity to hide their source code.  Also consider that chaincode processes may potentially be long-running.  It starts to become clear that there are some advantages to allowing chaincode to express their API interfaces in a way that is independent from the underlying implementation and supports schema management for forwards/backwards compatiblity.

OBCC helps in this regard by allowing applications to declare/consume one or more language neutral interface-definitions and package it with the project.  It also helps the developer by generating shim/stub code in their chosen programming language that helps them implement and/or consume the interfaces declared.  This means that external parties may learn about the interface(s) supported by a given endpoint in a language neutral manner without requiring access to the underlying code.  It also means that we can use [protobufs](https://developers.google.com/protocol-buffers/) to help with various API features such as managing forwards/backwards compatiblity in a transparent way, endian neutrality, basic type validation, etc.

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
### Project Structure

Like many modern build tools, OBCC is opinionated.  It expects a specific structure to your project as follows:

- a file [chaincode.conf](./testdata/example02/chaincode.conf) is in the root directory (discussed below)
- your chaincode entry-point in ./src/chaincode ([example](./testdata/example02/src/chaincode/chaincode_example02.go))
- your interface files are in ./src/interfaces ([example](./testdata/example02/src/interfaces/com.obc.chaincode.example02.cci))
- your project defines one project-level interface called ./src/interfaces/project.cci ([example](./testdata/example02/src/interfaces/project.cci))

#### chaincode.conf

_chaincode.conf_ is the central configuration file for a given obcc-managed chaincode project.  An example looks like this:

```
# ----------------------------------
# chaincode example02
# ----------------------------------
#
# Copyright (C) 2016 - OpenBlockChain
# All rights reserved
#

Schema:  1
Name:    com.obc.chaincode.example02  # trailing comment test
Version: 0.1-SNAPSHOT

Platform
{
        Name: com.obc.chaincode.golang
        Version: 1
}

Provides: [self] # 'self' is a keyword that means there should be $name.cci (e.g. com.obc.chaincode.example02.cci)
```

All chaincode.conf files may have an arbitrary number of comments (via "#") or whitespace which are summarily ignored.  Aside from whitespace, the real content of your file must start with "Schema: 1" to denote compatibility.  After this, there are 3 primary groups of fields:

- project name/version
- platform
- interface declarations (provides/consumes)

##### Project name/version

This is something that should uniquely identify your chaincode project for human/UX consumption.  It is generally advised that a DNS name of some kind be incorporated to promote global uniqueness.  Note that the OBC subsystem in general does not interpret these names in any meaningful way other than for display purposes.

##### Platform

It is here that a chaincode may declare the compatibility/conformity to a specific platform.  The idea is to promote extensibility (e.g. other platforms may be added in the future) and also compatility (e.g. platform X, version Y may mean something very specifically about the type of chaincode language supported, the ABI for any peripheral libraries, etc).  It is analogous to the notion that java 1.7 is a different ABI than java 1.8, etc.  At the time of writing, the only supported platform is com.obc.chaincode.golang version 1.  More platforms may be added in the future.

###### Adding platforms

The only core requirement is that both OBCC and the chosen OBC network are in agreement to support said platform.  The details of implementing this are "coming soon".

##### Interface Declarations

Interfaces (as included in ./src/interfaces) may be in one or two categories: Provided or Consumed.  _Provided_ means that the chaincode implements the interface and supports having clients or other chaincode invoke methods as declared.  Likewise, _consumed_ indicates that the chaincode expects to perform inter-chaincode invoke/query operations to a disparate chaincode instance that provides the interface.  It is perfectly fine (though perhaps uncommon) for a chaincode to both provide and consume a given interface (such as for proxy contracts which may accept operations in a polymorhphic manner before passing operations on to a concrete instance).

Both Provides and Consumes are expressed as an array of 1 or more entries.  For example:

```
Provides: [com.obc.chaincode.example02, com.obc.chaincode.example03]
Consumes: [com.obc.chaincode.example02]
```

If there aren't any interfaces in a particular category, the entry may be omitted.  Note that a chaincode that doesnt provide any interfaces doesn't sound particularly useful, however.

###### "self"

The keyword _self_ may be used as shorthand for an interface that shares the same name as the project (for instance, the com.obc.chaincode.example02 project surfacing the com.obc.chaincode.example02.cci interface), as a convenience.  It is idiomatic for a project to name its primary interfaces after itself, and therefore this shortcut is expected to be commonly used.  Example:

```
Provides: [self]
```

#### Chaincode

The opinionated portion of chaincode path largely applies to solely the entry-point for your application.  Other paths for non-entry point code are generally fine if you are using a language that supports namespaces, etc.  For instance, the com.obc.chaincode.golang platform assumes a $GOPATH of ./src and tries to build "chaincode" (via $GOPATH/src/chaincode).  However, if your chaincode uses go imports such as:

```golang
import (
   "foo"
   "bar/baz"
)
```

placed in ./src/foo and ./src/bar/baz respectively, they will be discovered perfectly fine. 

#### Interfaces

An interface is a file ending in .cci (Chaincode Interface) that defines a language neutral definition for various RPC-like functions that a given chaincode instance supports.  An chaincode instance may in fact support many different interfaces at one time.  This is convenient for creating a type of polymorphism within a network of chaincode instances.

Each .cci file is meant to represent an interface contract for compatibility.  Items declared within a .cci file have provisions (similar to protobuf indices) for mutating structures over time that do not break forwards or backwards compatbility.  Changes to a given interface should only be done in a manner which exploits this compatiblity mechanism.  If for some reason it is mandated that compatility _must_ be broken, the name of the interface should be changed.

##### Interface names

The name of the .cci file has direct meaning to the ABI: the name of the file will be translated into ABI tokens on the wire.  This was intentionally chosen so that the filesystem itself (under ./src/interfaces) takes a role in ensuring that only one interface of a particular type is in use within a project at any given time.  Likewise, if a project wishes to import and consume an interface from a different project, it is imperative that the filename be retained across both projects or the endpoints will be inadvertently namespace-isolated from one another.  To put it another way, do not rename .cci files on import!

Perhaps even more importantly, interface ABI needs to be globally managed.  Therefore it is advised to name .cci files in a way that is globally unique.  A UUID would suffice, at the expense of being somewhat difficult to humans to deal with.  Therefore, it is advised to name interfaces using DNS names as in the examples provided here.

##### Interface namespaces

Given the potential for multiple interfaces to use conflicting names, there is a need to let a project place specific interfaces in a unique namespace when necessary.  However, it was felt that the default mode should make it as easy as possible for a chaincode developer to work with the system.  Therefore, the default import of an abitrary .cci file will emit tokens in the global namespace for ease of use.  A future mechanism will be added to the chaincode.conf that will allow a developer to assign arbitrary interfaces to specific namespaces w.r.t. emitted tokens without affecting the wire ABI.  TBD.  For now, note that tokens declared within interfaces files within the same project may not conflict.
