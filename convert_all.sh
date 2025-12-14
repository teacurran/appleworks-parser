#!/bin/bash
#
# Convert all CWK test files to ODT format
#
# Usage: ./convert_all.sh
#

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INPUT_DIR="$SCRIPT_DIR/test_files"
OUTPUT_DIR="$SCRIPT_DIR/test_files_output"
PARSER_DIR="$SCRIPT_DIR/parser"

# Build the parser first
echo "Building parser..."
cd "$PARSER_DIR"
mvn compile -q
if [ $? -ne 0 ]; then
    echo "Build failed"
    exit 1
fi
echo "Build successful"
echo ""

# Run the batch converter
mvn exec:java -Dexec.mainClass="com.wirelust.appleworks.BatchConverter" \
    -Dexec.args="\"$INPUT_DIR\" \"$OUTPUT_DIR\"" -q
