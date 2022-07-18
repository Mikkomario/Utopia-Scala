package utopia.vault.coder.model.scala.template

import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.datatype.ScalaType

/**
  * A common trait for data types which can be converted to utopia.flow.datastructure.immutable.Value
  * @author Mikko Hilpinen
  * @since 17.7.2022, v1.5.1
  */
trait ValueConvertibleType
{
	/**
	  * @return The wrapped scala type
	  */
	def scalaType: ScalaType
	/**
	  * @return A default (empty) value appropriate for this data type
	  */
	def emptyValue: CodePiece
	
	/**
	  * Writes code that takes an instance of this type and converts it to a Value
	  * @param instanceCode A way to reference the instance to convert
	  * @return A code that takes an instance represented by the 'instanceCode' parameter and converts it to a Value.
	  *         An empty code piece if the instanceCode already converts to an instance of Value.
	  */
	def toValueCode(instanceCode: String): CodePiece
}
