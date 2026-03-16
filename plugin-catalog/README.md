# Official Plugin Catalog

This directory stores the official online plugin catalogs consumed by EasyPostman.

- `catalog-github.json`: official GitHub source
- `catalog-gitee.json`: official Gitee source

Do not edit those two generated files by hand. Use:

```bash
./scripts/update-plugin-catalog.sh
```

If you have already built all plugin jars and want to fill `sha256` too:

```bash
./scripts/update-plugin-catalog.sh --with-local-sha256
```

The script updates:

- public catalogs in `plugin-catalog/`
- bundled fallback catalogs in `easy-postman-plugins/plugin-manager/src/main/resources/plugin-catalog/`

`docs/plugin-market/catalog.sample.json` remains a documentation sample only.
