package utopia.reflection.container.swing.window

object WindowResizePolicy
{
	/**
	  * Policy where the user may resize a window and program must adhere to that
	  */
	case object User extends WindowResizePolicy
	{
		override def allowsUserResize = true
		override def allowsProgramResize = false
	}
	
	/**
	  * Policy where program may resize a window and user cannot
	  */
	case object Program extends WindowResizePolicy
	{
		override def allowsUserResize = false
		override def allowsProgramResize = true
	}
	
	/**
	  * Policy where window is not automatically resized and user cannot resize it either.
	  */
	case object Fixed extends WindowResizePolicy
	{
		override def allowsUserResize = false
		override def allowsProgramResize = false
	}
}

/**
  * These are the different policies one can use when determining who is able to resize a window
  * @author Mikko Hilpinen
  * @since 17.4.2019, v0.1+
  */
sealed trait WindowResizePolicy
{
	/**
	  * @return Whether this policy allows user to resize the window
	  */
	def allowsUserResize: Boolean
	
	/**
	  * @return Whether this policy allows the program (window contents) to dictate window size
	  */
	def allowsProgramResize: Boolean
}
