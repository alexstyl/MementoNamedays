# Build script taken and altered from: https://github.com/firebase/quickstart-android/blob/master/build.sh
#!/bin/bash

# Exit on error
set -e

# Limit memory usage
OPTS='-Dorg.gradle.jvmargs="-Xmx2048m -XX:+HeapDumpOnOutOfMemoryError"'

# Work off travis
if [[ ! -z TRAVIS_PULL_REQUEST ]]; then
  echo "TRAVIS_PULL_REQUEST: $TRAVIS_PULL_REQUEST"
else
  echo "TRAVIS_PULL_REQUEST: unset, setting to false"
  TRAVIS_PULL_REQUEST=false
fi

echo "Building ${SAMPLE}"

# Copy mock google-services file if necessary
if [ ! -f ./app/google-services.json ]; then
  echo "Using mock google-services.json"
  cp mock-google-services.json ./mobile/src/free/google-services.json
  cp mock-google-services.json ./mobile/src/pro/google-services.json
fi

# Build
if [ $TRAVIS_PULL_REQUEST = false ] ; then
  # For a merged commit, build all configurations.
  GRADLE_OPTS=$OPTS ./gradlew clean build
else
  # On a pull request, just build debug which is much faster and catches
  # obvious errors.
  GRADLE_OPTS=$OPTS ./gradlew clean :mobile:assembleDebug
fi
