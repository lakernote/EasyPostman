# EasyPostman Functional CLI example

This directory is a complete native workspace for `functional run`:

```text
functional-cli/
├── collections.json
├── environments.json
├── functional_config.json
├── users.csv
└── fixtures/
    └── sample-file.txt
```

Run the selected request and embedded CSV rows from `functional_config.json`:

```bash
java -jar easy-postman-app/target/easy-postman-*.jar \
  functional run docs/examples/functional-cli \
  --bail \
  --out target/functional-run-result.json
```

Override the embedded rows with a workspace-relative CSV file:

```bash
java -jar easy-postman-app/target/easy-postman-*.jar \
  functional run docs/examples/functional-cli \
  -d users.csv \
  --bail \
  --out target/functional-run-csv-result.json
```

Each command runs two iterations, sends two requests, and evaluates six tests.
