#!/bin/bash
set -eu

IMAGE_NAME=palantirtechnologies/circle-hadoop

echo $PWD
cd palantir/docker
echo $PWD
docker build . -t ${IMAGE_NAME}
docker tag $IMAGE_NAME hub.docker.com/$IMAGE_NAME:hadoop-2
docker login -u "${DOCKERHUB_USERNAME}" -p "${DOCKERHUB_PASSWORD}"
docker push $IMAGE_NAME
