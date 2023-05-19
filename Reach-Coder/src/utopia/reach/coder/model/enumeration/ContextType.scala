package utopia.reach.coder.model.enumeration

import utopia.coder.model.scala.datatype.Reference
import utopia.flow.util.StringExtensions._
import utopia.reach.coder.util.ReachReferences.Reach._
import utopia.reach.coder.util.ReachReferences._

/**
  * An enumeration for different available component creation context types
  * @author Mikko Hilpinen
  * @since 19.5.2023, v1.0
  */
sealed trait ContextType
{
	/**
	  * @return Keyword used for identifying this type from user input
	  */
	def keyword: String
	/**
	  * @return Reference to the associated context class
	  */
	def reference: Reference
	/**
	  * @return Reference to the associated component factory trait
	  */
	def factory: Reference
}

object ContextType
{
	// ATTRIBUTES   ------------------------
	
	val values = Vector[ContextType](Base, Color, Text, Window)
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param input User input
	  * @return The context type mentioned in that input. None if no context type was mentioned.
	  */
	def apply(input: String) = values.find { c => input.containsIgnoreCase(c.keyword) }
	
	
	// VALUES   ----------------------------
	
	/**
	  * Represents the BaseContext class from Firmament
	  */
	case object Base extends ContextType
	{
		override val keyword: String = "base"
		
		override def reference: Reference = firmament.baseContext
		override def factory: Reference = baseContextualFactory
	}
	/**
	  * Represents the ColorContext class from Firmament
	  */
	case object Color extends ContextType
	{
		override val keyword: String = "color"
		
		override def reference: Reference = firmament.colorContext
		override def factory: Reference = colorContextualFactory
	}
	/**
	  * Represents the TextContext class from Firmament
	  */
	case object Text extends ContextType
	{
		override val keyword: String = "text"
		
		override def reference: Reference = firmament.textContext
		override def factory: Reference = textContextualFactory
	}
	/**
	  * Represents the ReachContentWindowContext class
	  */
	case object Window extends ContextType
	{
		override val keyword: String = "window"
		
		override def reference: Reference = contentWindowContext
		override def factory: Reference = contentWindowContextualFactory
	}
}
