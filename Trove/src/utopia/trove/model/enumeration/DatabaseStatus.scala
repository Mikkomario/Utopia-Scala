package utopia.trove.model.enumeration

/**
  * An enumeration for database running status
  * @author Mikko Hilpinen
  * @since 21.9.2020, v1
  */
sealed trait DatabaseStatus
{
	// ABSTRACT	--------------------------
	
	/**
	  * @return Whether the database status is currently changing (this is a transitive status)
	  */
	def isProcessing: Boolean
	/**
	  * @return Whether the database has been started and is currently running
	  *         (might still be processing some updates, however)
	  */
	def isStarted: Boolean
	/**
	  * @return Whether the database is usable (started and updated) at the moment
	  */
	def isUsable: Boolean
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return Whether the database has completed its changes
	  */
	def isCompleted = !isProcessing
	/**
	  * @return Whether the database is yet to start
	  */
	def notStarted = !isStarted
	/**
	  * @return Whether the database is currently unusable (not started or not properly updated)
	  */
	def unusable = !isUsable
	
	/**
	  * @return Whether the database may be usable to some extent
	  */
	def isPartiallyUsable = isUsable || (isCompleted && isStarted)
}

sealed trait TransitiveDatabaseStatus extends DatabaseStatus
{
	// IMPLEMENTED	---------------------
	
	override def isProcessing = true
	override def isUsable = false
}

object DatabaseStatus
{
	/**
	  * A status where database is not started or has been shut down
	  */
	case object NotStarted extends DatabaseStatus
	{
		override def isProcessing = false
		override def isStarted = false
		override def isUsable = false
	}
	
	/**
	  * A status while database is starting
	  */
	case object Starting extends TransitiveDatabaseStatus
	{
		override def isStarted = false
	}
	
	/**
	  * A status while database is started and applying updates
	  */
	case object Updating extends TransitiveDatabaseStatus
	{
		override def isStarted = true
	}
	
	/**
	  * A status where database is both started and updated (at least to some extent)
	  */
	case object Setup extends DatabaseStatus
	{
		override def isProcessing = false
		override def isStarted = true
		override def isUsable = true
	}
	
	/**
	  * A status where database is started but not updated or couldn't be fully setup
	  */
	case object Started extends DatabaseStatus
	{
		override def isProcessing = false
		override def isStarted = true
		override def isUsable = false
	}
	
	/**
	  * A status while the database is stopping / shutting down
	  */
	case object Stopping extends TransitiveDatabaseStatus
	{
		override def isStarted = false
	}
}
