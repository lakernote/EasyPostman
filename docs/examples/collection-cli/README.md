# EasyPostman Collection CLI example

This directory is a complete native workspace for `collection run`:

```text
collection-cli/
├── collections.json
├── environments.json
├── users.csv
└── fixtures/
    └── sample-file.txt
```

Build the JAR from the repository root:

```bash
mvn -pl easy-postman-app -am -DskipTests package
```

Run the `Upload API` folder with the workspace-relative CSV file:

```bash
java -jar easy-postman-app/target/easy-postman-*.jar \
  collection run docs/examples/collection-cli \
  -c "EasyPostman CLI Example" \
  --folder "Upload API" \
  -d users.csv \
  --bail \
  --out target/collection-run-result.json
```

The command runs two iterations, sends two requests, and evaluates six tests.
`users.csv` is resolved from the workspace directory.
