# Testing

Run `./test.sh` and ensure both CLJ and CLJS tests pass.

# Releasing

1. Increment last element of version number in both [./project.clj](project.clj)
and [./README.md](README.md).
2. `git commit -am "v${number}"` && `git push origin`
2. Run `lein clojars deploy`.
