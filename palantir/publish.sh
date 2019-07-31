#!/bin/bash
set -euo pipefail

version=$(git describe --tags --always)
file_name="hadoop-dist-${version}.tgz"

tmp_settings="tmp-settings.xml"
echo "<settings><servers><server>" > $tmp_settings
echo "<id>bintray-palantir-release</id><username>$BINTRAY_USERNAME</username>" >> $tmp_settings
echo "<password>$BINTRAY_PASSWORD</password>" >> $tmp_settings
echo "</server></servers></settings>" >> $tmp_settings

# Update the version.
echo "Updating version to match git tag"
mvn -e versions:set -DnewVersion="$version" | grep -v 'Progress'
echo $PWD
sed -i "s/<hadoop\.version>3\.2\.0/<hadoop\.version>$version/g" pom.xml

# Deploy JARs to Bintray
echo "Deploying JARs"
mvn -e --settings $tmp_settings source:jar -DskipTests deploy -Pnative | grep -v 'Progress'

# Publish a dist to Bintray
echo "Publishing dist"
mvn -e package -Pdist,native,src,yarn-ui -DskipTests -Dmaven.javadoc.skip=true -Dtar | grep -v 'Progress' | grep -v 'longer than 100 characters'
curl -u $BINTRAY_USERNAME:$BINTRAY_PASSWORD -T hadoop-dist/target/hadoop-${version}.tar.gz "https://api.bintray.com/content/palantir/releases/hadoop/${version}/org/apache/hadoop/hadoop-dist/${version}/${file_name}"

# Tell Bintray to publish the artifacts for this release
curl -u $BINTRAY_USERNAME:$BINTRAY_PASSWORD -X POST https://api.bintray.com/content/palantir/releases/hadoop/$(git describe --tags)/publish
