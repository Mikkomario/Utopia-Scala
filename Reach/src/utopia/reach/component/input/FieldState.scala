package utopia.reach.component.input

/**
  * An enumeration for different field states
  * @author Mikko Hilpinen
  * @since 18.9.2022, v0.3.5
  */
sealed trait FieldState
{
	/**
	  * @return Whether the user is actively engaged with / editing the field at this state
	  */
	def isActive: Boolean
	/**
	  * @return Whether the user has interacted with the field at some point at this state
	  */
	def touched: Boolean
	
	/**
	  * @return Whether the user is not actively engaged with the field at this state
	  */
	def isPassive = !isActive
	/**
	  * @return Whether the field is yet untouched by the user at this state
	  */
	def untouched = !touched
}

object FieldState
{
	/**
	  * @return All possible field states
	  */
	def values = Vector[FieldState](BeforeEdit, Editing, AfterEdit)
	
	/**
	  * The state before a field has ever received focus
	  */
	case object BeforeEdit extends FieldState
	{
		override def isActive = false
		override def touched = false
	}
	/**
	  * The state in which the field is actively focused and/or edited by the user
	  */
	case object Editing extends FieldState
	{
		override def isActive = true
		override def touched = true
	}
	/**
	  * The state after the field has been edited and/or focused and is no longer so
	  */
	case object AfterEdit extends FieldState
	{
		override def isActive = false
		override def touched = true
	}
}
