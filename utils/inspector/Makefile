SRCS = $(shell find src -type f)
LEIN = $(shell which lein || echo ../../lein)

all: inspector

inspector: ./out/chaincode-inspector.js

node_modules: project.clj
	$(LEIN) npm install

./out/chaincode-inspector.js: $(SRCS) node_modules Makefile
	$(LEIN) cljsbuild once
	@echo "Compilation complete: use \"node $@ --help\" for execution instructions"

clean:
	-@rm -rf ./out ||:
	-@rm -rf node_modules ||:
