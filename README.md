# DataSonnet

![Maven Central](https://img.shields.io/maven-central/v/com.datasonnet/datasonnet-mapper)

## Build and Versioning notes

The version in the POM should always be a SNAPSHOT version. That is the version that will be published
on every push to the main branch (currently `master`).

All other branches will be versioned by the current POM version (without `-SNAPSHOT`) followed by their name (with slashes replaced by hyphens) followed by `-SNAPSHOT`.

Builds triggered on individual commits will have the version `${POM VERSION}-commit-{HASH}-SNAPSHOT`.

Tags that start with `v` will be published with whatever the exact tag is (without the v).

To make a release where the POM SNAPSHOT version is `X.Y.Z-SNAPSHOT`
    - tag the commit being released with `vX.Y.Z` and push.
    - update the POM version to the next SNAPSHOT release by ticking one of the version numbers and make a PR into the main branch.

If a build fails, make a new push, which will trigger the new build with the necessary version information. If a
tagged build fails, tick the patch version into a new tag and push that to trigger the new build.
