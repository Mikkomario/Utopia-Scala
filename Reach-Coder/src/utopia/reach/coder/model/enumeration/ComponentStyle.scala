package utopia.reach.coder.model.enumeration

/**
  * An enumeration for the styles of approaches to component creation and functionality;
  * Namely, whether the component is immutable, based on pointers or mutable.
  * @author Mikko Hilpinen
  * @since 2.6.2023, v1.0
  */
sealed trait ComponentStyle
{
	/**
	  * @return A keyword used for distinguishing this value from user input
	  */
	def keyword: String
}

object ComponentStyle
{
	// ATTRIBUTES   ----------------
	
	/**
	  * All values of this enumeration
	  */
	val values = Vector[ComponentStyle](Immutable, View, Mutable)
	
	
	// OTHER    --------------------
	
	/**
	  * @param input User input which represents a component style
	  * @return Style matching user input. None if no style matches.
	  */
	def apply(input: String) = {
		val lower = input.toLowerCase
		values.find { v => lower.contains(v.keyword) }
	}
	
	
	// VALUES   --------------------
	
	/**
	  * A style where components are totally immutable and (mostly) unchanging
	  */
	case object Immutable extends ComponentStyle
	{
		override val keyword: String = "immutable"
	}
	/**
	  * A style where components don't provide a mutable interface,
	  * but may be controlled with various pointers that are specified during component creation.
	  */
	case object View extends ComponentStyle
	{
		override val keyword: String = "view"
	}
	/**
	  * A style where a component provides a mutable interface to one or more of its properties.
	  */
	case object Mutable extends ComponentStyle
	{
		override val keyword: String = "mutable"
	}
}
