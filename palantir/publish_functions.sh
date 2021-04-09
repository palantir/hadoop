#!/bin/bash
set -euo pipefail

get_version() {
  git describe --tags --always | sed -e "s/^rel\/release-//"
}

get_release_type() {
  [ -n "${CIRCLE_TAG-}" ] && echo "release" || echo "snapshot"
}

set_versions() {
  version=$(get_version)
  mvn -e versions:set -DnewVersion="$version" | grep -v 'Progress' | grep -v 'Downloaded' | grep -v 'Downloading'
  sed -i "s/<hadoop\.version>3\.2\.2/<hadoop\.version>$version/g" pom.xml
}

set_version_and_package() {
  # Create a local maven deploy such that publish can grab it and sync it with remote repository afterwards
  mkdir local_deploy
  set_versions
  mvn -e -DaltDeploymentRepository=staging-repository::default::file://local_deploy -DskipTests deploy | grep -v 'Progress' | grep -v 'Downloaded' | grep -v 'Downloading'
}

publish_artifacts() {
  version=$(get_version)
  artifact_name="hadoop-dist-${version}.tgz"

  tmp_settings="tmp-settings.xml"
  echo "<settings><servers><server>" > $tmp_settings
  echo "<id>internal-palantir-repository</id><username>$ARTIFACTORY_USERNAME</username>" >> $tmp_settings
  echo "<password>$ARTIFACTORY_PASSWORD</password>" >> $tmp_settings
  echo "</server></servers></settings>" >> $tmp_settings

  # Publish JARs for client-side use.
  echo "Deploying JARs"
  mvn -e org.codehaus.mojo:wagon-maven-plugin:2.0.2:merge-maven-repos \
      -Dwagon.targetId=internal-palantir-repository \
      -Dwagon.source=file://local_deploy \
      -Dwagon.target=https://publish.artifactory.palantir.build/artifactory/internal-jar-fork-$(get_release_type) \
      --settings $tmp_settings | grep -v 'Downloaded' | grep -v 'Downloading'

  # Publish the dist for server-side use
  # TODO (srand) This is inefficient -- it's not reusing the work from the build-maven job, and probably we should build the dist before the publish step instead of during it.
  echo "Publishing dist"
  set_versions
  mvn -e package -Pdist -DskipTests -Dmaven.javadoc.skip=true -Dtar | grep -v 'Progress' | grep -v 'Downloaded' | grep -v 'Downloading'
  curl -u $ARTIFACTORY_USERNAME:$ARTIFACTORY_PASSWORD -T hadoop-dist/target/hadoop-${version}.tar.gz "https://publish.artifactory.palantir.build/artifactory/internal-dist-fork-$(get_release_type)/org/apache/hadoop/hadoop-dist/${version}/${artifact_name}"
}
