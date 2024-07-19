# Migration Guides

In general, you should always save your changes/customizations in the following directories:

- `cfg/` : Configuration files
- `web/` : Your HTML/TXT templates, CSS style-sheets, Java-scripts, images, fonts, translations 
- `ssl/` : Your SSL keystore file if used
- `gen/` : In case you adjusted the templates used by PLANT
- `db/h2/db/` : Your H2 DB, if you use H2 and this location

## Release 3.0.0

Release 3.0.0 is a major release and many changes have been made. Migrating from 2.x.x requires some effort.

See [Migration Guide 3.0.0](migration/3_0_0.md).
