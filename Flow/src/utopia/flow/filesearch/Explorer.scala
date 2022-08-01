package utopia.flow.filesearch

import scala.concurrent.{ExecutionContext, Future}

import utopia.flow.util.CollectionExtensions._

/**
 * Explorers can traverse in the mines
 * @author Mikko Hilpinen
 * @since 17.12.2019, v1.6.1+
 */
abstract class Explorer[R](val origin: Mine[R], private var _currentRoute: Vector[Mine[R]] = Vector())
{
	// ABSTRACT	------------------------
	
	def explore()(implicit exc: ExecutionContext): Future[Any]
	
	
	// COMPUTED	------------------------
	
	def currentLocation = _currentRoute.lastOption.getOrElse(origin)
	
	def isAtOrigin = _currentRoute.isEmpty
	
	def currentRoute = _currentRoute
	
	
	// OTHER	-----------------------
	
	protected def backtrack() = {
		if (isAtOrigin)
			false
		else {
			_currentRoute = _currentRoute.dropRight(1)
			true
		}
	}
	
	protected def findDeadEnd() = {
		// May need to backtrack a little first
		if (findUnexploredRoot())
		{
			// Traverses deeper into the mine, preferring routes that haven't been explored or started yet
			while (goDeeper()) {  }
			!currentLocation.status.isStarted
		}
		else
			false
	}
	
	protected def findUnexploredRoot() = {
		// Backtracks to an incomplete passage
		while (currentLocation.status.isStarted && backtrack()) {
			// Condition moves this explorer
		}
		!currentLocation.status.isStarted
	}
	
	protected def goDeeper(): Boolean = {
		// Finds the next path, preferring those that haven't been explored yet
		currentLocation.pathWays.filter { _.isExplorable }.bestMatch { !_.status.isTraversed }.headOption match {
			case Some(nextPath) =>
				goDeeper(nextPath)
				true
			case None => false
		}
	}
	
	protected def goDeeper(path: Mine[R]) = {
		path.declareTraversed()
		_currentRoute :+= path
	}
}
