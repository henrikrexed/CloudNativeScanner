#!/bin/bash

echo "🧪 Running Standalone Scanner Test..."
echo "===================================="

# Set Java environment
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

echo "📋 Java version:"
java -version

echo ""
echo "🔨 Compiling classes..."
mvn compile -q

# Compile the standalone test file
echo "🔨 Compiling standalone test..."
javac -cp "target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" TestScanner.java

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful"
    
    echo ""
    echo "🧪 Running standalone test..."
    
    # Get classpath
    CLASSPATH=$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)
    CLASSPATH=".:target/classes:$CLASSPATH"
    
    # Run the standalone test
    java -cp "$CLASSPATH" TestScanner
    
    echo ""
    echo "✅ Standalone test completed!"
    
else
    echo "❌ Compilation failed"
    exit 1
fi
