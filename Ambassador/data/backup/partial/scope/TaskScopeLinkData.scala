package utopia.ambassador.model.partial.scope

import utopia.flow.time.Now

import java.time.Instant

/**
  * Contains information about a link between a task and a scope
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  * @param taskId Id of the task being described
  * @param scopeId Id of the linked scope
  * @param created Requirement creation time (default = now)
  * @param deprecatedAfter Time when this requirement was removed. None if not removed (default).
  * @param isRequired Whether this is a required scope that can't be replaced with an alternative (default = true)
  */
case class TaskScopeLinkData(taskId: Int, scopeId: Int, created: Instant = Now, deprecatedAfter: Option[Instant] = None,
                             isRequired: Boolean = true)
