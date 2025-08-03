package utopia.vault.database

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.{Breakable, Wait}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.operator.ordering.CombinedOrdering
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.mutable.Settable
import utopia.flow.view.mutable.async.Volatile
import utopia.vault.model.error.NoConnectionException

import java.time.Instant
import scala.collection.immutable.VectorBuilder
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

/**
  * This pool is used for creating and reusing database connections
  * @author Mikko Hilpinen
  * @since 7.5.2019, v1.1
 *  @param maxConnections Maximum number of simultaneous database connections (default = 100)
 *  @param maxClientsPerConnection Maximum number of functions sharing a single database connection (default = 6)
 *  @param connectionKeepAlive How long connections are kept open after they are last used. Open connections are
 *                             reused if another client requests a connection within that time period. (default = 15 seconds)
  * @param connectionValidationInterval The minimum time interval between connection validations
  *                                     (when they are acquired via this pool).
  *                                     Default = 15 seconds.
  * @param exc Execution context used in automated connection closing
  */
class ConnectionPool(maxConnections: Int = 100, maxClientsPerConnection: Int = 6,
					 val connectionKeepAlive: FiniteDuration = 15.seconds,
					 connectionValidationInterval: Duration = 15.seconds)
                    (implicit exc: ExecutionContext)
	extends Breakable
{
	// ATTRIBUTES	----------------------
	
	private implicit val log: Logger = SysErrLogger
	
	private val validatesConnections = connectionValidationInterval.isFinite
	private val validationInterval = connectionValidationInterval.finite.getOrElse(Duration.Zero)
	
	private val connections = Volatile.seq[ReusableConnection]()
	private val waitLock = new AnyRef()
	private val timeoutCompletion = Volatile(Future.successful[Any](()))
	private val closeFutures = Volatile.seq[Future[Unit]]()
	
	private val maxClientThresholds = {
		var currentMax = 1
		var start = 0
		
		// Uses halving algorithm to find the thresholds (for example getting 0 to 100: 50, 75, 87, 93, 96, 98, 99)
		val buffer = new VectorBuilder[(Int, Int)]()
		while (currentMax < maxClientsPerConnection - 1) {
			val length = (maxConnections - start) / 2
			if (length > 0) {
				buffer += (start + length -> currentMax)
				currentMax += 1
				start += length
			}
			else
				currentMax = maxClientsPerConnection
		}
		
		buffer += (maxConnections -> maxClientsPerConnection)
		buffer.result()
	}
	
	
	// INITIAL CODE	-----------------------
	
	registerToStopOnceJVMCloses()
	
	
	// COMPUTED	---------------------------
	
	/**
	  * Finds a joinable connection, or starts a new one
	  * @return A usable connection
	  */
	private def connection = connections.mutate { all =>
		// Sorts the connections by availability
		val sorted = all.sorted
		lazy val maxClientCount = maxClientsPerConnectionFor(all.size)
		
		// Finds the first joinable connection
		sorted.find { _.tryJoin(maxClientCount) } match {
			// Case: Joined an existing connection
			case Some(joinedConnection) => joinedConnection -> sorted
			// Case: No connection could be joined to => Creates a new connection
			case None =>
				val newConnection = new ReusableConnection()
				newConnection -> (newConnection +: sorted)
		}
	}
	
	
	// IMPLEMENTED	-----------------------
	
	override def stop() = {
		// Closes all current connections (may have to wait for clients to exit)
		(connections.map { c: ReusableConnection => c.stop() } ++ closeFutures).futureCompletion
	}
	
	
	// OPERATORS	-----------------------
	
	/**
	  * Performs an operation using a database connection. The function must not use the connection after its completion.
	  * Please use tryWith(...) if you wish to catch the (likely) exceptions thrown by the provided function
	  * @param f The function that will be run
	  * @tparam B Result type
	  * @return Function results
	  */
	def apply[B](f: Connection => B) = connection.doAndLeave(f)
	
	
	// OTHER	---------------------------
	
	/**
	  * Performs an operation using a database connection. The function must not use the connection after its completion.
	  * Catches any exceptions
	  * @param f The function that will be run
	  * @tparam B Result type
	  * @return Function results, wrapped in a try
	  */
	def tryWith[B](f: Connection => B) = connection.tryAndLeave(f)
	/**
	  * Performs an operation using a database connection.
	  * The specified function must not use the connection after it has completed
	  * (i.e. not make asynchronous uses or form long-term references).
	  * Catches and logs any encountered exceptions.
	  * @param f A function for utilizing the acquired connection
	  * @param log Implicit logging implementation for logging any encountered exception
	  * @tparam B Type of function result
	  * @return The function result. None if the function threw an exception.
	  */
	def logging[B](f: Connection => B)(implicit log: Logger) = tryWith(f).log
	
	// Finds the first treshold that hasn't been reached and uses that connection amount
	private def maxClientsPerConnectionFor(openConnectionCount: Int) =
		(maxClientThresholds.find { _._1 >= openConnectionCount } getOrElse maxClientThresholds.last)._2
	
	/**
	  * Makes sure there's an active background process for closing idle connections
	  */
	private def closeUnusedConnections() = timeoutCompletion.update { old =>
		// Will not create another future if one is active already
		if (old.isCompleted) {
			Future {
				OptionsIterator
					.iterate(Some(Now + connectionKeepAlive)) { waitTarget =>
						val uninterrupted = Wait(waitTarget, waitLock)
						// Updates connection list and determines next close time
						val (nextWaitTarget, newCloseFutures) = connections.mutate { all =>
							// Keeps connections that are still open
							val (closing, keepOpen) = {
								// Case: Standard state => Keeps connections alive for a specific time
								if (uninterrupted)
									all.divideBy { _.shouldBeKeptOpen }
								// Case: Interrupted / scheduled to close => closes as many connections as possible
								else
									all.divideBy { _.isInUse }
							}.toTuple
							val closeFutures = closing.map { _.tryClose() }
							val lastLeaveTime = keepOpen.iterator.filterNot { _.isInUse }
								.map { _.lastLeaveTime }.minOption
							
							(lastLeaveTime.map { _ + connectionKeepAlive }, closeFutures) -> keepOpen
						}
						// Keeps track of thread closing futures in order to delay a possible system exit
						closeFutures.update { _.filterNot { _.isCompleted } ++ newCloseFutures }
						
						nextWaitTarget
					}
					.foreach { _ => () }
			}
		}
		else
			old
	}
	
	
	// NESTED CLASSES	-------------------
	
	private object ReusableConnection
	{
		/**
		  * An ordering that:
		  *         1. Places closed connections at the end
		  *         2. places first the connections with the least amount of clients.
		  *         3. Within these, sorts by connection creation time (descending)
		  */
		implicit val ord: Ordering[ReusableConnection] = CombinedOrdering[ReusableConnection](
			Ordering.by { _.closed.isSet },
			Ordering.by { _.currentClientCount },
			Ordering.by[ReusableConnection, Instant] { _.created }.reverse
		)
	}
	private class ReusableConnection extends Breakable
	{
		// ATTRIBUTES	-------------------
		
		private val created = Now.toInstant
		
		/**
		  * Set to true once this connection is no longer accepting any new clients
		  */
		private val closed = Settable()
		private val connection = new Connection()
		private val connectionClosePromise = Promise[Unit]()
		/**
		  * The number of clients that are actively using (i.e. holding a reference) the wrapped connection
		  */
		private val clientCount = Volatile(1)
		
		/**
		  * Contains time when the last connection validation was performed
		  */
		private val lastValidationP = Volatile(Now.toInstant)
		
		private val lastLeaveTimeP = Volatile.eventful(Now.toInstant)
		private val autoCloseTimeP = lastLeaveTimeP.map { _ + connectionKeepAlive }
		
		
		// COMPUTED	-----------------------
		
		def lastLeaveTime = lastLeaveTimeP.value
		def currentClientCount = clientCount.value
		
		def isInUse = currentClientCount > 0
		def shouldBeKeptOpen = isInUse || Now < autoCloseTimeP.value
		
		
		// IMPLEMENTED	-------------------
		
		override def stop() = tryClose()
		
		
		// OTHER	-----------------------
		
		def tryAndLeave[B](f: Connection => B) = Try { doAndLeave(f) }
		def doAndLeave[B](f: Connection => B) = {
			try {
				// Before passing the connection to the specified function, may make sure its usable
				if (validateIfNecessary())
					f(connection)
				else
					throw new NoConnectionException("Database connection was invalid and couldn't be reset")
			}
			finally { leave() }
		}
		
		/**
		  * Attempts to attach a new client to this connection
		  * @param currentMaxCapacity Currently allowed maximum number of clients per connection
		  * @return Whether a new client was registered to this connection
		  *         (indicating that the caller should continue with this connection)
		  */
		def tryJoin(currentMaxCapacity: Int) = clientCount.mutate { currentCount =>
			// Case: At maximum capacity or already closing => Won't allow new users
			if (currentCount >= currentMaxCapacity || closed.isSet)
				false -> currentCount
			// Case: Has capacity => Allows joining
			else
				true -> (currentCount + 1)
		}
		
		private def leave(): Unit = {
			lastLeaveTimeP.value = Now
			// Updates the client counter
			val becameEmpty = clientCount.mutate { currentClientCount =>
				(currentClientCount == 1) -> (currentClientCount - 1)
			}
			// Case: This connection is now idle
			if (becameEmpty) {
				// Case: This connection was queued to close => Closes the wrapped connection
				if (closed.isSet)
					closeConnection()
				// Otherwise requests a connection-clearing, if necessary
				else
					closeUnusedConnections()
			}
		}
		
		/**
		  * Makes this connection close once it's no longer used
		  * @return Future that resolves once this connection has closed
		  */
		def tryClose() = {
			if (closed.set())
				clientCount.lockWhile { count => if (count <= 0) closeConnection() }
			connectionClosePromise.future
		}
		private def closeConnection(): Unit = {
			connectionClosePromise.synchronized {
				if (!connectionClosePromise.isCompleted) {
					connection.close()
					connectionClosePromise.success(())
				}
			}
		}
		
		/**
		  * Validates the wrapped connection. Skips this process if validated recently.
		  * @return Whether the wrapped connection is usable.
		  */
		private def validateIfNecessary() = {
			if (validatesConnections) {
				// Checks whether validation should be performed
				val shouldValidate = lastValidationP.mutate { lastValidation =>
					// Case: It's been some time since the last validation => Queues a new one
					if (lastValidation < Now - validationInterval)
						true -> Now
					// Case: Last validation was recent => Skips validation
					else
						false -> lastValidation
				}
				if (shouldValidate) {
					// Validates the connection
					// Case: Valid
					if (connection.validate(2))
						true
					// Case: Invalid => Prepares to close this connection after the current client leaves
					else {
						closed.set()
						false
					}
				}
				// Case: Validation was not necessary
				else
					true
			}
			// Case: Connection-validation is disabled => Assumes that the connection is OK
			else
				true
		}
	}
}
