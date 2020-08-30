package org.typelevel.jawn

/**
 * Lazy character sequence parsing.
 *
 * This is similar to StringParser, but acts on character sequences.
 */
final private[jawn] class CharSequenceParser[J](cs: CharSequence) extends SyncParser[J] with CharBasedParser[J] {
  private[this] var _line = 0
  private[this] var offset = 0
  protected[this] def column(i: Int): Int = i - offset
  protected[this] def newline(i: Int): Unit = { _line += 1; offset = i + 1 }
  protected[this] def line(): Int = _line
  protected[this] def reset(i: Int): Int = i
  protected[this] def checkpoint(state: Int, i: Int, context: FContext[J], stack: List[FContext[J]]): Unit = ()
  protected[this] def at(i: Int): Char = cs.charAt(i)
  protected[this] def at(i: Int, j: Int): CharSequence = cs.subSequence(i, j)
  protected[this] def atEof(i: Int): Boolean = i == cs.length
  protected[this] def close(): Unit = ()
}
