#!/bin/bash
set -eu

IMAGE_NAME=palantirtechnologies/circle-hadoop-2

docker build . -t ${IMAGE_NAME}
docker login -u "${DOCKERHUB_USERNAME}" -p "${DOCKERHUB_PASSWORD}"
docker push $IMAGE_NAME
