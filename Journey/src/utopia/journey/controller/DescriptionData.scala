package utopia.journey.controller

import java.time.{Instant, Period}

import utopia.annex.controller.QueueSystem
import utopia.annex.model.request.GetRequest
import utopia.annex.model.schrodinger.{CachedFindSchrodinger, CompletedSchrodinger}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.container.SaveTiming.Delayed
import utopia.flow.container.{ModelFileContainer, ObjectsFileContainer}
import utopia.flow.datastructure.immutable.Model
import utopia.flow.util.FileExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.flow.generic.ValueConversions._
import utopia.journey.util.JourneyContext._
import utopia.metropolis.model.combined.language.DescribedLanguage

import scala.concurrent.duration.Duration

/**
  * An access point used for retrieving description-related data that is shared between all user accounts
  * (languages, user role descriptions, etc.)
  * @author Mikko Hilpinen
  * @since 19.7.2020, v1
  */
class DescriptionData(currentQueueSystem: => QueueSystem, defaultUpdatePeriod: Period = 3.days,
					  requestTimeout: Duration = 10.seconds)
{
	// ATTRIBUTES	-------------------------
	
	private val updateTimesContainer = new ModelFileContainer(containersDirectory/"data-updates.json",
		Delayed(5.minutes))
	
	private val languagesContainer = new ObjectsFileContainer(containersDirectory/"languages.json", DescribedLanguage)
	// TODO: Implement description role handling and take that into account when parsing other descriptions as well
	// TODO: It may be that descriptionRole can't be an enumeration anymore...
	// private val descriptionRolesContainer = new ObjectsFileContainer(containersDirectory/"description-roles.json",
	//	DescribedDescriptionRole)
	
	
	// COMPUTED	-----------------------------
	
	private def lastLanguageUpdate = updateTimesContainer("language").instantOr(Instant.EPOCH)
	
	def languages =
	{
		// May request an update in the background
		if (lastLanguageUpdate < Instant.now() - defaultUpdatePeriod)
			updateLanguages()
		else
		{
			val localData = languagesContainer.current
			CompletedSchrodinger.success(localData)
		}
	}
	
	
	// OTHER	---------------------------
	
	def updateLanguages() =
	{
		val localData = languagesContainer.current
		val requestTime = Instant.now()
		
		val schrodinger = new CachedFindSchrodinger(localData)
		schrodinger.completeWith(currentQueueSystem.push(GetRequest("languages",
			localData.isEmpty && Instant.now() > requestTime + requestTimeout))) {
			_.vector(DescribedLanguage).parsed } { log(_) }
		// Caches server results when they arrive
		schrodinger.serverResultFuture.foreachSuccess { languages =>
			languagesContainer.current = languages
			updateTimesContainer("language") = Instant.now()
		}
		
		schrodinger
	}
	
	def resetUpdateStatus() = updateTimesContainer.current = Model.empty
}
