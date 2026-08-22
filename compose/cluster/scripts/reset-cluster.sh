#!/usr/bin/env bash

cat >&2 <<'MESSAGE'
Cluster reset is intentionally not automated because RESET HARD destroys node
identity and topology. If this is disposable lab data, use `make destroy` and
then `make cluster-up` to recreate only the lab's named volumes.
MESSAGE
exit 2
