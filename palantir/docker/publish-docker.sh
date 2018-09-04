#!/bin/bash
set -eu

IMAGE_NAME=palantirtechnologies/circle-hadoop-2

echo $PWD
cd palantir/docker
echo $PWD
docker build . -t ${IMAGE_NAME}
docker login -u "${DOCKERHUB_USERNAME}" -p "${DOCKERHUB_PASSWORD}"
docker push $IMAGE_NAME
