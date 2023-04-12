package utopia.paradigm.color

import utopia.flow.collection.mutable.iterator.OptionsIterator

/**
  * An enumeration for various roles for color in a user interface
  * @author Mikko Hilpinen
  * @since 18.8.2020, Reflection v1.2
  */
trait ColorRole
{
	// ABSTRACT ------------------------
	
	/**
	  * @return An alternative backup for this color role
	  */
	def alternative: Option[ColorRole]
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return An iterator that returns color roles alternative to this one in the order of preference
	  */
	def alternativesIterator = OptionsIterator.iterate(alternative) { _.alternative }
}

object ColorRole
{
	// VALUES   -------------------------
	
	/**
	  * Primary UI color. Used in interactive elements, areas and backgrounds alike
	  */
	case object Primary extends ColorRole
	{
		override def alternative = None
	}
	/**
	  * Secondary UI color. Used for highlighting primary interactive UI elements
	  */
	case object Secondary extends ColorRole
	{
		override def alternative = Some(Primary)
	}
	/**
	  * Tertiary UI color. Used as an alternative highlighting color
	  */
	case object Tertiary extends ColorRole
	{
		override def alternative = Some(Secondary)
	}
	
	/**
	  * Grayscale UI color. Used in background elements, text fields etc.
	  */
	case object Gray extends ColorRole
	{
		override def alternative = None
	}
	
	/**
	  * Color indicating an error. Used as a highlight color when something goes wrong and demands user attention.
	  */
	case object Failure extends ColorRole
	{
		override def alternative = Some(Tertiary)
	}
	
	/**
	  * Color indicating an error. Used when the problem is question is not as serious as it would be when using the
	  * Error color.
	  */
	case object Warning extends ColorRole
	{
		override def alternative = Some(Failure)
	}
	
	/**
	  * Color indicating additional information
	  */
	case object Info extends ColorRole
	{
		override def alternative = Some(Primary)
	}
	
	/**
	  * Color indicating success
	  */
	case object Success extends ColorRole
	{
		override def alternative = Some(Secondary)
	}
}
