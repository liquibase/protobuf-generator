SHELL=/bin/bash
.PHONY: build generate package

build:
	mkdir -p $(PWD)/liquibase_libs
	mvn clean package
	cp $(PWD)/target/liquibase-protobuf-$(VERSION).jar $(PWD)/liquibase_libs/

generate: build
	mkdir -p $(PWD)/proto
	liquibase generateProtobuf --outputDir $(PWD)/proto

package: generate
	mkdir -p $(PWD)/releases
	cd $(PWD)/proto && zip liquibase-protobuf-$(VERSION).zip *
	mv $(PWD)/proto/liquibase-protobuf-$(VERSION).zip $(PWD)/releases

clean:
	rm $(PWD)/liquibase_libs/liquibase-protobuf-*
	rm -Rf $(PWD)/proto