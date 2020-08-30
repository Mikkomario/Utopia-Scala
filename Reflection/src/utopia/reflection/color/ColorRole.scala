package utopia.reflection.color

/**
  * An enumeration for various roles for color in a user interface
  * @author Mikko Hilpinen
  * @since 18.8.2020, v1.2
  */
sealed trait ColorRole

sealed trait AdditionalColorRole extends ColorRole
{
	/**
	  * @return A role that can be used instead of this color when another shade is required or this
	  *         color is not available.
	  */
	def backup: ColorRole
}

object ColorRole
{
	/**
	  * Primary UI color. Used in interactive elements, areas and backgrounds alike
	  */
	case object Primary extends ColorRole
	
	/**
	  * Secondary UI color. Used for highlighting primary interactive UI elements
	  */
	case object Secondary extends AdditionalColorRole
	{
		override def backup = Primary
	}
	
	/**
	  * Tertiary UI color. Used as an alternative highlighting color
	  */
	case object Tertiary extends AdditionalColorRole
	{
		override def backup = Secondary
	}
	
	/**
	  * Grayscale UI color. Used in background elements, text fields etc.
	  */
	case object Gray extends ColorRole
	
	/**
	  * Color indicating an error. Used as a highlight color when something goes wrong and demands user attention.
	  */
	case object Error extends AdditionalColorRole
	{
		override def backup = Tertiary
	}
	
	/**
	  * Color indicating an error. Used when the problem is question is not as serious as it would be when using the
	  * Error color.
	  */
	case object Warning extends AdditionalColorRole
	{
		override def backup = Error
	}
	
	/**
	  * Color indicating additional information
	  */
	case object Info extends AdditionalColorRole
	{
		override def backup = Primary
	}
	
	/**
	  * Color indicating success
	  */
	case object Success extends AdditionalColorRole
	{
		override def backup = Info
	}
}
