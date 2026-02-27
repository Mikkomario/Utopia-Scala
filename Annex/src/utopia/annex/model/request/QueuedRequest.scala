package utopia.annex.model.request

import utopia.annex.model.response.RequestResult
import utopia.flow.async.context.ActionQueue.QueuedAction
import utopia.flow.time.Now

import java.time.Instant

/**
 * A request wrapper that combines the request with the queued execution & eventual result (futures)
 * @param request The wrapped request or request seed
 * @param result A queued action of the request's execution
 * @param queueTime Timestamp of when this request was added to the request queue
 * @author Mikko Hilpinen
 * @since 27.02.2026, v1.12
 */
case class QueuedRequest[+A](request: RequestQueueable[A], result: QueuedAction[RequestResult[A]],
                             queueTime: Instant = Now)