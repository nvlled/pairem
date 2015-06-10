BUILD_DIR=classes

compile: ${BUILD_DIR}
	javac -d ${BUILD_DIR} src/*.java

run:
	java -cp ${BUILD_DIR} Main

${BUILD_DIR}:
	mkdir -p ${BUILD_DIR}
