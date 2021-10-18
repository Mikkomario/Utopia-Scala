package utopia.vault.coder.model.scala.template

import utopia.vault.coder.model.scala.code.CodePiece

/**
  * Common trait for items which may be converted to scala code
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
trait ScalaConvertible
{
	// ABSTRACT -------------------------------
	
	/**
	  * @return A scala string based on this item
	  */
	def toScala: CodePiece
	
	
	// IMPLEMENTED  ---------------------------
	
	override def toString = toScala.text
}
