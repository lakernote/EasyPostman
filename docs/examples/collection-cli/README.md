# EasyPostman Collection CLI example

This example sends two multipart uploads to Postman Echo, one for each row in `users.csv`. Java 17 or newer is required.

From the repository root, either:

- download `easy-postman-{version}.jar` from [GitHub Releases](https://github.com/lakernote/easy-postman/releases), or
- build the current source:

```bash
mvn -pl easy-postman-app -am -DskipTests clean package
```

Run with the source-built JAR:

```bash
java -jar easy-postman-app/target/easy-postman-*.jar \
  collection run docs/examples/collection-cli/upload.postman_collection.json \
  -e docs/examples/collection-cli/postman-echo.postman_environment.json \
  -d docs/examples/collection-cli/users.csv \
  --folder "Upload API" \
  --bail \
  --out target/collection-cli-result.json
```

For a downloaded JAR, replace `easy-postman-app/target/easy-postman-*.jar` with its local path. Check support first with `java -jar easy-postman-{version}.jar collection run --help`.

The upload path in the collection is `fixtures/sample-file.txt`. Relative file paths are resolved from the collection directory, so the command works regardless of the shell's current directory.
