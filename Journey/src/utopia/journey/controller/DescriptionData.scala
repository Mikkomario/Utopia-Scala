package utopia.journey.controller

import java.time.Instant

import utopia.annex.controller.QueueSystem
import utopia.flow.container.SaveTiming.Delayed
import utopia.flow.container.{ModelFileContainer, ObjectsFileContainer}
import utopia.flow.util.FileExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.journey.util.JourneyContext._
import utopia.metropolis.model.combined.language.DescribedLanguage

/**
  * An access point used for retrieving description-related data that is shared between all user accounts
  * (languages, user role descriptions, etc.)
  * @author Mikko Hilpinen
  * @since 19.7.2020, v1
  */
class DescriptionData(currentQueueSystem: => QueueSystem)
{
	// ATTRIBUTES	-------------------------
	
	private val updateTimesContainer = new ModelFileContainer(containersDirectory/"data-updates.json",
		Delayed(5.minutes))
	
	private val languagesContainer = new ObjectsFileContainer(containersDirectory/"languages.json", DescribedLanguage)
	
	
	// COMPUTED	-----------------------------
	
	// TODO: I guess update times should be user-specific since used languages may vary
	private def lastLanguageUpdate = updateTimesContainer.current("language").instantOr(Instant.EPOCH)
}
