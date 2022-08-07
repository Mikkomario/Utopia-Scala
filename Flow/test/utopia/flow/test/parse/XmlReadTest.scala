package utopia.flow.test.parse

import utopia.flow.generic.DataType
import utopia.flow.generic.ValueConversions._
import utopia.flow.parse.{XmlElement, XmlReader}
import utopia.flow.time.Now
import utopia.flow.util.FileExtensions._
import utopia.flow.util.StringFrom

import java.nio.charset.StandardCharsets

/**
  * Tests xml reading / parsing
  * @author Mikko Hilpinen
  * @since 31.7.2022, v1.16
  */
object XmlReadTest extends App
{
	DataType.setup()
	
	println(StringFrom.path("Flow/test/test.xml", StandardCharsets.UTF_8).get.takeWhile { _ != '<' }.length)
	val xml = XmlReader.parseFile("Flow/test/test.xml").get
	println(xml.toXml)
	/*
	val xml2 = xml.mutableCopy()
	val order = xml2/"FuelFlightLeg"/"Fuel"/"FuelOrder"
	order.name.namespace.use { implicit ns =>
		order += XmlElement("Acknowledgement", children = Vector(
			XmlElement("DateTime", Now.toValue),
			XmlElement("Acknowledged", true)
		))
	}
	println(xml2.toXml)
	
	val mapped = xml.mapPath("FuelFlightLeg", "Fuel", "FuelOrder") { order =>
		order + XmlElement("Acknowledgement", children = Vector(
			XmlElement("DateTime", Now.toValue),
			XmlElement("Acknowledged", true)
		))
	}
	println(mapped.toXml)*/
}
