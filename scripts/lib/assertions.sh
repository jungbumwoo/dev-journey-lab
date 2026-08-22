#!/usr/bin/env bash

# shellcheck source=common.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

assert_equals() {
    local expected="$1"
    local actual="$2"
    local message="${3:-values differ}"
    if [[ "$expected" != "$actual" ]]; then
        die "$message: expected='$expected', actual='$actual'"
    fi
}

assert_contains() {
    local expected_part="$1"
    local actual="$2"
    local message="${3:-text not found}"
    if [[ "$actual" != *"$expected_part"* ]]; then
        die "$message: expected to contain '$expected_part', actual='$actual'"
    fi
}

assert_nonempty() {
    local actual="$1"
    local message="${2:-value is empty}"
    [[ -n "$actual" ]] || die "$message"
}
