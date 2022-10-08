package utopia.flow.test.generic

import utopia.flow.generic.casting.ConversionHandler
import utopia.flow.generic.model.mutable.DataType

/**
  * Prints all data type conversion routes that are used
  * @author Mikko Hilpinen
  * @since 9.8.2022, v1.16
  */
object ConversionRoutesTest extends App
{
	
	
	val types = DataType.values.toVector.sortBy { _.name }
	
	types.foreach { target =>
		println(s"\n${target.name.toUpperCase} \t---------------")
		types.filterNot { _.isOfType(target) }
			.flatMap { origin => ConversionHandler.conversionRouteBetween(origin, target)
				.map { route => route -> route.foldLeft(0) { _ + _.cost } } }
			.filterNot { _._1.size == 1 }
			.sortBy { _._2 }
			.foreach { case (route, cost) =>
				println(s"\t${route.head.source.name} ${ route.map { r => s"=> ${r.target.name}" }.mkString(" ") } ($cost)")
			}
	}
}
