#!/bin/bash
# Clean build script for Ipon

echo "Cleaning Gradle cache..."
./gradlew clean

echo "Cleaning build directories..."
rm -rf app/build
rm -rf build
rm -rf .gradle

echo "Running debug build..."
./gradlew assembleDebug

echo "Build complete! APK location: app/build/outputs/apk/debug/app-debug.apk"
