package utopia.flow.parse

import java.io.InputStream

/**
 * An empty implementation of InputStream
 * @author Mikko Hilpinen
 * @since 05.11.2025, v2.8
 */
object EmptyInputStream extends InputStream
{
	override def read(): Int = -1
	override def read(b: Array[Byte]): Int = -1
	override def read(b: Array[Byte], off: Int, len: Int): Int = -1
	
	override def skip(n: Long): Long = 0
	
	override def close(): Unit = ()
}
