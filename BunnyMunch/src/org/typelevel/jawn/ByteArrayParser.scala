/*
 * Copyright (c) 2012 Typelevel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.typelevel.jawn

/**
 * Basic byte array parser.
 */
final class ByteArrayParser[J](src: Array[Byte]) extends SyncParser[J] with ByteBasedParser[J] {
  private[this] var lineState = 0
  private[this] var offset = 0
  protected[this] def line(): Int = lineState

  final protected[this] def newline(i: Int): Unit = { lineState += 1; offset = i + 1 }
  final protected[this] def column(i: Int): Int = i - offset

  final protected[this] def close(): Unit = ()
  final protected[this] def reset(i: Int): Int = i
  final protected[this] def checkpoint(
    state: Int,
    i: Int,
    context: FContext[J],
    stack: List[FContext[J]]
  ): Unit = {}

  final protected[this] def byte(i: Int): Byte = {
    if (Platform.isJs) {
      if (i < 0 || i >= src.length) throw new ArrayIndexOutOfBoundsException
    }
    src(i)
  }

  final protected[this] def at(i: Int): Char = {
    if (Platform.isJs) {
      if (i < 0 || i >= src.length) throw new ArrayIndexOutOfBoundsException
    }
    src(i).toChar
  }

  final protected[this] def at(i: Int, k: Int): CharSequence = {
    if (Platform.isJs) {
      if (i < 0 || i >= src.length) throw new ArrayIndexOutOfBoundsException
      if (k < 0 || k > src.length) throw new ArrayIndexOutOfBoundsException
      if (i > k) throw new ArrayIndexOutOfBoundsException
    }
    new String(src, i, k - i, utf8)
  }

  final protected[this] def atEof(i: Int): Boolean = i >= src.length
}
