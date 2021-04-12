package utopia.flow.async

import utopia.flow.util.CollectionExtensions.RichTry
import utopia.flow.time.WaitTarget
import utopia.flow.time.WaitTarget.Until
import utopia.flow.util.RichComparable._

import scala.util.Try

/**
  * Combines multiple loops and runs them in the same thread. Please note that <b>SynchronizedLoops will discard any
  * loop that cannot provide a specific (finite) wait duration</b>. Loops that require notifications are ignored.
  * In general, the loop trigger notifications will be ignored altogether.
  * @author Mikko Hilpinen
  * @since 3.4.2020, v1.7
  * @constructor Creates a new synchronized loop by combining the specified loops
  * @param loops The loops to synchronize together. <b>None of the loops should be started at this point</b>.
  */
class SynchronizedLoops(loops: IterableOnce[Loop]) extends Loop
{
	// ATTRIBUTES	-----------------------
	
	// Gets a specific end time for all loops and orders them
	// Will ignore loops that cannot give a specific end time
	private var orderedLoops = loops.iterator.filterNot { _.hasStarted }.map { l => l -> l.nextWaitTarget.endTime }
		.filterNot { _._2.isEmpty }.map { case (loop, target) => loop -> target.get }.toVector.sortBy { _._2 }
	
	
	// IMEPLEMENTED	-----------------------
	
	// Waits first by default
	override def run(): Unit = super.run(waitFirst = true)
	
	override def runOnce() =
	{
		// Performs the next task in the ordered loops (unless empty)
		if (orderedLoops.isEmpty)
			stop()
		else
		{
			val nextTask = orderedLoops.head._1
			// Catches any thrown exceptions and prints them
			Try { nextTask.runOnce() }.failure.foreach { _.printStackTrace() }
			
			// Determines the next time this task will be run (if it will be run)
			nextTask.nextWaitTarget.endTime match
			{
				case Some(nextTime) =>
					if (orderedLoops.size > 1)
					{
						val beforeNext = orderedLoops.tail.takeWhile { _._2 < nextTime }
						orderedLoops = (beforeNext :+ (nextTask -> nextTime)) ++ orderedLoops.drop(beforeNext.size + 1)
					}
					else
						orderedLoops = Vector(nextTask -> nextTime)
				case None =>
					orderedLoops = orderedLoops.drop(1)
			}
		}
	}
	
	override def nextWaitTarget = orderedLoops.headOption match
	{
		case Some(next) => Until(next._2)
		case None => WaitTarget.zero
	}
}
