#!/bin/bash
# 바이트코드 검사 스크립트 (javap 래퍼)
#
# 사용법:
#   ./scripts/inspect.sh <ClassName> [flags]
#
# flags:
#   -c         : 바이트코드 인스트럭션 출력 (기본값)
#   -verbose   : 상수 풀, Signature, flags 등 상세 출력
#   -p         : private 멤버 포함 (bridge method 보려면 필수)
#   -l         : LocalVariableTable 출력
#   -c -p      : 바이트코드 + private 멤버
#
# 주요 확인 포인트:
#   Shape.class      : ACC_ABSTRACT flag, abstract 메서드 (Code 없음)
#   Circle.class     : invokespecial Shape.<init> (super() 호출)
#   Box.class        : Signature attribute, T -> Object erasure
#   UpperCaseTransformer: ACC_BRIDGE | ACC_SYNTHETIC bridge method
#
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/out"

CLASS=$1
shift
FLAGS="${*:--c}"  # 기본 플래그: -c

if [ -z "$CLASS" ] || [ "$CLASS" = "help" ]; then
    echo "사용법: $0 <ClassName> [javap flags]"
    echo ""
    echo "플래그:"
    echo "  -c         바이트코드 인스트럭션 (기본)"
    echo "  -verbose   상세 (Signature, flags, constant pool)"
    echo "  -p         private 멤버 포함 (bridge method 확인 시 필요)"
    echo ""
    echo "사용 예시:"
    echo "  $0 Shape -verbose          # ACC_ABSTRACT, 상수 풀 확인"
    echo "  $0 Circle -c               # invokespecial super() 확인"
    echo "  $0 Box -verbose            # Signature attribute 확인"
    echo "  $0 UpperCaseTransformer -p # bridge method 확인"
    echo "  $0 TypeErasureDemo -c      # checkcast 인스트럭션 확인"
    echo ""
    echo "사용 가능한 클래스:"
    find "$OUT" -name "*.class" 2>/dev/null | sort | while read f; do
        name="${f#$OUT/}"
        name="${name%.class}"
        name="${name//\//.}"
        # 내부 클래스($) 포함 표시
        echo "  $name"
    done
    exit 0
fi

# 클래스 파일 검색 (내부 클래스 지원: $ 포함)
CLASS_FILE=$(find "$OUT" -name "${CLASS}.class" 2>/dev/null | head -1)

if [ -z "$CLASS_FILE" ]; then
    echo "클래스를 찾을 수 없음: $CLASS"
    echo ""
    echo "사용 가능한 클래스:"
    find "$OUT" -name "*.class" 2>/dev/null | sort | while read f; do
        basename "${f%.class}"
    done
    exit 1
fi

# 파일 경로 -> 클래스 이름 변환
CLASS_NAME="${CLASS_FILE#$OUT/}"
CLASS_NAME="${CLASS_NAME%.class}"
CLASS_NAME="${CLASS_NAME//\//.}"

echo "=== javap $FLAGS $CLASS_NAME ==="
echo ""
javap $FLAGS -classpath "$OUT" "$CLASS_NAME"
