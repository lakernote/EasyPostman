# EasyPostman Collection CLI example

This example sends two multipart uploads to Postman Echo, one for each row in `users.csv`. Java 17 or newer is required. The only required CLI argument is the collection file; this full example also supplies an environment, iteration data, a folder filter, bail behavior, and a report path.

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

The collection, `-e` environment, and `-d` data paths above are relative to the directory where the command is run (the repository root in this example). They may all be replaced with absolute paths. `--working-dir` does not change how `-e`, `-g`, or `-d` paths are resolved; it only changes the base directory for relative upload paths.

The collection reads its upload path from `{{uploadFile}}`. The environment sets it to the relative path `fixtures/sample-file.txt`, which is resolved from the collection directory, so the command works regardless of the shell's current directory. Absolute paths are also supported: replace that environment value with a full path such as `/opt/api-fixtures/sample-file.txt` or `C:\\api-fixtures\\sample-file.txt`.

Runtime behavior matches the complete CLI guide: disabled collection/folder variables are skipped, iteration data is available through `pm.iterationData`, `pm.variables`, and `{{name}}`, and local values created with `pm.variables.set(...)` last for the whole run. Pre-request `pm.test(...)` assertions affect reports and exit status. A Postman file field may also use an array-valued `src`; every listed file is uploaded with that field name.
