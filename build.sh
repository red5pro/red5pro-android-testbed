i#!bin/bash

 rm -rf dist
 docker build --platform=linux/amd64 --progress=plain \
   --build-arg LICENSE_KEY="XXXX-XXXX-XXXX-XXXX" \
   --build-arg SM_ENDPOINT="https://todd.cloud.red5.net" \
   --build-arg STANDALONE_ENDPOINT="https://todd-oci.red5pro.net" \
   --build-arg LIB_PATH="dist_red5pro/red5-android-sdk-*-qa-release.aar"
   -t android-testbed-builder .
 docker create --name testbed-out android-testbed-builder
 docker cp testbed-out:/workspace/dist ./dist
 docker rm testbed-out

 #docker run --platform=linux/amd64 --rm -v $(pwd):/workspace -v $(pwd)/dist:/workspace/dist android-testbed-builder

 # Interactive
 #docker run -it --rm -v $(pwd):/workspace android-testbed-builder bash