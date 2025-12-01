#!/bin/bash
set -uo pipefail


BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$BASE_DIR/build/libs/MiniC-AstGen.jar"
TEST_DIR="$BASE_DIR/minic/parser/tst/base/AST_testcases"
EXPECTED_DIR="$BASE_DIR/minic/parser/tst/base/AST_solutions_trees"
OUT_TREE_DIR="$BASE_DIR/output_tree"
DIFF_DIR="$OUT_TREE_DIR/diffs"

mkdir -p "$OUT_TREE_DIR"
mkdir -p "$DIFF_DIR"
ERR_DIR="$OUT_TREE_DIR/errors"
mkdir -p "$ERR_DIR"

if [ ! -f "$JAR" ]; then
  echo "Jar not found: $JAR" >&2
  exit 1
fi

no_input=()
step1_failed=()
solutions_missing=()
identical=()
different=()
gen_success=0
compared_count=0
compare_error=()

for i in {1..42}; do
  input="$TEST_DIR/c${i}.mc"
  out_tree="$OUT_TREE_DIR/c${i}.mc.ast"
  expected="$EXPECTED_DIR/c${i}.mc.ast"

  if [ ! -f "$input" ]; then
    no_input+=("c${i}")
    continue
  fi

  echo "[1] c${i}: $input -> $out_tree"
  tmp_log="$ERR_DIR/c${i}.step1.tmp"
  log="$ERR_DIR/c${i}.step1.log"
  java -jar "$JAR" -t "$out_tree" "$input" >"$tmp_log" 2>&1
  java_rc=$?
  if [ -f "$out_tree" ] && [ -s "$out_tree" ]; then
    gen_success=$((gen_success+1))
    # keep log only if process returned non-zero; otherwise remove temp
    if [ $java_rc -ne 0 ]; then
      mv "$tmp_log" "$log"
    else
      rm -f "$tmp_log"
    fi
  else
    mv "$tmp_log" "$log"
    echo "[1] FAILED c${i} (rc=$java_rc; missing or empty $out_tree) (log: $log)" >&2
    step1_failed+=("c${i}")
    # don't attempt comparison for this test
    continue
  fi

  if [ ! -f "$expected" ]; then
    solutions_missing+=("c${i}")
    continue
  fi
  # count tests where expected exists and comparison will be attempted
  compared_count=$((compared_count+1))

  # Use diff --brief: rc=0 identical, rc=1 different, rc>1 error
  diff --brief "$out_tree" "$expected" >/dev/null 2>&1
  rc=$?
  if [ $rc -eq 0 ]; then
    identical+=("c${i}")
    echo "[2] c${i}: identical"
    # remove any stale diff
    rm -f "$DIFF_DIR/c${i}.diff"
  elif [ $rc -eq 1 ]; then
    different+=("c${i}")
    diff_file="$DIFF_DIR/c${i}.diff"
    echo "[2] c${i}: different -> $diff_file"
    # save unified diff for inspection
    diff -u "$expected" "$out_tree" > "$diff_file" || true
  else
    compare_error+=("c${i}")
    echo "[2] c${i}: compare error (diff exit $rc)" >&2
  fi
done

echo
echo "Tree comparison summary:"
echo "  Total tests: 42"
echo "  Missing input: ${#no_input[@]}"
echo "  [1] Compilation successes: $gen_success"
echo "  [1] Compilation failures: ${#step1_failed[@]}"
echo "  [2] Missing solutions: ${#solutions_missing[@]}"
echo "  [2] Identical: ${#identical[@]}"
echo "  [2] Different: ${#different[@]}"
echo "  [2] Compared (expected present): $compared_count"
echo "  [2] Compare errors: ${#compare_error[@]}"
echo "  Diffs dir: $DIFF_DIR"
echo
echo "Detailed lists (if any):"
if [ ${#no_input[@]} -ne 0 ]; then
  echo "  Missing input: ${no_input[*]}"
fi
if [ ${#step1_failed[@]} -ne 0 ]; then
  echo "  Compilation failed: ${step1_failed[*]}"
fi
if [ ${#solutions_missing[@]} -ne 0 ]; then
  echo "  Missing expected files: ${solutions_missing[*]}"
fi
if [ ${#different[@]} -ne 0 ]; then
  echo "  Different files: ${different[*]}"
fi
if [ ${#compare_error[@]} -ne 0 ]; then
  echo "  Compare errors: ${compare_error[*]}"
fi

echo "Outputs written to: $OUT_TREE_DIR"
