NAME=hlcc
LEIN = $(shell which lein || echo ./lein)
BINDIR ?= /usr/bin
OUTPUT=target/$(NAME)

SRCS += $(shell find src -type f)
SRCS += $(shell find resources -type f)

all: $(OUTPUT)

$(OUTPUT): $(SRCS) Makefile
	@$(LEIN) bin

$(PREFIX)$(BINDIR):
	mkdir -p $@

proto:
	protoc --java_out=./src resources/proto/*.proto

install: $(OUTPUT) $(PREFIX)$(BINDIR)
	cp $(OUTPUT) $(PREFIX)$(BINDIR)

clean:
	@echo "Cleaning up.."
	-@rm -rf target
	-@rm -f *~
