#!/bin/sh

export VERSION=$(git rev-parse --short "$GITHUB_SHA")
export IMAGE=ferlabcrsj/ferload:$VERSION
export LATEST_IMAGE=ferlabcrsj/ferload:latest
docker build -t $IMAGE .
docker tag $IMAGE $LATEST_IMAGE

docker push $LATEST_IMAGE
docker push $IMAGE
