package utopia.vault.coder.model.scala.declaration

import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.template.ScalaConvertible

/**
  * An enumeration for different prefixes that can be used with declarations, excluding visibility prefix
  * @author Mikko Hilpinen
  * @since 1.11.2021, v1.3
  */
sealed trait DeclarationPrefix extends ScalaConvertible
{
	/**
	  * @return Scala keyword used to indicate this prefix
	  */
	def keyword: String
	
	override def toScala = CodePiece(keyword)
}

object DeclarationPrefix
{
	// ATTRIBUTES   ---------------------
	
	/**
	  * All available values of this enumeration
	  */
	val values = Vector[DeclarationPrefix](Sealed, Implicit, Abstract, Case, Override, Lazy)
	
	
	// NESTED   -------------------------
	
	/**
	  * Prefix used with case classes and case objects
	  */
	case object Case extends DeclarationPrefix
	{
		override val keyword = "case"
	}
	/**
	  * Prefix used with overridden property- and function declarations
	  */
	case object Override extends DeclarationPrefix
	{
		override val keyword = "override"
	}
	/**
	  * Prefix used with lazy values
	  */
	case object Lazy extends DeclarationPrefix
	{
		override val keyword = "lazy"
	}
	/**
	  * Prefix used with implicit classes, functions, parameters and values
	  */
	case object Implicit extends DeclarationPrefix
	{
		override val keyword = "implicit"
	}
	/**
	  * Prefix used with sealed traits
	  */
	case object Sealed extends DeclarationPrefix
	{
		override val keyword = "sealed"
	}
	/**
	  * Prefix used with abstract classes
	  */
	case object Abstract extends DeclarationPrefix
	{
		override val keyword = "abstract"
	}
}