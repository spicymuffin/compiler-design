#!/usr/bin/env bash

# MiniC Assignment 5 CodeGen Test Script (runtime-output based)
# Run from the assn05 repo root (or anywhere; script will cd to its own dir).

set -u

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

# Repo root = directory containing this script
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR" || exit 1

# Directories / files
TESTCASES_DIR="minic/codegen/tst/base/testcases"
SOLUTIONS_DIR="minic/codegen/tst/base/solutions"
JAR_FILE="build/libs/MiniC-CodeGen.jar"
JASMIN_JAR="resources/jasmin_2.4/jasmin.jar"

TEMP_DIR="/tmp/minic_codegen_test_$$"
RESULTS_DIR="/tmp/minic_codegen_results_$$"

mkdir -p "$TEMP_DIR" "$RESULTS_DIR"

cleanup() {
  rm -rf "$TEMP_DIR" "$RESULTS_DIR"
}
trap cleanup EXIT

# Previous run tracking
PREV_PASSED_TESTS=0
PREV_FAILED_TESTS=0
PREV_PERCENTAGE=0
declare -a PREV_FAILED_LIST
RUN_COUNT=0

# Pick a Jasmin assembler command
jasmin_assemble() {
  local jfile="$1"
  if command -v jasmin >/dev/null 2>&1; then
    jasmin "$jfile"
    return $?
  fi

  if [ -f "$ROOT_DIR/$JASMIN_JAR" ]; then
    java -jar "$ROOT_DIR/$JASMIN_JAR" "$jfile"
    return $?
  fi

  echo -e "${RED}No Jasmin assembler found.${NC}"
  echo -e "${YELLOW}Install 'jasmin' or ensure ${JASMIN_JAR} exists.${NC}"
  return 127
}

build_project() {
  # Try the most helpful tasks first (assignment provides these tasks)
  local tasks=("jarNoScannerNoParserNoSem" "jarNoScannerNoParser" "jarNoScanner" "jar")
  for t in "${tasks[@]}"; do
    ./gradlew "$t" -q >/dev/null 2>&1
    if [ $? -eq 0 ] && [ -f "$JAR_FILE" ]; then
      echo -e "${GREEN}Build successful using task: ${t}${NC}"
      return 0
    fi
  done

  echo -e "${RED}Build failed! Could not produce ${JAR_FILE}${NC}"
  echo -e "${YELLOW}Try manually: ./gradlew jarNoScannerNoParserNoSem${NC}"
  return 1
}

run_tests() {
  TOTAL_TESTS=0
  PASSED_TESTS=0
  FAILED_TESTS=0
  declare -a FAILED_LIST

  RUN_COUNT=$((RUN_COUNT + 1))

  echo -e "${BLUE}========================================${NC}"
  echo -e "${BLUE}  MiniC CodeGen Test Suite (Run #${RUN_COUNT})${NC}"
  echo -e "${BLUE}========================================${NC}"
  echo ""

  echo -e "${YELLOW}Building project...${NC}"
  if ! build_project; then
    return 1
  fi
  echo ""

  if [ ! -d "$TESTCASES_DIR" ]; then
    echo -e "${RED}Testcases directory not found: ${TESTCASES_DIR}${NC}"
    return 1
  fi
  if [ ! -d "$SOLUTIONS_DIR" ]; then
    echo -e "${RED}Solutions directory not found: ${SOLUTIONS_DIR}${NC}"
    return 1
  fi

  echo -e "${YELLOW}Running tests (compile → assemble → run → compare runtime output)...${NC}"
  echo ""

  for testfile in "$TESTCASES_DIR"/*.mc; do
    [ -f "$testfile" ] || continue

    filename="$(basename "$testfile")"
    testname="${filename%.mc}"
    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    workdir="$TEMP_DIR/$testname"
    mkdir -p "$workdir"

    # 1) Compile .mc -> .j (Emitter writes output in CWD), so run from workdir
    compile_log="$workdir/compile_${testname}.log"
    (
      cd "$workdir" || exit 1
      java -ea -jar "$ROOT_DIR/$JAR_FILE" "$ROOT_DIR/$testfile" >"$compile_log" 2>&1
    )
    comp_rc=$?

    generated_j="$workdir/${testname}.j"
    if [ $comp_rc -ne 0 ] || [ ! -f "$generated_j" ]; then
      echo -e "  ${RED}✗${NC} Test ${testname}: COMPILATION FAILED"
      FAILED_TESTS=$((FAILED_TESTS + 1))
      FAILED_LIST+=("$testname")
      cp -f "$compile_log" "$RESULTS_DIR/${testname}_compile.log" 2>/dev/null
      continue
    fi

    # 2) Assemble .j -> .class
    asm_log="$workdir/asm_${testname}.log"
    (
      cd "$workdir" || exit 1
      jasmin_assemble "${testname}.j" >"$asm_log" 2>&1
    )
    asm_rc=$?

    generated_class="$workdir/${testname}.class"
    if [ $asm_rc -ne 0 ] || [ ! -f "$generated_class" ]; then
      echo -e "  ${RED}✗${NC} Test ${testname}: JASMIN ASSEMBLY FAILED"
      FAILED_TESTS=$((FAILED_TESTS + 1))
      FAILED_LIST+=("$testname")
      cp -f "$asm_log" "$RESULTS_DIR/${testname}_asm.log" 2>/dev/null
      continue
    fi

    # 3) Run bytecode
    output_file="$workdir/output_${testname}.txt"
    run_log="$workdir/run_${testname}.log"
    (
      cd "$workdir" || exit 1
      # build/classes/java/main contains minic/lang runtime
      java -cp "$ROOT_DIR/build/classes/java/main:." "$testname" >"$output_file" 2>"$run_log"
    )
    run_rc=$?

    if [ $run_rc -ne 0 ]; then
      echo -e "  ${RED}✗${NC} Test ${testname}: RUNTIME ERROR (exit ${run_rc})"
      FAILED_TESTS=$((FAILED_TESTS + 1))
      FAILED_LIST+=("$testname")
      cp -f "$run_log" "$RESULTS_DIR/${testname}_run.log" 2>/dev/null
      continue
    fi

    # 4) Compare runtime output to expected
    expected_out="$SOLUTIONS_DIR/${testname}.txt"
    if [ ! -f "$expected_out" ]; then
      echo -e "  ${YELLOW}?${NC} Test ${testname}: Expected output not found (${testname}.txt) (skipping compare)"
      continue
    fi

    diff -u --ignore-all-space "$expected_out" "$output_file" > /dev/null 2>&1
    if [ $? -eq 0 ]; then
      echo -e "  ${GREEN}✓${NC} Test ${testname}: PASSED"
      PASSED_TESTS=$((PASSED_TESTS + 1))
    else
      echo -e "  ${RED}✗${NC} Test ${testname}: FAILED"
      FAILED_TESTS=$((FAILED_TESTS + 1))
      FAILED_LIST+=("$testname")
      diff -u --ignore-all-space "$expected_out" "$output_file" > "$RESULTS_DIR/diff_${testname}.txt" 2>&1
      cp -f "$generated_j" "$RESULTS_DIR/${testname}.j" 2>/dev/null
      cp -f "$compile_log" "$RESULTS_DIR/${testname}_compile.log" 2>/dev/null
      cp -f "$asm_log" "$RESULTS_DIR/${testname}_asm.log" 2>/dev/null
      cp -f "$run_log" "$RESULTS_DIR/${testname}_run.log" 2>/dev/null
    fi
  done

  if [ $TOTAL_TESTS -gt 0 ]; then
    PERCENTAGE=$((PASSED_TESTS * 100 / TOTAL_TESTS))
  else
    PERCENTAGE=0
  fi

  echo ""
  echo -e "${BLUE}========================================${NC}"
  echo -e "${BLUE}  Test Summary${NC}"
  echo -e "${BLUE}========================================${NC}"
  echo ""
  echo -e "  Total Tests:   ${BLUE}${TOTAL_TESTS}${NC}"
  echo -e "  Passed:        ${GREEN}${PASSED_TESTS}${NC}"
  echo -e "  Failed:        ${RED}${FAILED_TESTS}${NC}"
  echo -e "  Success Rate:  ${BLUE}${PERCENTAGE}%${NC}"
  echo ""

  if [ $RUN_COUNT -gt 1 ]; then
    echo -e "${CYAN}========================================${NC}"
    echo -e "${CYAN}  Comparison with Previous Run${NC}"
    echo -e "${CYAN}========================================${NC}"
    echo ""

    PASSED_DIFF=$((PASSED_TESTS - PREV_PASSED_TESTS))
    FAILED_DIFF=$((FAILED_TESTS - PREV_FAILED_TESTS))
    PERCENTAGE_DIFF=$((PERCENTAGE - PREV_PERCENTAGE))

    if [ $PASSED_DIFF -gt 0 ]; then
      echo -e "Passed:        ${GREEN}+${PASSED_DIFF}${NC} (${PREV_PASSED_TESTS} → ${PASSED_TESTS})"
    elif [ $PASSED_DIFF -lt 0 ]; then
      echo -e "Passed:        ${RED}${PASSED_DIFF}${NC} (${PREV_PASSED_TESTS} → ${PASSED_TESTS})"
    else
      echo -e "Passed:        No change (${PASSED_TESTS})"
    fi

    if [ $FAILED_DIFF -gt 0 ]; then
      echo -e "Failed:        ${RED}+${FAILED_DIFF}${NC} (${PREV_FAILED_TESTS} → ${FAILED_TESTS})"
    elif [ $FAILED_DIFF -lt 0 ]; then
      echo -e "Failed:        ${GREEN}${FAILED_DIFF}${NC} (${PREV_FAILED_TESTS} → ${FAILED_TESTS})"
    else
      echo -e "Failed:        No change (${FAILED_TESTS})"
    fi

    if [ $PERCENTAGE_DIFF -gt 0 ]; then
      echo -e "Success Rate:  ${GREEN}+${PERCENTAGE_DIFF}%${NC} (${PREV_PERCENTAGE}% → ${PERCENTAGE}%)"
    elif [ $PERCENTAGE_DIFF -lt 0 ]; then
      echo -e "Success Rate:  ${RED}${PERCENTAGE_DIFF}%${NC} (${PREV_PERCENTAGE}% → ${PERCENTAGE}%)"
    else
      echo -e "Success Rate:  No change (${PERCENTAGE}%)"
    fi
    echo ""
  fi

  if [ $FAILED_TESTS -gt 0 ]; then
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}  Currently Failed Tests${NC}"
    echo -e "${RED}========================================${NC}"
    echo ""
    for failure in "${FAILED_LIST[@]}"; do
      echo -e "  ${RED}✗${NC} Test $failure"
      if [ -f "$RESULTS_DIR/diff_${failure}.txt" ]; then
        echo -e "      Diff saved: ${RESULTS_DIR}/diff_${failure}.txt"
      fi
      if [ -f "$RESULTS_DIR/${failure}_compile.log" ]; then
        echo -e "      Compile log: ${RESULTS_DIR}/${failure}_compile.log"
      fi
      if [ -f "$RESULTS_DIR/${failure}_asm.log" ]; then
        echo -e "      Asm log:     ${RESULTS_DIR}/${failure}_asm.log"
      fi
      if [ -f "$RESULTS_DIR/${failure}_run.log" ]; then
        echo -e "      Run log:     ${RESULTS_DIR}/${failure}_run.log"
      fi
    done
    echo ""
    echo -e "${YELLOW}Tip: Inspect ${RESULTS_DIR}/ for diffs/logs and the generated .j files.${NC}"
    echo ""
  else
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  All tests passed! 🎉${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
  fi

  PREV_PASSED_TESTS=$PASSED_TESTS
  PREV_FAILED_TESTS=$FAILED_TESTS
  PREV_PERCENTAGE=$PERCENTAGE
  PREV_FAILED_LIST=("${FAILED_LIST[@]}")

  return 0
}

show_test_details() {
  local testname="$1"

  echo -e "${CYAN}========================================${NC}"
  echo -e "${CYAN}  Test Details: ${testname}${NC}"
  echo -e "${CYAN}========================================${NC}"
  echo ""

  local testfile="$TESTCASES_DIR/${testname}.mc"
  local expected="$SOLUTIONS_DIR/${testname}.txt"
  local workdir="$TEMP_DIR/${testname}"
  local actual="$workdir/output_${testname}.txt"

  if [ -f "$testfile" ]; then
    echo -e "${YELLOW}Test Input (${testname}.mc):${NC}"
    sed -n '1,200p' "$testfile"
    echo ""
  else
    echo -e "${RED}Test file not found: $testfile${NC}"
    echo ""
  fi

  if [ -f "$expected" ]; then
    echo -e "${YELLOW}Expected Output (${testname}.txt):${NC}"
    cat "$expected"
    echo ""
  else
    echo -e "${RED}Expected output not found: $expected${NC}"
    echo ""
  fi

  if [ -f "$actual" ]; then
    echo -e "${YELLOW}Actual Output:${NC}"
    cat "$actual"
    echo ""
  else
    echo -e "${RED}Actual output not found (run tests first).${NC}"
    echo ""
  fi

  if [ -f "$RESULTS_DIR/diff_${testname}.txt" ]; then
    echo -e "${YELLOW}Diff:${NC}"
    cat "$RESULTS_DIR/diff_${testname}.txt"
    echo ""
  else
    echo -e "${GREEN}No diff saved (test may have passed or not compared).${NC}"
    echo ""
  fi

  if [ -f "$RESULTS_DIR/${testname}.j" ]; then
    echo -e "${YELLOW}Generated Jasmin (.j) saved at:${NC} ${RESULTS_DIR}/${testname}.j"
  fi
}

list_tests() {
  echo -e "${CYAN}========================================${NC}"
  echo -e "${CYAN}  Available CodeGen Tests${NC}"
  echo -e "${CYAN}========================================${NC}"
  echo ""
  for testfile in "$TESTCASES_DIR"/*.mc; do
    [ -f "$testfile" ] || continue
    basename "$testfile" .mc
  done
  echo ""
}

# Main loop
while true; do
  run_tests

  echo ""
  echo -e "${MAGENTA}========================================${NC}"
  echo -e "${YELLOW}Options:${NC}"
  echo -e "  ${GREEN}r${NC} - Run tests again"
  echo -e "  ${GREEN}d [name]${NC} - Show details for test (e.g., 'd hello' or 'd short_circuit')"
  echo -e "  ${GREEN}l${NC} - List all available tests"
  echo -e "  ${GREEN}q${NC} - Quit"
  echo ""
  echo -n -e "${YELLOW}Choose an option: ${NC}"
  read -r response
  response="$(echo "$response" | tr '[:upper:]' '[:lower:]')"

  if [ "$response" = "q" ] || [ "$response" = "quit" ]; then
    echo -e "${BLUE}Exiting test suite. Goodbye!${NC}"
    break
  elif [ "$response" = "r" ] || [ "$response" = "run" ] || [ "$response" = "y" ] || [ "$response" = "yes" ]; then
    echo ""
    echo ""
    continue
  elif [ "$response" = "l" ] || [ "$response" = "list" ]; then
    list_tests
    echo -n -e "${YELLOW}Press Enter to continue...${NC}"
    read -r
    echo ""
  elif [[ "$response" == d* ]]; then
    testname="$(echo "$response" | sed 's/^d *//')"
    if [ -f "$TESTCASES_DIR/${testname}.mc" ]; then
      show_test_details "$testname"
      echo -n -e "${YELLOW}Press Enter to continue...${NC}"
      read -r
      echo ""
    else
      echo -e "${RED}Test not found: ${testname}${NC}"
      echo -e "${YELLOW}Use 'l' to list available tests${NC}"
      sleep 1
    fi
  else
    echo ""
    echo ""
    continue
  fi
done

exit 0
