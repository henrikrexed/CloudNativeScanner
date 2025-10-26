#!/bin/bash

echo "🧪 Testing Scanner Components..."
echo "================================"

# Set Java environment
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

echo "📋 Java version:"
java -version

echo ""
echo "🔨 Compiling test classes..."
mvn compile test-compile -q

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful"
    
    echo ""
    echo "🧪 Running scanner tests..."
    
    # Run the standalone test
    java -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" TestScanner
    
    echo ""
    echo "🧪 Running JUnit tests..."
    
    # Run JUnit tests
    mvn test -Dtest=ScannerTest -q
    
    echo ""
    echo "✅ Scanner tests completed!"
    
else
    echo "❌ Compilation failed"
    exit 1
fi

