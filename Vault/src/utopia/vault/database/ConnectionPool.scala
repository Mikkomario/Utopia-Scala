package utopia.vault.database

import java.time.Instant
import utopia.flow.async.AsyncExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.async.{Breakable, NewThreadExecutionContext, Volatile, VolatileFlag, Wait}
import utopia.flow.collection.VolatileList
import utopia.flow.time.Now
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.collection.immutable.VectorBuilder
import scala.concurrent.duration.FiniteDuration
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
  */
class ConnectionPool(maxConnections: Int = 100, maxClientsPerConnection: Int = 6,
					 val connectionKeepAlive: FiniteDuration = 15.seconds) extends Breakable
{
	// ATTRIBUTES	----------------------
	
	private val connections = VolatileList[ReusableConnection]()
	private val waitLock = new AnyRef()
	private val timeoutCompletion: Volatile[Future[Any]] = new Volatile(Future.successful(()))
	private val closeFutures = VolatileList[Future[Unit]]()
	
	private val maxClientThresholds = {
		var currentMax = 1
		var start = 0
		
		// Uses halving algorithm to find the thresholds (for example getting 0 to 100: 50, 75, 87, 93, 96, 98, 99)
		val buffer = new VectorBuilder[(Int, Int)]()
		while (currentMax < maxClientsPerConnection - 1)
		{
			val length = (maxConnections - start) / 2
			if (length > 0)
			{
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
	
	private def connection = connections.pop { all =>
		// Returns the first reusable connection, if no such connection exists, creates a new connection
		// Tries to use the connection with least clients
		val reusable = if (all.nonEmpty) Some(all.minBy { _.currentClientCount }).filter {
			_.tryJoin(maxClientPerConnectionWhen(all.size)) } else None
		
		reusable match {
			case Some(conn) => conn -> all
			case None =>
				val newConnection = new ReusableConnection()
				newConnection -> (all :+ newConnection)
		}
	}
	
	
	// IMPLEMENTED	-----------------------
	
	override def stop() = {
		// Closes all current connections (may have to wait for clients to exit)
		implicit val logger: Logger = SysErrLogger
		(connections.map { c: ReusableConnection => c.stop() } ++ closeFutures)
			.futureCompletion(new NewThreadExecutionContext("Closing connection pool"))
	}
	
	
	// OPERATORS	-----------------------
	
	/**
	  * Performs an operation using a database connection. The function must not use the connection after its completion.
	  * Please use tryWith(...) if you wish to catch the (likely) exceptions thrown by the provided function
	  * @param f The function that will be run
	  * @param context Asynchronous execution context for closing open connections afterwards
	  * @tparam B Result type
	  * @return Function results
	  */
	def apply[B](f: Connection => B)(implicit context: ExecutionContext) = connection.doAndLeave(f)
	
	
	// OTHER	---------------------------
	
	/**
	  * Performs an operation using a database connection. The function must not use the connection after its completion.
	  * Catches any exceptions
	  * @param f The function that will be run
	  * @param context Asynchronous execution context for closing open connections afterwards
	  * @tparam B Result type
	  * @return Function results, wrapped in a try
	  */
	def tryWith[B](f: Connection => B)(implicit context: ExecutionContext) = connection.tryAndLeave(f)
	
	// Finds the first treshold that hasn't been reached and uses that connection amount
	private def maxClientPerConnectionWhen(openConnectionCount: Int) =
		(maxClientThresholds.find { _._1 >= openConnectionCount } getOrElse maxClientThresholds.last)._2
	
	private def closeUnusedConnections()(implicit context: ExecutionContext) =
	{
		// Makes sure connection closing is active
		timeoutCompletion.update { old =>
			// Will not create another future if one is active already
			if (old.isCompleted) {
				Future {
					Iterator.unfold(Now + connectionKeepAlive) { waitTarget =>
						val uninterrupted = Wait(waitTarget, waitLock)
						// Updates connection list and determines next close time
						val (w, futures) = connections.pop { all =>
							// Keeps connections that are still open
							val (closing, open) = {
								// Case: Standard state => Keeps connections alive for a specific time
								if (uninterrupted)
									all.divideBy { _.isOpen }
								// Case: Interrupted / scheduled to close => closes as many connections as possible
								else
									all.divideBy { _.isInUse }
							}
							val closeFutures = closing.map { _.tryClose() }
							val lastLeaveTime = open.filterNot { _.isInUse }.map { _.lastLeaveTime }.minOption
							
							(lastLeaveTime.map { _ + connectionKeepAlive }, closeFutures) -> open
						}
						// Keeps track of thread closing futures in order to delay possible system exit
						closeFutures.update { _.filterNot { _.isCompleted } ++ futures }
						w.map { w => () -> w }
					}.foreach { _ => () }
				}
			}
			else
				old
		}
	}
	
	
	// NESTED CLASSES	-------------------
	
	private class ReusableConnection extends Breakable
	{
		// ATTRIBUTES	-------------------
		
		private val closed = new VolatileFlag()
		private val connection = new Connection()
		private val connectionClosePromise = Promise[Unit]()
		private val clientCount = new Volatile(1)
		
		private var _lastLeaveTime = Instant.now()
		
		
		// COMPUTED	-----------------------
		
		def lastLeaveTime = _lastLeaveTime
		
		def currentClientCount = clientCount.value
		
		def isInUse = currentClientCount > 0
		
		def isOpen = isInUse || (Instant.now < _lastLeaveTime + connectionKeepAlive)
		
		
		// IMPLEMENTED	-------------------
		
		override def stop() = tryClose()
		
		
		// OTHER	-----------------------
		
		def tryAndLeave[B](f: Connection => B)(implicit context: ExecutionContext) = Try(doAndLeave(f))
		
		def doAndLeave[B](f: Connection => B)(implicit context: ExecutionContext) = {
			try { f(connection) }
			finally { leave() }
		}
		
		def tryJoin(currentMaxCapacity: Int) = clientCount.pop { currentCount =>
			if (currentCount >= currentMaxCapacity || closed.isSet)
				false -> currentCount
			else
				true -> (currentCount + 1)
		}
		
		def leave()(implicit context: ExecutionContext) = {
			_lastLeaveTime = Now
			clientCount.update { currentCount =>
				if (currentCount == 1) {
					if (closed.isSet)
						closeConnection()
					else
						closeUnusedConnections()
				}
				currentCount - 1
			}
		}
		
		def tryClose() = {
			clientCount.lock { count => if (count <= 0) closeConnection() }
			closed.set()
			connectionClosePromise.future
		}
		
		private def closeConnection() = {
			connectionClosePromise.synchronized {
				if (!connectionClosePromise.isCompleted) {
					connection.close()
					connectionClosePromise.success(())
				}
			}
		}
	}
}
