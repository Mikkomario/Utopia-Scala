package utopia.courier.model.read

/**
  * An enumeration for different rules to apply to message deletion
  * @author Mikko Hilpinen
  * @since 11.9.2021, v0.1
  */
sealed trait DeletionRule
{
	/**
	  * @return Whether this deletion rule allows deletion under some circumstances
	  */
	def canDelete: Boolean
	
	/**
	  * @return Whether skipped messages should be deleted also
	  */
	def shouldDeleteSkipped: Boolean
	/**
	  * @return Whether messages should be deleted which failed to be processed or read
	  */
	def shouldDeleteFailed: Boolean
	/**
	 * @return Whether successfully read messages should be deleted
	 */
	def shouldDeleteProcessed: Boolean
}

object DeletionRule
{
	/**
	  * A rule that never deletes messages
	  */
	case object NeverDelete extends DeletionRule
	{
		override def canDelete = false
		override def shouldDeleteSkipped = false
		override def shouldDeleteFailed = false
		override def shouldDeleteProcessed: Boolean = false
	}
	/**
	  * A rule that deletes all messages where reading succeeded
	  */
	case object DeleteProcessed extends DeletionRule
	{
		override def canDelete = true
		override def shouldDeleteSkipped = false
		override def shouldDeleteFailed = false
		override def shouldDeleteProcessed: Boolean = true
	}
	/**
	  * A rule that deletes messages that failed to be processed
	  */
	case object DeleteOnFailure extends DeletionRule
	{
		override def canDelete = true
		override def shouldDeleteSkipped = false
		override def shouldDeleteFailed = true
		override def shouldDeleteProcessed: Boolean = false
	}
	/**
	  * A rule that deletes skipped messages only
	  */
	case object DeleteSkipped extends DeletionRule
	{
		override def canDelete = true
		override def shouldDeleteSkipped = true
		override def shouldDeleteFailed = false
		override def shouldDeleteProcessed: Boolean = false
	}
	/**
	  * A rule that deletes all non-failing messages, including those that were skipped
	  */
	case object DeleteAllExceptFailed extends DeletionRule
	{
		override def canDelete = true
		override def shouldDeleteSkipped = true
		override def shouldDeleteFailed = false
		override def shouldDeleteProcessed: Boolean = true
	}
	/**
	 * A rule that deletes read messages, whether reading succeeded or failed. Preserves messages marked as skipped.
	 */
	case object DeleteAllExceptSkipped extends DeletionRule
	{
		override def canDelete: Boolean = true
		override def shouldDeleteSkipped: Boolean = false
		override def shouldDeleteFailed: Boolean = true
		override def shouldDeleteProcessed: Boolean = true
	}
	/**
	  * A rule that deletes messages after they have been read or skipped, or after they have caused a read failure
	  */
	case object AlwaysDelete extends DeletionRule
	{
		override def canDelete = true
		override def shouldDeleteSkipped = true
		override def shouldDeleteFailed = true
		override def shouldDeleteProcessed: Boolean = true
	}
}
