package org.typelevel.jawn

/**
 * Basic byte array parser.
 */
final class ByteArrayParser[J](src: Array[Byte]) extends SyncParser[J] with ByteBasedParser[J] {
  private[this] var lineState = 0
  private[this] var offset = 0
  protected[this] def line(): Int = lineState
  
  protected[this] def newline(i: Int): Unit = { lineState += 1; offset = i + 1 }
  protected[this] def column(i: Int): Int = i - offset

  protected[this] def close(): Unit = ()
  protected[this] def reset(i: Int): Int = i
  protected[this] def checkpoint(
    state: Int,
    i: Int,
    context: FContext[J],
    stack: List[FContext[J]]
  ): Unit = {}
  protected[this] def byte(i: Int): Byte = src(i)
  protected[this] def at(i: Int): Char = src(i).toChar

  protected[this] def at(i: Int, k: Int): CharSequence =
    new String(src, i, k - i, utf8)

  protected[this] def atEof(i: Int): Boolean = i >= src.length
}
