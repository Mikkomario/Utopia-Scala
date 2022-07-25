package utopia.journey.controller

import java.time.Instant
import utopia.annex.controller.QueueSystem
import utopia.annex.model.request.GetRequest
import utopia.annex.model.schrodinger.{CachedFindSchrodinger, CompletedSchrodinger}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.container.SaveTiming.Delayed
import utopia.flow.container.{ModelFileContainer, ObjectsFileContainer}
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.util.FileExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.flow.util.logging.Logger
import utopia.journey.util.JourneyContext._
import utopia.metropolis.model.combined.description.DescribedDescriptionRole
import utopia.metropolis.model.combined.language.DescribedLanguage

import scala.concurrent.duration.Duration

/**
  * An access point used for retrieving description-related data that is shared between all user accounts
  * (languages, user role descriptions, etc.)
  * @author Mikko Hilpinen
  * @since 19.7.2020, v0.1
  */
class DescriptionData(currentQueueSystem: => QueueSystem, defaultUpdatePeriod: Duration = 3.days,
					  requestTimeout: Duration = 10.seconds)
{
	// ATTRIBUTES	-------------------------
	
	private val updateTimesContainer = new ModelFileContainer(containersDirectory/"data-updates.json",
		Delayed(5.minutes))
	
	private val languagesContainer = new ObjectsFileContainer(containersDirectory/"languages.json", DescribedLanguage)
	private val descriptionRolesContainer = new ObjectsFileContainer(containersDirectory/"description-roles.json",
		DescribedDescriptionRole)
	// TODO: Implement handling for user roles and tasks when needed
	
	
	// COMPUTED	-----------------------------
	
	private def lastLanguageUpdate = lastUpdateFor("language")
	
	private def lastDescriptionRolesUpdate = lastUpdateFor("description_role")
	
	def languages = getOrUpdate(languagesContainer,
		"language", "languages")
	
	def descriptionRoles = getOrUpdate(descriptionRolesContainer,
		"description_role", "description-roles")
	
	
	// OTHER	---------------------------
	
	/**
	  * Updates locally cached language data by requesting new data from the server
	  * @return A schrödinger that will contain the read language data. Populated with local data during the request.
	  */
	def updateLanguages() = update(languagesContainer, "language", "languages")
	
	/**
	  * Updates locally cached description role data by requesting new description roles from the server
	  * @return A shcrödinger that will contain the read description role data. Populated with locally cached
	  *         data during the request.
	  */
	def updateDescriptionRoles() =
		update(descriptionRolesContainer, "description_role", "description-roles")
	
	private def lastUpdateFor(typeName: String) = updateTimesContainer(typeName).instantOr(Instant.EPOCH)
	
	// Updates data if too long a time has passed since the last update
	// Otherwise returns local data
	private def getOrUpdate[A <: ModelConvertible](container: ObjectsFileContainer[A], typeName: String,
												   requestPath: String) =
	{
		if (defaultUpdatePeriod.finite.exists { lastUpdateFor(typeName) < Now - _ })
			update(container, typeName, requestPath)
		else
			CompletedSchrodinger.success(container.current)
	}
	
	// Updates a single containers data by performing a server request
	// Returns a shrödinger
	private def update[A <: ModelConvertible](container: ObjectsFileContainer[A], typeName: String, requestPath: String) =
	{
		// Populates the schrödinger with local data
		val localData = container.current
		val requestTime = Instant.now()
		
		val schrodinger = new CachedFindSchrodinger(localData)
		// Deprecates request after a timeout if there is local data to use
		schrodinger.completeWith(currentQueueSystem.push(GetRequest(requestPath,
			localData.nonEmpty && requestTimeout.finite.exists { Now > requestTime + _ }))) {
			_.vector(container.factory).parsed } { log(_) }
		
		// Updates container & update time status once server results arrive
		schrodinger.serverResultFuture.foreachSuccess { descriptionRoles =>
			container.current = descriptionRoles
			updateTimesContainer(typeName) = Now
		}
		
		schrodinger
	}
	
	/**
	  * Resets the cache logic so that data will be requested again the next time it is needed
	  */
	def resetUpdateStatus() = updateTimesContainer.current = Model.empty
}
