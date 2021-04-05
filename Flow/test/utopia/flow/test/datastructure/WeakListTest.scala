package utopia.flow.test.datastructure

import utopia.flow.collection.WeakList
import utopia.flow.time.WaitUtils
import utopia.flow.time.TimeExtensions._

/**
 * Tests WeakList use
 * @author Mikko Hilpinen
 * @since 3.4.2019
 */
object WeakListTest extends App
{
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
	
	WaitUtils.wait(5.seconds, new AnyRef())
	println(weak2.reduceOption { _ + ", " + _ })
}
