package utopia.flow.test.parse

import utopia.flow.generic.DataType
import utopia.flow.parse.XmlReader
import utopia.flow.util.FileExtensions._
import utopia.flow.operator.EqualsExtensions._

import scala.util.{Failure, Success}

/**
  * Tests Xml Reader
  * @author Mikko Hilpinen
  * @since 18.8.2022, v1.16
  */
object XmlReaderTest extends App
{
	DataType.setup()
	
	val result = XmlReader.readFile("Flow/test/test.xml") { reader =>
		reader.toNextElementWithName("PREPRINTYEAR")
		reader.readElement().flatMap { _.value.int } match {
			case Some(year) =>
				println(year)
				// NB: Already at PREPRINTDELIVERYNOTICE, causing this to fail (design problem in the reader itself)
				reader.toNextElementWithName("PREPRINTEDDELIVERYNOTICE")
				reader.toNextElementWithName("DELIVERYNOTE")
				val numbers = Iterator
					.continually {
						if (reader.currentElementName.exists { _ ~== "DELIVERYNOTE" }) {
							println("Reading delivery note")
							reader.readElement()
						}
						else
							None
					}
					.takeWhile { _.isDefined }.flatten
					.flatMap { _.value.long }
					.toVector.sorted
				Success(numbers)
			case None => Failure(new NoSuchElementException("No PREPRINTYEAR"))
		}
	}
	
	println(result.get)
	
	assert(result.get.size > 10)
}
