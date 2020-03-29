package utopia.flow.filesearch

import java.nio.file.Path

import utopia.flow.async.AsyncExtensions._

import scala.concurrent.{ExecutionContext, Future}

/**
 * A group of miners that can then split into individual workers
 * @author Mikko Hilpinen
 * @since 17.12.2019, v1.6.1+
 */
class Entourage[R](origin: Mine[R], size: Int, startingPath: Vector[Mine[R]] = Vector())
			   (private val search: Path => R) extends Explorer(origin, startingPath)
{
	// ATTRIBUTES	-------------------
	
	private var deployedMinerCount = 0
	private var sentCompletions: Vector[Future[Any]] = Vector()
	
	
	// IMPLEMENTED	-------------------
	
	// Moves forward in the mines until a split is found
	override def explore()(implicit exc: ExecutionContext) =
	{
		// TODO: Shouldn't this be asynchronous as well?
		while (move()) { }
		sentCompletions.future
	}
	
	
	// OTHER	-----------------------
	
	private def move()(implicit exc: ExecutionContext): Boolean =
	{
		// Finds a passage that hasn't been started yet
		if (findUnexploredRoot())
		{
			// Checks whether this entourage should split or move on as a single unit
			// In case of a dead-end, leaves a single miner behind while the rest backtrack
			val pathsToExplore = currentLocation.unexploredPaths
			if (pathsToExplore.isEmpty)
			{
				if (deploySingleLocationMiner())
					backtrack()
				else
					false
			}
			// If there's only a single way to go, the entourage moves there
			else if (pathsToExplore.size == 1)
			{
				goDeeper(pathsToExplore.head)
				true
			}
			// if there are multiple possible pathways, splits the entourage between the pathways
			else
			{
				// Reserves all remaining miners
				val minersToSend = size - deployedMinerCount
				
				// There may be too few miners to send between different paths, in which case all miners are sent individually
				if (minersToSend <= pathsToExplore.size)
				{
					pathsToExplore.take(minersToSend).foreach { deployMinerUnder }
					deployedMinerCount = size
				}
				// If there are more miners than pathways, deploys them as sub-groups (where available)
				else
				{
					val minMinersPerPath = minersToSend / pathsToExplore.size
					val extraMinersCount = minersToSend % pathsToExplore.size
					
					val (pathsWithMoreMiners, pathsWithLessMiners) = pathsToExplore.splitAt(extraMinersCount)
					pathsWithMoreMiners.foreach { path => deployEntourageUnder(path, minMinersPerPath + 1) }
					
					if (minMinersPerPath > 1)
						pathsWithLessMiners.foreach { path => deployEntourageUnder(path, minMinersPerPath) }
					else
						pathsWithLessMiners.foreach { deployMinerUnder }
					
					deployedMinerCount = size
				}
				false
			}
		}
		else
			false
	}
	
	private def deploySingleLocationMiner()(implicit exc: ExecutionContext) =
	{
		// Deploys the miner to current location
		sentCompletions :+= new Miner(origin, currentRoute)(search).mineCurrentLocationThenExplore()
		deployedMinerCount += 1
		deployedMinerCount < size
	}
	
	private def deployMinerUnder(path: Mine[R])(implicit exc: ExecutionContext) =
		sentCompletions :+= new Miner(origin, currentRoute :+ path)(search).explore()
	
	private def deployEntourageUnder(path: Mine[R], size: Int)(implicit exc: ExecutionContext) =
		sentCompletions :+= new Entourage(origin, size, currentRoute :+ path)(search).explore()
}
