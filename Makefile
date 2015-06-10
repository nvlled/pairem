BUILD_DIR=classes

compile: ${BUILD_DIR}
	javac -cp ${BUILD_DIR} -Xlint -d ${BUILD_DIR} src/nvlled/emit/*.java
	javac -cp ${BUILD_DIR} -Xlint -d ${BUILD_DIR} src/nvlled/memgame/*.java

run:
	java -cp ${BUILD_DIR} nvlled.memgame.Main

${BUILD_DIR}:
	mkdir -p ${BUILD_DIR}
