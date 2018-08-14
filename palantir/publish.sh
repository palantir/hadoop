#!/bin/bash
set -euo pipefail

# Circle doesn't set this for us, but our mvn package step needs it due to its use of ${env.JAVA_HOME}.
export JAVA_HOME=/usr/lib/jvm/jdk1.8.0

version=$(git describe --tags --always)
file_name="hadoop-dist-${version}.tgz"

tmp_settings="tmp-settings.xml"
echo "<settings><servers><server>" > $tmp_settings
echo "<id>bintray-palantir-release</id><username>$BINTRAY_USERNAME</username>" >> $tmp_settings
echo "<password>$BINTRAY_PASSWORD</password>" >> $tmp_settings
echo "</server></servers></settings>" >> $tmp_settings

# Deploy JARs to Bintray
mvn -e versions:set -DnewVersion="$version"
mvn -e --settings $tmp_settings source:jar -DskipTests deploy

# Publish a dist to Bintray
mvn -e package -Pdist,native,src -DskipTests -Dmaven.javadoc.skip=true -Dtar | grep -v 'longer than 100'
curl -u $BINTRAY_USERNAME:$BINTRAY_PASSWORD -T hadoop-dist/target/hadoop-${version}.tar.gz "https://api.bintray.com/content/palantir/releases/hadoop/${version}/org/apache/hadoop/hadoop-dist/${version}/${file_name}"

# Tell Bintray to publish the artifacts for this release
curl -u $BINTRAY_USERNAME:$BINTRAY_PASSWORD -X POST https://api.bintray.com/content/palantir/releases/hadoop/$(git describe --tags)/publish
