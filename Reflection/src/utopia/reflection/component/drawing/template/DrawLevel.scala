package utopia.reflection.component.drawing.template

object DrawLevel
{
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
	
	/**
	  * All available draw levels in order from background to foreground
	  */
	val values = Vector(Background, Normal, Foreground)
	
	/**
	  * @return The default draw level
	  */
	def default = Normal
}

/**
  * These are the different levels where component contents may be drawn
  * @author Mikko Hilpinen
  * @since 29.4.2019, v1+
  */
sealed trait DrawLevel extends Equals with Ordered[DrawLevel]
{
	def index: Int
	
	override def compare(that: DrawLevel) = index - that.index
}
