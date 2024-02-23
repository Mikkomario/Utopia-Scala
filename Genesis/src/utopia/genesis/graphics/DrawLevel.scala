package utopia.genesis.graphics

/**
  * These are the different levels where component contents may be drawn
  * @author Mikko Hilpinen
  * @since 29.4.2019, Reflection v1. Moved to Genesis at v4.0
  */
sealed trait DrawLevel extends Equals with Ordered[DrawLevel]
{
	def index: Int
	
	override def compare(that: DrawLevel) = index - that.index
}

object DrawLevel
{
	// ATTRIBUTES   --------------------
	
	/**
	  * All available draw levels in order from background to foreground
	  */
	val values = Vector[DrawLevel](Background, Normal, Foreground)
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return The default draw level
	  */
	def default = Normal
	
	
	// VALUES   ------------------------
	
	/**
	  * This draw level draws behind component contents
	  */
	case object Background extends DrawLevel
	{
		override def index = 0
	}
	/**
	  * This draw level draws above component contents but under sub components
	  */
	case object Normal extends DrawLevel
	{
		override def index = 1
	}
	/**
	  * This draw level draws above all other content
	  */
	case object Foreground extends DrawLevel
	{
		override def index = 2
	}
}
