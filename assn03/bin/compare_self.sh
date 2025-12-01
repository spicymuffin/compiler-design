#!/bin/bash
set -uo pipefail

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$BASE_DIR/build/libs/MiniC-AstGen.jar"
TEST_DIR="$BASE_DIR/minic/parser/tst/base/AST_testcases"
OUT_DIR="$BASE_DIR/output_self"
DUPLICATES_DIR="$OUT_DIR/recompiled"
DIFF_DIR="$OUT_DIR/diffs"

mkdir -p "$OUT_DIR" "$DUPLICATES_DIR" "$DIFF_DIR"
ERR_DIR="$OUT_DIR/errors"
mkdir -p "$ERR_DIR"

if [ ! -f "$JAR" ]; then
  echo "Jar not found: $JAR" >&2
  exit 1
fi

echo "== Running (re)compilation and diff per testcase (c1..c42) =="
first_failed=()
second_failed=()
missing_input=()
missing_diff=()
identical_count=0
diff_count=0
gen_success=0
regen_success=0

for i in {1..42}; do
  input_mc="$TEST_DIR/c${i}.mc"
  a="$OUT_DIR/c${i}.mc.u"
  b="$DUPLICATES_DIR/c${i}.mc.u.u"
  diff_file="$DIFF_DIR/c${i}.diff"

  if [ ! -f "$input_mc" ]; then
    echo "[SKIP] c${i}: missing testcase $input_mc"
    missing_input+=("c${i}")
    continue
  fi

  # Step 1: generate AST and unparse to a
  echo "[1] c${i}: $input_mc -> $a"
  tmp_log="$ERR_DIR/c${i}.step1.tmp"
  log="$ERR_DIR/c${i}.step1.log"
  java -jar "$JAR" -u "$a" "$input_mc" >"$tmp_log" 2>&1
  rc=$?
  if [ $rc -ne 0 ]; then
    mv "$tmp_log" "$log"
    echo "[1] FAILED c${i} (log: $log)" >&2
    first_failed+=("c${i}")
    # skip unparse and diff for this test
    continue
  else
    rm -f "$tmp_log"
  fi
  # count successful compilation
  gen_success=$((gen_success+1))

  # Step 2: re-generate AST based on a and unparse to b
  echo "[2] c${i}: $a -> $b"
  tmp_log2="$ERR_DIR/c${i}.step2.tmp"
  log2="$ERR_DIR/c${i}.step2.log"
  java -jar "$JAR" -u "$b" "$a" >"$tmp_log2" 2>&1
  rc2=$?
  if [ $rc2 -ne 0 ]; then
    mv "$tmp_log2" "$log2"
    echo "[2] FAILED c${i} (log: $log2)" >&2
    second_failed+=("c${i}")
    # skip diff if unparse failed
    continue
  else
    # Even if the process returned success, ensure the expected output file was produced
    if [ ! -s "$b" ]; then
      # keep the log for debugging when output is missing or empty
      mv "$tmp_log2" "$log2"
      echo "[2] WARNING c${i}: recompiled output missing or empty (log: $log2)" >&2
      second_failed+=("c${i}")
      # skip diff if output missing
      continue
    else
      rm -f "$tmp_log2"
    fi
  fi
  # count successful recompilation
  regen_success=$((regen_success+1))

  # Step 3: diff a and b
  if [ ! -f "$a" ] || [ ! -f "$b" ]; then
    echo "[3] c${i}: missing file(s):"
    [ ! -f "$a" ] && echo "  missing: $a"
    [ ! -f "$b" ] && echo "  missing: $b"
    missing_diff+=("c${i}")
    continue
  fi

  if diff -u "$a" "$b" > "$diff_file"; then
    echo "[3] c${i}: identical"
    rm -f "$diff_file"
    identical_count=$((identical_count+1))
  else
    echo "[3] c${i}: differences -> $diff_file"
    diff_count=$((diff_count+1))
  fi
done

# Error report
echo
echo "Self-comparison summary:"
echo "  Total tests: 42"
echo "  Missing input: ${#missing_input[@]}"
echo "  [1] Compilation failures: ${#first_failed[@]}"
echo "  [1] Compilation successes: $gen_success"
echo "  [2] Recompilation failures: ${#second_failed[@]}"
echo "  [2] Recompilation successes: $regen_success"
echo "  [3] Identical: $identical_count"
echo "  [3] Different: $diff_count"
echo "  [3] Missing : ${#missing_diff[@]}"
echo "  [3] Diffs dir: $DIFF_DIR"

echo
echo "Detailed lists (if any):"
if [ ${#missing_input[@]} -ne 0 ]; then
  echo "  Missing input: ${missing_input[*]}"
fi
if [ ${#first_failed[@]} -ne 0 ]; then
  echo "  [1] Compilation failed: ${first_failed[*]}"
fi
if [ ${#second_failed[@]} -ne 0 ]; then
  echo "  [2] Recompilation failed: ${second_failed[*]}"
fi
if [ ${#missing_diff[@]} -ne 0 ]; then
  echo "  [3] Missing files for diff stage: ${missing_diff[*]}"
fi
if [ $diff_count -ne 0 ]; then
  echo "  [3] Different files:"
  ls -1 "$DIFF_DIR" | sed -n '1,200p' || true
fi

echo "Done."