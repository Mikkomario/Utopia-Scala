package utopia.citadel.coder.model.scala.declaration

import utopia.citadel.coder.model.scala.Visibility.Public
import utopia.citadel.coder.model.scala.Visibility
import utopia.citadel.coder.model.scala.template.Referencing

/**
  * Declares a scala item of some sort
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
trait Declaration extends Referencing
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return Visibility of this declaration
	  */
	def visibility: Visibility
	
	/**
	  * @return Keyword for this declaration (E.g. "def" or "val")
	  */
	def keyword: String
	
	/**
	  * @return Name of this method / property
	  */
	def name: String
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return The base part of this declaration as a string. E.g. "private def test"
	  */
	protected def baseString =
	{
		val visibilityString = if (visibility == Public) "" else s"${ visibility.toScala } "
		s"$visibilityString$keyword $name"
	}
}
