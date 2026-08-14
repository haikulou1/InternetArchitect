#!/bin/sh
# Build and run script for Hello World
set -e

echo "=== Compiling HelloWorld ==="
mkdir -p out
javac src/HelloWorld.java -d out

echo ""
echo "=== Running HelloWorld ==="
java -cp out hello.HelloWorld