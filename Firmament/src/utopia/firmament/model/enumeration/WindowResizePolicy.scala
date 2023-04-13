package utopia.firmament.model.enumeration

/**
  * These are the different policies one can use when determining who is able to resize a window
  * @author Mikko Hilpinen
  * @since 17.4.2019, v0.1+
  */
sealed trait WindowResizePolicy
{
	// ABSTRACT -----------------------
	
	/**
	  * @return Whether this policy allows user to resize the window
	  */
	def allowsUserResize: Boolean
	/**
	  * @return Whether this policy allows the program (window contents) to dictate window size
	  */
	def allowsProgramResize: Boolean
	
	
	// COMPUTED ----------------------
	
	/**
	  * @return A copy of this policy that allows the user to resize windows
	  */
	def allowingResizeByUser = WindowResizePolicy(allowsProgramResize, allowUserResize = true)
	/**
	  * @return A copy of this policy that allows the program to resize windows
	  */
	def allowingResizeByProgram = WindowResizePolicy(allowProgramResize = true, allowsUserResize)
	
	/**
	  * @return A copy of this policy that won't allow the user to resize windows
	  */
	def disallowingResizeByUser = WindowResizePolicy(allowsProgramResize, allowUserResize = false)
	/**
	  * @return A copy of this policy that won't allow the program to resize windows (unless necessary)
	  */
	def disallowingResizeByProgram = WindowResizePolicy(allowProgramResize = false, allowsUserResize)
}

object WindowResizePolicy
{
	// OTHER    ---------------------
	
	/**
	  * @param allowProgramResize Whether program-initiated window-resizing should be enabled
	  * @param allowUserResize Whether user-initiated window-resizing should be enabled
	  * @return A resize policy that supports the specified use-case
	  */
	def apply(allowProgramResize: Boolean, allowUserResize: Boolean): WindowResizePolicy = {
		if (allowProgramResize) {
			if (allowUserResize)
				UserAndProgram
			else
				Program
		}
		else if (allowUserResize)
			User
		else
			Fixed
	}
	
	
	// VALUES   ---------------------
	
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
	  * Policy where the program resizes the window on content updates,
	  * but where the user is also allowed to resize the window.
	  */
	case object UserAndProgram extends WindowResizePolicy
	{
		override def allowsUserResize: Boolean = true
		override def allowsProgramResize: Boolean = true
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