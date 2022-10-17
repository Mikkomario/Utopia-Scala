package utopia.vault.coder.controller.writer.input

import utopia.vault.model.immutable.Table

import scala.io.Codec

/**
  * Writes an input template based on an existing database structure
  * @author Mikko Hilpinen
  * @since 17.10.2022, v1.7.1
  */
object InputFromDbWriter
{
	def apply(table: Table)(implicit codec: Codec) = {
	
	}
}
