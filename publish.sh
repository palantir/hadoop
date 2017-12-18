#!/bin/bash
set -euo pipefail
set -x

version=$(git describe --tags --always)
file_name="hadoop-dist-${version}.tgz"

# Deploy JARs to Bintray
mvn -Dmaven.repo.remote=file:///Users/srand/tmpMaven/releases -e versions:set -DnewVersion=$version
#mvn -Dmaven.repo.remote=file:///Users/srand/tmpMaven/releases -e versions:commit
mvn -Dmaven.repo.remote=file:///Users/srand/tmpMaven/releases -e -DskipTests deploy | grep -v 'Progress'
