These scripts run the following commands as specified in the assignment:

- Compile/unparse the given testcases, recompile/unparse the output, and compare the results:
```bash
java -jar build/libs/MiniC-AstGen.jar -u mytest.mc.u mytest.mc
java -jar build/libs/MiniC-AstGen.jar -u mytest.mc.u.u mytest.mc.u
diff mytest.mc.u mytest.mc.u.u
```

- Generate an AST and compare it to a reference solution:
```bash
java -jar build/libs/MiniC-AstGen.jar -t my_c1.mc.ast minic/parser/tst/base/AST_testcases/c1.mc
diff --brief my_c1.mc.ast minic/parser/tst/base/AST_solutions_trees/c1.mc.ast
echo $?
```

Usage

1. Place `compare_self.sh` and `compare_tree.sh` in the project root (`CAS4104_Assignment_3_v2/`).
2. Run the script you want. Each script prints per-test progress and a final summary with counts of successes and failures.

Output and logs

- `compare_self.sh` writes two unparsed files for each testcase under `./output_self/` and `./output_self/recompiled/`.
- `compare_tree.sh` writes generated `.ast` files under `./output_tree/`.
- If a compilation or comparison fails, logs are saved under `./output_self/errors/` or `./output_tree/errors/` respectively.
- Any diffs are stored in `./output_self/diffs/` or `./output_tree/diffs/`.

Notes

- Paths in the examples assume you run commands from the project root (`CAS4104_Assignment_3_v2/`).
- Only the 42 provided testcases are tested.
- These scripts may contain mistakes. **Use at your own risk!**