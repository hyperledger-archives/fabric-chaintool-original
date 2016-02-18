# obcc - openblockchain compiler (Work In Progress)

obcc is a propsal for a toolchain to assist in various phases of chaincode development, such as compilation, test, packaging, and deployment.

## Installation

   $ make install

## Usage

    $ obcc -h
    Usage: obcc [options] action
    
    Options:
      -p, --path PATH  ./  path to chaincode project
      -v, --version
      -h, --help
    
    Actions:
      build -> Build the chaincode project
      clean -> Clean the chaincode project
      package -> Package the chaincode for deployment


