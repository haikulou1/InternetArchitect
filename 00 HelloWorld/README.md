# Hello World

A minimal Java Hello World example, designed as the first program for learners.

## Prerequisites

- Java Development Kit (JDK) — any version that provides `javac` and `java`

## Build & Run

### Option A: Using the build script

```bash
cd "00 HelloWorld"
./build.sh
```

### Option B: Manual steps

```bash
cd "00 HelloWorld"
mkdir -p out
javac src/HelloWorld.java -d out
java -cp out hello.HelloWorld
```

## Expected Output

```
Hello, World!
```

## Learning Objectives

- Understand the basic structure of a Java class
- Learn how to compile (`javac`) and run (`java`) a Java program
- Understand the `main()` method as the program entry point
- Learn about Java package structure (`hello` package)