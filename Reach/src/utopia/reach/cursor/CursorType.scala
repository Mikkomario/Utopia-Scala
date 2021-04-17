package utopia.reach.cursor

/**
  * A common trait for various cursor roles
  * @author Mikko Hilpinen
  * @since 11.11.2020, v0.1
  */
trait CursorType
{
	/**
	  * @return A cursor type that should be used when this type is unavailable
	  */
	def backup: Option[CursorType]
}

object CursorType
{
	/**
	  * The default arrow pointer cursor
	  */
	case object Default extends CursorType
	{
		override def backup = None
	}
	
	/**
	  * A cursor used for indicating an interactive object
	  */
	case object Interactive extends CursorType
	{
		override def backup = Some(Default)
	}
	
	/**
	  * A cursor used for indicating editable or selectable text
	  */
	case object Text extends CursorType
	{
		override def backup = Some(Interactive)
	}
}
