To upgrade the base version to a new version of Apache Hadoop:

1. Check out the tag corresponding to the target release.
2. Modify the root `pom.xml` file to point to Bintray as the target repository.
3. Copy the `palantir` folder from the most recent release branch.
4. Make sure that `publish.sh` is executable, and update the version that it uses when it updates `$hadoop.version` via `sed`.
5. Apply any backports and/or reverts.
6. Update the changelog.
