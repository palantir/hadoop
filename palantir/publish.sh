#!/bin/bash
set -euo pipefail

get_release_type() {
  [ -n "${CIRCLE_TAG-}" ] && echo "release" || echo "snapshot"
}

version=$(git describe --tags --always | sed -e "s/^rel\/release-//")
artifact_name="hadoop-dist-${version}.tgz"

tmp_settings="tmp-settings.xml"
echo "<settings><servers><server>" > $tmp_settings
echo "<id>internal-palantir-repository</id><username>$ARTIFACTORY_USERNAME</username>" >> $tmp_settings
echo "<password>$ARTIFACTORY_PASSWORD</password>" >> $tmp_settings
echo "</server></servers></settings>" >> $tmp_settings

# Update the version.
echo "Updating version to match git tag"
mvn -e versions:set -DnewVersion="$version" | grep -v 'Progress'
echo $PWD
sed -i "s/<hadoop\.version>3\.2\.2/<hadoop\.version>$version/g" pom.xml

# Publish JARs for client-side use. Remove the layout when we upgrade the maven deploy plugin to 3.0+.
echo "Deploying JARs"
mvn -e -DaltDeploymentRepository=internal-palantir-repository::default::https://publish.artifactory.palantir.build/artifactory/internal-jar-fork-$(get_release_type) --settings $tmp_settings source:jar -DskipTests deploy | grep -v 'Progress'

# Publish the dist for server-side use
echo "Publishing dist"
mvn -e package -Pdist -DskipTests -Dmaven.javadoc.skip=true -Dtar | grep -v 'Progress' | grep -v 'longer than 100 characters'
curl -u $ARTIFACTORY_USERNAME:$ARTIFACTORY_PASSWORD -T hadoop-dist/target/hadoop-${version}.tar.gz "https://publish.artifactory.palantir.build/artifactory/internal-dist-fork-$(get_release_type)/org/apache/hadoop/hadoop-dist/${version}/${artifact_name}"
