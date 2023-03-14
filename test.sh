#! /bin/bash
EMU_FILE=./build/emu

if [[ -z $1 ]]; then
	echo "Usage: ${0} <test_folders>"
	exit 1
fi

if [[ ! -x ${EMU_FILE} ]]; then
	make emu || exit 1
fi

for FOLDER in $@
do
	BIN_FILES=`eval "find $FOLDER -mindepth 1 -maxdepth 2 -regex \".*\.\(bin\)\""`
	for BIN_FILE in $BIN_FILES; do
		FILE_NAME=`basename ${BIN_FILE%.*}`
		printf "[%20s] " $FILE_NAME
		./$EMU_FILE $BIN_FILE 
	done
done
