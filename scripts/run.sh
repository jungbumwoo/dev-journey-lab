#!/bin/bash
# 데모 실행 스크립트
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/out"

DEMO=${1:-help}

case "$DEMO" in
    abstract)
        echo ">>> AbstractClassDemo"
        java -cp "$OUT" abstractclass.AbstractClassDemo
        ;;
    erasure)
        echo ">>> TypeErasureDemo"
        java -cp "$OUT" generics.TypeErasureDemo
        ;;
    bridge)
        echo ">>> BridgeMethodDemo"
        java -cp "$OUT" generics.BridgeMethodDemo
        ;;
    bounded)
        echo ">>> BoundedTypeDemo"
        java -cp "$OUT" generics.BoundedTypeDemo
        ;;
    all)
        SCRIPT="$ROOT/scripts/run.sh"
        for d in abstract erasure bridge bounded; do
            echo ""
            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            "$SCRIPT" "$d"
        done
        ;;
    *)
        echo "사용법: $0 <demo>"
        echo ""
        echo "  abstract  - Abstract class 내부 동작 (invokevirtual, ACC_ABSTRACT)"
        echo "  erasure   - Type erasure (checkcast, Signature attribute)"
        echo "  bridge    - Bridge method (ACC_BRIDGE, ACC_SYNTHETIC)"
        echo "  bounded   - Bounded type parameters & PECS"
        echo "  all       - 전체 실행"
        ;;
esac
