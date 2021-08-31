package utopia.citadel.coder.test

import utopia.citadel.coder.controller.writer.ModelWriter
import utopia.citadel.coder.model.data.{Class, Property}
import utopia.citadel.coder.model.enumeration.BasicPropertyType.Text
import utopia.citadel.coder.model.enumeration.PropertyType.{CreationTime, Optional}
import utopia.flow.util.FileExtensions._

import java.nio.file.Path

/**
  * Tests writing class data
  * @author Mikko Hilpinen
  * @since 31.8.2021, v0.1
  */
object WriteTest extends App
{
	val targetDirectory: Path = "Citadel-Coder/data/test-output"
	val testClass = Class("Test", Vector(Property("name", Text()), Property("additionalInfo", Optional(Text())),
		Property("created", CreationTime)), "test")
	ModelWriter(targetDirectory, "utopia.citadel.test", testClass).get
}
