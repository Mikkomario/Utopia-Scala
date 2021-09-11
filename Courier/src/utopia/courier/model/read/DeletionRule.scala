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
	  * @param wasSuccess Whether message reading succeeded
	  * @return Whether the message should be deleted
	  */
	def shouldDelete(wasSuccess: Boolean): Boolean
}

object DeletionRule
{
	/**
	  * A rule that never deletes messages
	  */
	case object NeverDelete extends DeletionRule
	{
		override def canDelete = false
		override def shouldDelete(wasSuccess: Boolean) = false
	}
	/**
	  * A rule that deletes messages if reading succeeded
	  */
	case object DeleteOnSuccess extends DeletionRule
	{
		override def canDelete = true
		override def shouldDelete(wasSuccess: Boolean) = wasSuccess
	}
	/**
	  * A rule that deletes messages that failed to be processed
	  */
	case object DeleteOnFailure extends DeletionRule
	{
		override def canDelete = true
		override def shouldDelete(wasSuccess: Boolean) = !wasSuccess
	}
	/**
	  * A rule that deletes messages after they have been read or after they have caused a read failure
	  */
	case object AlwaysDelete extends DeletionRule
	{
		override def canDelete = true
		override def shouldDelete(wasSuccess: Boolean) = true
	}
}
