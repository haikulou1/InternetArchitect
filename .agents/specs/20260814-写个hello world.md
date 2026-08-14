# Hello World Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a minimal standalone Java "Hello World" module as an introductory example for learners.

**Architecture:** A single Java class with a `main()` method, placed in a dedicated `00 HelloWorld/` directory at the repository root. No framework dependencies — pure Java SE.

**Tech Stack:** Java (any version that supports `javac`/`java`), POSIX shell (for build script).

---

## Global Constraints

- Java source file must compile with `javac` (no external build tool required)
- Output must be exactly `Hello, World!` to stdout
- Follow the design from `.agents/specs/dima.md` — Approach A: Standalone Java Hello World
- All files created under `00 HelloWorld/` directory at repo root

---

## Task 1: Hello World Source & Build Script

**Files:**
- Create: `00 HelloWorld/src/HelloWorld.java`
- Create: `00 HelloWorld/README.md`
- Create: `00 HelloWorld/build.sh`

**Interfaces:**
- Consumes: Nothing (first task)
- Produces: Runnable `HelloWorld.class` (compiled artifact), build instructions for learners

- [ ] **Step 1: Create `00 HelloWorld/src/HelloWorld.java`**

```java
package hello;

/**
 * A minimal Hello World application.
 * Compile: javac src/HelloWorld.java -d .
 * Run:     java hello.HelloWorld
 */
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
```

- [ ] **Step 2: Create `00 HelloWorld/build.sh`**

```bash
#!/bin/sh
# Build and run script for Hello World
set -e

echo "=== Compiling HelloWorld ==="
mkdir -p out
javac src/HelloWorld.java -d out

echo ""
echo "=== Running HelloWorld ==="
java -cp out hello.HelloWorld
```

- [ ] **Step 3: Make build script executable**

```bash
chmod +x 00 HelloWorld/build.sh
```

- [ ] **Step 4: Create `00 HelloWorld/README.md`**

```markdown
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
```

- [ ] **Step 5: Verify the program compiles and runs**

```bash
cd "00 HelloWorld"
javac src/HelloWorld.java -d out
java -cp out hello.HelloWorld
```

Expected output:
```
Hello, World!
```

- [ ] **Step 6: Commit**

```bash
git add "00 HelloWorld/"
git commit -m "feat: add standalone Hello World Java module"
```

---

## Self-Review Checklist

1. **Spec coverage:** The plan creates a standalone Java Hello World (Approach A from dima.md) with `HelloWorld.java`, `README.md`, and `build.sh`. All requirements covered.
2. **Placeholder scan:** No TBD, TODO, or placeholder code. Every step has complete code or commands.
3. **Type consistency:** Single file, no cross-task type dependencies to mismatch.
4. **DRY/YAGNI:** No unnecessary abstractions, no framework dependencies, minimal files.