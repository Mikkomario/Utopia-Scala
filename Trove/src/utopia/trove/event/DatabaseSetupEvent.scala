package utopia.trove.event

import utopia.flow.util.Version
import utopia.trove.model.stored.DatabaseVersion
import utopia.trove.model.DatabaseStructureSource

import scala.util.{Failure, Success}

/**
  * A common trait for different events generated while setting up a database
  * @author Mikko Hilpinen
  * @since 19.9.2020, v1
  */
sealed trait DatabaseSetupEvent

/**
  * A common trait for events that complete a database setup process
  */
sealed trait DatabaseSetupCompletionEvent extends DatabaseSetupEvent
{
	// ABSTRACT	------------------------
	
	/**
	  * @return A possible error associated with this event. None if this event was a success.
	  */
	def failure: Option[Throwable]
	
	
	// COMPUTED	-----------------------
	
	/**
	  * @return Whether database setup finished successfully
	  */
	def isSuccess = failure.isEmpty
	/**
	  * @return Whether database setup failed to finish
	  */
	def isFailure = failure.nonEmpty
	/**
	  * @return A try based on this event
	  */
	def toTry = failure match
	{
		case Some(error) => Failure(error)
		case None => Success(())
	}
}

object DatabaseSetupEvent
{
	// NESTED	-------------------------------
	
	/**
	  * An event generated when database settings have been configured
	  */
	case object DatabaseConfigured extends DatabaseSetupEvent
	
	/**
	  * An event generated when the local database has started
	  */
	case object DatabaseStarted extends DatabaseSetupEvent
	
	/**
	  * An event generated when new database updates were found
	  * @param filesToImport Files that will be imported
	  * @param currentVersion Current database version (None if no version is installed yet)
	  */
	case class UpdatesFound(filesToImport: Vector[DatabaseStructureSource],
							currentVersion: Option[DatabaseVersion] = None) extends DatabaseSetupEvent
	
	/**
	  * An event generated for each update file that was successfully applied
	  * @param appliedUpdate An update that was last applied
	  * @param remainingUpdates Updates still remaining to be applied (empty if this was the last update)
	  */
	case class UpdateApplied(appliedUpdate: DatabaseStructureSource,
							 remainingUpdates: Vector[DatabaseStructureSource] = Vector()) extends DatabaseSetupEvent
	
	/**
	  * An event generated when database configuration or starting fails. Database won't be usable at all in this case.
	  * @param error Error that caused the setup process to fail
	  */
	case class SetupFailed(error: Throwable) extends DatabaseSetupCompletionEvent
	{
		override def failure = Some(error)
	}
	
	/**
	  * An event generated when a database structure update fails, usually due to a sql syntax error
	  * @param error An error that caused this failure
	  * @param update The update that failed to complete
	  * @param currentVersion Current database structure version (None if no structure has been initialized yet)
	  */
	case class UpdateFailed(error: Throwable, update: DatabaseStructureSource, currentVersion: Option[Version])
		extends DatabaseSetupCompletionEvent
	{
		override def failure = Some(error)
	}
	
	/**
	  * An event generated when database setup process completes without errors
	  * @param currentVersion Current database structure version (None if no structure has been initialized yet)
	  */
	case class SetupSucceeded(currentVersion: Option[DatabaseVersion]) extends DatabaseSetupCompletionEvent
	{
		// COMPUTED	------------------------------
		
		/**
		  * @return Whether a database structure has also been set up properly
		  */
		def isDatabaseStructureSetup = currentVersion.isDefined
		
		
		// IMPLEMENTED	--------------------------
		
		override def failure = None
	}
}
