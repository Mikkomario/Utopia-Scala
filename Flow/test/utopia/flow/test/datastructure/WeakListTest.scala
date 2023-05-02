package utopia.flow.test.datastructure

import utopia.flow.async.context.ThreadPool
import utopia.flow.async.process.Wait
import utopia.flow.collection.immutable.WeakList
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.concurrent.ExecutionContext

/**
 * Tests WeakList use
 * @author Mikko Hilpinen
 * @since 3.4.2019
 */
object WeakListTest extends App
{
	implicit val logger: Logger = SysErrLogger
	private implicit val exc: ExecutionContext = new ThreadPool("Test")
	
	val first = "First"
	val second = "Second"
	
	val weakList = WeakList(first, second)
	// implicit val cbf = WeakList.canBuildFrom[String, String]
	
	val mapped = weakList.map { _.head.toString } // (WeakList.canBuildFrom[String, Char])
	
	println(mapped.reduce { _ + ", " + _ })
	
	def addItem(item: String, toList: WeakList[String]) =
	{
		println(s"$item to $toList")
		val newList = toList :+ item
		println(newList.reduceOption { _ + ", " + _ })
		newList
	}
	
	val weak2 = addItem("Moi", weakList)
	
	Wait(5.seconds)
	println(weak2.reduceOption { _ + ", " + _ })
}
