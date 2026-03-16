# Official Plugin Catalog

This directory stores the official online plugin catalogs consumed by EasyPostman.

- `catalog-github.json`: official GitHub source
- `catalog-gitee.json`: official Gitee source

These files are treated as generated release metadata.

Do not edit them casually by hand. The current primary update path is:

- `.github/workflows/plugin-release.yml`

That workflow can update:

- public catalogs in `plugin-catalog/`
- bundled fallback catalogs in `easy-postman-plugins/plugin-manager/src/main/resources/plugin-catalog/`

Current behavior:

- each plugin release may update the matching GitHub and Gitee catalog entry
- bundled fallback catalogs are kept in sync with the public catalogs
- `sha256` is supported by the installer and should be filled by the release pipeline when available

If you need to adjust catalog structure manually, update both places together:

- `plugin-catalog/`
- `easy-postman-plugins/plugin-manager/src/main/resources/plugin-catalog/`
