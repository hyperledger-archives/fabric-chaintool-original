
NAME=obcc
LEIN = $(shell which lein || echo ./lein)
BINDIR ?= /usr/bin
OUTPUT=target/$(NAME)

SRCS += $(shell find src/$(NAME) -name "*.clj")

all: $(OUTPUT)

$(OUTPUT): $(SRCS) Makefile
	@$(LEIN) bin

$(PREFIX)$(BINDIR):
	mkdir -p $@

install: $(OUTPUT) $(PREFIX)$(BINDIR)
	cp $(OUTPUT) $(PREFIX)$(BINDIR)

clean:
	@echo "Cleaning up.."
	-@rm -rf target
	-@rm -f *~

