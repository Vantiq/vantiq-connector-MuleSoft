# Vantiq connector artifacts

This directory holds the artifacts required for the Vantiq connector functional and system tests.

These should be loaded into the target Vantiq namespace before the tests are executed,
typically, `mule_certification`.

The artifacts can be loaded executing the `import` command on the Vantiq CLI in this
directory:

```
% vantiq -s <profile> import
```
