.PHONY: all test dependencies serve clean clean-all
.FORCE:

FRAMEWORK_FILES = $(shell find framework -type f | sed 's/ /\\ /g')
LAB_FILES = $(shell find lab -type f | sed 's/ /\\ /g')
HANDOUT_FILES = $(shell find handout -type f | sed 's/ /\\ /g')

JAR_FILES = build/libs/framework.jar \
						build/libs/grader.jar \
						build/libs/framework-sources.jar \
						build/libs/grader-sources.jar \
						build/libs/framework-deps.jar \
						build/libs/grader-deps.jar \
						build/libs/framework-deps-sources.jar
OTHER_FILES = build/doc/ lombok.config


ifeq ($(shell uname -s),Darwin)
	TAR = gtar
else
	TAR = tar
endif

ifeq ($(shell uname -s),Darwin)
	CP = gcp
else
	CP = cp
endif


all: build/handout/

dependencies:
	./gradlew copyDependencies

build/libs/: $(FRAMEWORK_FILES)
	./gradlew assemble
	touch $@

build/doc/: $(FRAMEWORK_FILES)
	./gradlew javadoc
	touch $@

build/handout/: $(LAB_FILES) $(HANDOUT_FILES) $(OTHER_FILES) build/libs/
	rm -rf $@
	mkdir $@ build/handout/jars
	$(CP) -r lab handout/. $(OTHER_FILES) $@
	$(CP) $(JAR_FILES) build/handout/jars

build/handout.tar.gz: build/handout/
	$(TAR) -czf $@ --transform "s/^build\/handout/dslabs/" $^

test:
	./gradlew test

www/javadoc/: build/doc/
	rsync -a --delete $< $@

build/www/: www/javadoc/ .FORCE
	mkdir -p $@
	cd ./www;	bundle exec jekyll build -d ../$@
	touch $@

serve: www/javadoc/
	cd www; watchy -w _config.yml -- bundle exec jekyll serve --watch -d ../build/www/

clean:
	rm -rf build www/javadoc www/.jekyll-cache

clean-all: clean
	rm -rf deps .gradle traces
