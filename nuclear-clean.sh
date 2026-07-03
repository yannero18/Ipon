#!/bin/bash
set -e

echo "======================================"
echo "NUCLEAR CLEAN AND REBUILD"
echo "======================================"

echo "Step 1: Remove all Gradle caches..."
rm -rf ~/.gradle
rm -rf .gradle
rm -rf app/.gradle

echo "Step 2: Remove all build artifacts..."
rm -rf build
rm -rf app/build
rm -rf */build

echo "Step 3: Remove all compiled outputs..."
find . -name "*.class" -delete
find . -name "*.dex" -delete

echo "Step 4: Clean with Gradle..."
./gradlew clean --no-build-cache

echo "Step 5: Fresh rebuild..."
./gradlew assembleDebug --no-build-cache

echo ""
echo "======================================"
echo "BUILD COMPLETE!"
echo "APK: app/build/outputs/apk/debug/app-debug.apk"
echo "======================================"