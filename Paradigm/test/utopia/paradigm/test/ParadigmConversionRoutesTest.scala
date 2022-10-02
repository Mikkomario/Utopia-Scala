package utopia.paradigm.test

import utopia.flow.generic.casting.ConversionHandler
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.generic.ParadigmDataType

/**
  * Prints more complex conversion routes when Paradigm data types are in use
  * @author Mikko Hilpinen
  * @since 9.8.2022, v1.0
  */
object ParadigmConversionRoutesTest extends App
{
	ParadigmDataType.setup()
	
	val types = DataType.values.toVector.sortBy { _.name }
	
	val start = Now.toInstant
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
	println()
	println((Now - start).description)
}
