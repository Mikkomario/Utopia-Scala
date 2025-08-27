package utopia.scribe.core.model.combined.management

import utopia.scribe.core.model.stored.management.{IssueNotification, Resolution}

/**
 * Includes comment and notification information to a Resolution, where applicable
 *
 * @author Mikko Hilpinen
 * @since 26.08.2025, v1.2
 */
case class DetailedResolution(resolution: Resolution, text: String, notification: Option[IssueNotification])
	extends ResolutionWithText with CombinedResolution[DetailedResolution]
{
	override protected def wrap(factory: Resolution): DetailedResolution = copy(resolution = factory)
}