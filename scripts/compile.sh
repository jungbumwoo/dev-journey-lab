#!/bin/bash
# Java 소스 컴파일 스크립트
set -e

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC="$ROOT/src"
OUT="$ROOT/out"

echo "=== Compiling Java sources ==="
echo "  src : $SRC"
echo "  out : $OUT"
echo ""

mkdir -p "$OUT"

# 모든 .java 파일 찾아서 컴파일
find "$SRC" -name "*.java" | sort | xargs javac -d "$OUT" -sourcepath "$SRC"

echo "컴파일 성공!"
echo ""
echo "생성된 클래스 파일:"
find "$OUT" -name "*.class" | sort | while read f; do
    echo "  ${f#$OUT/}"
done
