package utopia.echo.controller.vastai

import utopia.annex.model.response.{RequestFailure, RequestResult, Response}
import utopia.annex.util.RequestResultExtensions._
import utopia.echo.controller.client.VastAiApiClient
import utopia.echo.model.request.vastai.{DestroyInstance, ShowInstance}
import utopia.echo.model.vastai.process.VastAiProcessState
import utopia.echo.model.vastai.process.VastAiProcessState._
import utopia.echo.model.vastai.instance.{LiveInstance, VastAiInstance}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.ShutdownReaction.SkipDelay
import utopia.flow.async.process.{Delay, Process}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Duration, Now}
import utopia.flow.util.logging.Logger
import utopia.flow.util.result.TryExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.AssignableOnce
import utopia.flow.view.template.eventful.{Changing, Flag}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

object VastAiProcess
{
	/**
	 * Creates a new process for managing Vast AI.
	 * Note: This process is not started automatically.
	 * @param statusUpdateInterval Interval between instance pointer / instance state updates.
	 *                             Default = 10 seconds.
	 * @param maxConsecutiveStatusCheckFailures Maximum number of consecutive GET instance request failures,
	 *                                          the instance is automatically destroyed.
	 *                                          Default = None = No limit on request failures.
	 * @param acquireInstance A function called when this process starts.
	 *                        Accepts a flag that is set to true if stop() is called for this process.
	 *                        Acquires an instance (ID) to use.
	 *                        May yield a failure, in which case this process terminates in state [[Failed]].
	 * @param exc Implicit execution context
	 * @param log Implicit logging implementation
	 * @param client Implicit Vast AI client interface
	 * @return A new process
	 */
	def apply(statusUpdateInterval: Duration = 10.seconds, maxConsecutiveStatusCheckFailures: Option[Int] = None)
	         (acquireInstance: Flag => Future[Try[Int]])
	         (implicit exc: ExecutionContext, log: Logger, client: VastAiApiClient) =
		new VastAiProcess(statusUpdateInterval, maxConsecutiveStatusCheckFailures)(acquireInstance)
}

/**
 * Represents the process of temporarily renting and utilizing a machine / GPU.
 * This process is single-use only. Unless a failure is encountered, it will remain active until [[stop]] is called.
 * @param statusUpdateInterval Interval between instance pointer updates
 * @param maxConsecutiveStatusCheckFailures Maximum number of consecutive GET instance request failures,
 *                                          the instance is automatically destroyed.
 *                                          Default = None = No limit on request failures.
 * @param acquireInstance A function called when this process starts.
 *                        Accepts a flag that is set to true if stop() is called for this process.
 *                        Yields the ID of the instance to use.
 *                        May yield a failure, in which case this process terminates in state [[Failed]].
 * @author Mikko Hilpinen
 * @since 25.02.2026, v1.5
 */
// TODO: Add support for stopping the instance instead of destroying it
class VastAiProcess(statusUpdateInterval: Duration = 10.seconds, maxConsecutiveStatusCheckFailures: Option[Int] = None)
                   (acquireInstance: Flag => Future[Try[Int]])
                   (implicit exc: ExecutionContext, log: Logger, client: VastAiApiClient)
	extends Process(shutdownReaction = Some(SkipDelay))
{
	// ATTRIBUTES   -------------------------
	
	override protected val isRestartable: Boolean = true
	/**
	 * Contains a timestamp of when this process was/is started.
	 * Mutated exactly once.
	 */
	private var startTime = Now.toInstant
	
	private val _stateP = Volatile.lockable[VastAiProcessState](NotStarted)
	/**
	 * A pointer that contains the [[VastAiProcessState]] of this process
	 */
	val detailedStatePointer = _stateP.readOnly
	
	private val instanceIdFutureP = AssignableOnce[Future[Try[Int]]]()
	private val instancePointerP = AssignableOnce[Try[Changing[VastAiInstance]]]()
	
	/**
	 * A future that resolves into the ID of the utilized instance. Yields a failure if no instance could be acquired.
	 */
	lazy val instanceIdFuture = instanceIdFutureP.future.flatten
	/**
	 * A future that resolves into a pointer that contains the latest instance state.
	 * Yields a failure if no instance could be acquired,
	 * or if [[stop]] was called while the instance was being acquired.
	 */
	val instancePointerFuture = instancePointerP.future
	/**
	 * A future that resolves into a regularly updated instance once it becomes available.
	 * Yields a failure if no instance could be acquired,
	 * or if [[stop]] was called while the instance was being acquired.
	 */
	lazy val liveInstanceFuture =
		instancePointerFuture.mapSuccess { new LiveInstance(_, detailedStatePointer, startTime) }
	
	
	// INITIAL CODE -------------------------
	
	registerToStopOnceJVMCloses()
	
	
	// COMPUTED -----------------------------
	
	/**
	 * @return ID of the managed instance.
	 *         None if no instance is or was managed.
	 */
	def instanceId = instanceIdFutureP.value.flatMap { _.currentResult.flatMap { _.toOption } }
	
	/**
	 * @return The current detailed state of this process
	 */
	def detailedState = detailedStatePointer.value
	
	
	// IMPLEMENTED  -------------------------
	
	override protected def runOnce(): Unit = {
		println("Vast AI process starting")
		startTime = Now
		// Acquires a new instance (blocks extensively)
		_stateP.value = Starting
		instanceIdFutureP.setOne(acquireInstance(hurryFlag)).waitForResult() match {
			case Success(instanceId) =>
				println(s"Instance $instanceId selected")
				// Prepares to terminate the contract once this process completes or is requested to stop,
				// or if the instance becomes inaccessible
				val terminationStartPromise = Promise[Future[RequestResult[_]]]()
				def terminationRequested = terminationStartPromise.isCompleted
				def terminate(): Unit = this.synchronized {
					if (!terminationRequested) {
						println(s"$instanceId: Terminating the Vast AI process")
						terminationStartPromise.success(client.send(DestroyInstance(instanceId)))
						_stateP.update { previous => Stopping(previous.instanceStatus) }
					}
				}
				val terminator = ChangeListener[Boolean] { e =>
					if (e.newValue) {
						terminate()
						Detach
					}
					else
						Continue
				}
				
				// Case: Already requested to stop => Destroys the instance immediately
				if (shouldHurry) {
					terminate()
					instancePointerP.setOne(Failure(
						new IllegalStateException("Requested to stop before the instance was acquired")))
				}
				else {
					// Prepares to terminate this contract if this process is requested to stop
					hurryFlag.addListener(terminator)
					
					// Sets up the instance in the background
					// Attempts to retrieve the instance multiple times, in case of {"instances": null} result
					println(s"$instanceId: Requests initial instance information")
					client.send(ShowInstance(instanceId, deprecationView = View { terminationRequested }))
						.forResult {
							// Case: Instance acquired => Prepares it for use and starts monitoring it
							case Response.Success(instance, _, _) =>
								println(s"$instanceId: Initial instance version loaded")
								val instanceP = Volatile.lockable(instance)
								instancePointerP.setOne(Success(instanceP.readOnly))
								_stateP.value = {
									if (terminationRequested)
										Stopping(Some(instance.status))
									else
										Running(instance.status)
								}
								// Once monitoring completes, locks the instance pointer
								println(s"$instanceId: Starts monitoring the instance status")
								monitorInstance(instanceId, instanceP)(terminate).onComplete { result =>
									instanceP.lock()
									result.logWithMessage("Unexpected failure during the monitoring process")
								}
							
							// Case: Failed to acquire the initial instance => Proceeds to destroy the instance
							case failure: RequestFailure =>
								println(s"$instanceId: Failed to acquire the initial instance version")
								instancePointerP.setOne(failure.toFailure)
								terminate()
						}
				}
				
				// Waits for the contract to terminate
				println(s"$instanceId: Waiting until terminated")
				terminationStartPromise.future.flatten.waitForResult() match {
					case _: Response.Success[_] =>
						println(s"$instanceId: Terminated")
						_stateP.value = Terminated
					case failure: RequestFailure =>
						println(s"$instanceId: Termination failed")
						_stateP.update { previousState => Failed(failure.cause, previousState, Some(instanceId)) }
				}
				hurryFlag.removeListener(terminator)
				_stateP.lock()
			
			// Case: Renting failed => Completes immediately
			case Failure(error) =>
				_stateP.value = Failed(error, Starting)
				instancePointerP.setOne(Failure(error))
		}
	}
	
	
	// OTHER    ------------------------------
	
	private def monitorInstance(instanceId: Int, instanceP: Pointer[VastAiInstance], previousFailures: Int = 0)
	                           (terminate: () => Unit): Future[Unit] =
	{
		// Waits for a while, then requests the latest instance status
		Delay
			.apply(statusUpdateInterval) {
				// Case: Finished during the delay => Won't perform a request
				if (state.isFinal) {
					println(s"$instanceId: Stops monitoring")
					Future.successful(None)
				}
				// Case: Not yet finished => Checks the instance's status
				else
					client.send(ShowInstance(instanceId)).map { Some(_) }
			}
			.flatten
			.flatMap {
				// Case: No further monitoring was necessary
				case None => Future.unit
				
				// Case: Status successfully acquired => Updates the state & instance pointers, if appropriate
				case Some(Response.Success(instance, _, _)) =>
					instanceP.value = instance
					// Case: Should continue monitoring => Schedules the next iteration
					if (_stateP.tryUpdate { _.withInstanceStatus(instance.status) })
						monitorInstance(instanceId, instanceP)(terminate)
					// Case: Terminated => Finishes this loop
					else {
						println(s"$instanceId: Terminated => Finishes monitoring")
						Future.unit
					}
				
				// Case: Request failed => Checks how many consecutive failures there has been
				case _ =>
					val consecutiveFailures = previousFailures + 1
					println(s"$instanceId: Instance check failed (#$consecutiveFailures)")
					// Case: Too many consecutive failures => Requests termination
					if (maxConsecutiveStatusCheckFailures.exists { _ <= consecutiveFailures }) {
						println(s"$instanceId: Too many failures => Requests termination")
						terminate()
					}
					
					// Continues monitoring until the instance is actually terminated
					monitorInstance(instanceId, instanceP, consecutiveFailures)(terminate)
			}
	}
}
