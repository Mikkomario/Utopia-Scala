package utopia.vault.coder.model.scala.template

import utopia.flow.parse.file.FileExtensions._
import utopia.vault.coder.model.scala.code.Code

import java.nio.file.Path
import scala.io.Codec

/**
  * Common trait for items which can be converted to 1 or more full lines of code
  * @author Mikko Hilpinen
  * @since 31.8.2021, v0.1
  */
trait CodeConvertible
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return This item converted to code. Expects the topmost line not to be intended but the other
	  *         lines to be intended correctly relative to the topmost line.
	  */
	def toCode: Code
	
	
	// OTHER    -----------------------------
	
	/**
	  * Writes this code as a file into the specified path
	  * @param path Path to which this code is written
	  * @param codec Codec used when encoding the file
	  * @return Success or failure
	  */
	def writeTo(path: Path)(implicit codec: Codec) =
		path.createParentDirectories().flatMap { _.writeLines(toCode.lines.map { _.toString }) }
}
