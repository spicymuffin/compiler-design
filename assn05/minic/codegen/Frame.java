package minic.codegen;

/** Frame class used for book-keeping information related to the method
 * frame of a MiniC function (during compilation).
 */
public class Frame {

  private int labelNr;
  private int localVarNr;
  private boolean isMain;

  /*
   * local variables in main (static methods):
   * 0: argv
   * 1: mc$
   *
   * local variables for all other MiniC functions (instance methods)
   * 0: "this" ptr
   *
   */

  /** Constructor of the Frame class. */
  public Frame(boolean isMain) {
    this.isMain = isMain;
    labelNr = -1;
    if (this.isMain) {
      localVarNr = 1;
    } else {
      localVarNr = 0;
    }
  }

  /** Method getNewLabel: obtain the label number of the next available
   * label of a method frame.
   */
  public int getNewLabel() {
    labelNr++;
    return labelNr;
  } 

  /** Method getNewLocalVarIndex: obtain the index of the next free slot
   * in the local variable array of a method frame.
   */
  public int getNewLocalVarIndex() {
    localVarNr++;
    return localVarNr;
  } 

}
