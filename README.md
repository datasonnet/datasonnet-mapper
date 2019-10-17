# DataSonnet

## Usage

DataSonnet Mapper is available as a Maven dependency. To include it in your project, use one of the following snippets:

Maven:
```
<dependency>
    <groupId>com.datasonnet</groupId>
    <artifactId>datasonnet-mapper</artifactId>
    <version>1.0.1-SNAPSHOT</version>
</dependency>
```

Gradle:
```
compile group: 'com.datasonnet', name: 'datasonnet-mapper', version: '1.0.1-SNAPSHOT'
```

To use the mapper in your Java project, import the `com.datasonnet.Mapper` class, instantiate it and invoke the `transform` method:

```
import com.datasonnet.Mapper;
import com.datasonnet.StringDocument;
...

String jsonData = ...;
String dataSonnetMappingScript = ...;

Mapper mapper = new Mapper(dataSonnetMappingScript, new ArrayList<>(), true);
String mappedJson = mapper.transform(new StringDocument(jsonData, "application/json"), new HashMap<>(), "application/json").contents();

...
```
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