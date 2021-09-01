package utopia.citadel.coder.test

import utopia.citadel.coder.controller.writer.{DbModelWriter, FactoryWriter, ModelWriter, TablesWriter}
import utopia.citadel.coder.model.data.{Class, ProjectSetup, Property}
import utopia.citadel.coder.model.enumeration.BasicPropertyType.Text
import utopia.citadel.coder.model.enumeration.PropertyType.{CreationTime, Optional}
import utopia.flow.util.FileExtensions._

import java.nio.file.Path
import scala.util.{Failure, Success}

/**
  * Tests writing class data
  * @author Mikko Hilpinen
  * @since 31.8.2021, v0.1
  */
object WriteTest extends App
{
	val targetDirectory: Path = "Citadel-Coder/data/test-output"
	implicit val setup: ProjectSetup = ProjectSetup("utopia.citadel.test", targetDirectory)
	val testClass = Class("Test", Vector(Property("name", Text()), Property("additionalInfo", Optional(Text())),
		Property("created", CreationTime)), "test")
	println("Writing files...")
	TablesWriter(Vector(testClass)).flatMap { tablesRef =>
		ModelWriter(testClass).flatMap { case (modelRef, dataRef) =>
			FactoryWriter(testClass, tablesRef, modelRef, dataRef).flatMap { factoryRef =>
				DbModelWriter(testClass, modelRef, dataRef, factoryRef)
			}
		}
	} match
	{
		case Success(_) => println("Success!")
		case Failure(exception) =>
			println("Failure!")
			exception.printStackTrace()
	}
}
