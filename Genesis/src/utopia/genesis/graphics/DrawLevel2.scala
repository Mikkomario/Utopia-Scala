package utopia.genesis.graphics

/**
  * These are the different levels where component contents may be drawn
  * @author Mikko Hilpinen
  * @since 29.4.2019, Reflection v1+
  */
sealed trait DrawLevel2 extends Equals with Ordered[DrawLevel2]
{
	def index: Int
	
	override def compare(that: DrawLevel2) = index - that.index
}

object DrawLevel2
{
	// ATTRIBUTES   --------------------
	
	/**
	  * All available draw levels in order from background to foreground
	  */
	val values = Vector[DrawLevel2](Background, Normal, Foreground)
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return The default draw level
	  */
	def default = Normal
	
	
	// VALUES   ------------------------
	
	/**
	  * This draw level draws behind component contents
	  */
	case object Background extends DrawLevel2
	{
		override def index = 0
	}
	/**
	  * This draw level draws above component contents but under sub components
	  */
	case object Normal extends DrawLevel2
	{
		override def index = 1
	}
	/**
	  * This draw level draws above all other content
	  */
	case object Foreground extends DrawLevel2
	{
		override def index = 2
	}
}
