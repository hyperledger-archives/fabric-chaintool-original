SRCS = $(shell find src -type f)

all: client

client: ./out/example02.js

node_modules: project.clj
	lein npm install

./out/example02.js: $(SRCS) node_modules
	lein cljsbuild once

clean:
	-@rm -rf ./out ||:
	-@rm -rf node_modules ||:
