package utopia.flow.util.console

import java.io.OutputStream

/**
 * A stream that writes into System.out without ever closing it
 * @author Mikko Hilpinen
 * @since 08.01.2026, v2.8
 */
object ConsoleStream extends OutputStream
{
	// IMPLEMENTED  ------------------------
	
	override def write(b: Int): Unit = System.out.write(b)
	override def write(b: Array[Byte]): Unit = System.out.write(b)
	override def write(b: Array[Byte], off: Int, len: Int): Unit = System.out.write(b, off, len)
	
	override def flush(): Unit = System.out.flush()
}
