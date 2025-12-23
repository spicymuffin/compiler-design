package minic.lang;

import java.io.*;
import java.util.StringTokenizer;

/** The System class provides the built-in functions of the MiniC programming
 * language. Technically, those are static methods that get called from the
 * bytecode of a MiniC program.
 */
public class System {

  private static BufferedReader reader;

  static {
    // Initialize static field of class System:
    reader = new BufferedReader(new InputStreamReader(java.lang.System.in));
  }

  /** getInt() method for the MiniC built-in function of the same name. */
  public static final int getInt() {
    try {
      java.lang.System.out.print("Please enter an integer value: ");
      String s = reader.readLine();
      StringTokenizer st = new StringTokenizer(s);
      int i = Integer.parseInt(st.nextToken());
      java.lang.System.out.println("You have entered " + i + ".");
      return i;
    } catch (java.io.IOException e) {
      java.lang.System.out.println("Caught IOException " + e.getMessage());
      java.lang.System.exit(1);
      return -1;
    }
  }

  /** getBool() method for the MiniC built-in function of the same name. */
  public static final boolean getBool() {
    try {
      java.lang.System.out.print("Please enter a bool value (true or false): ");
      String s = reader.readLine();
      StringTokenizer st = new StringTokenizer(s);
      boolean b = Boolean.parseBoolean(st.nextToken());
      java.lang.System.out.println("You have entered " + b + ".");
      return b;
    } catch (java.io.IOException e) {
      java.lang.System.out.println("Caught IOException " + e.getMessage());
      java.lang.System.exit(1);
      return false;
    }
  }

  /** getFloat() method for the MiniC built-in function of the same name. */
  public static final float getFloat() {
    try {
      java.lang.System.out.print("Please enter a float value: ");
      String s = reader.readLine();
      StringTokenizer st = new StringTokenizer(s);
      float f = Float.parseFloat(st.nextToken());
      java.lang.System.out.println("You have entered " + f + ".");
      return f;
    } catch (java.io.IOException e) {
      java.lang.System.out.println("Caught IOException " + e.getMessage());
      java.lang.System.exit(1);
      return (float) -1.0;
    }
  }

  /** getString() method for the MiniC built-in function of the same name. */
  public static final void getString() {
    java.lang.System.out.println("Error: getString not implemented.");
  }

  /** putInt() method for the MiniC built-in function of the same name. */
  public static final void putInt(int i) {
    java.lang.System.out.print(i);
  }

  /** putBool() method for the MiniC built-in function of the same name. */
  public static final void putBool(boolean b) {
    java.lang.System.out.print(b);
  }

  /** putFloat() method for the MiniC built-in function of the same name. */
  public static final void putFloat(float f) {
    java.lang.System.out.print(f);
  }

  /** putString() method for the MiniC built-in function of the same name. */
  public static final void putString(String s) {
    java.lang.System.out.print(s);
  }

  /** putLn() method for the MiniC built-in function of the same name. */
  public static final void putLn() {
    java.lang.System.out.println("");
  }
}
