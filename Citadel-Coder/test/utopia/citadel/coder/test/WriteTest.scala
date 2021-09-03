package utopia.citadel.coder.test

import utopia.citadel.coder.controller.writer.{AccessWriter, DbModelWriter, FactoryWriter, ModelWriter, SqlWriter, TablesWriter}
import utopia.citadel.coder.model.data.{Class, ProjectSetup, Property}
import utopia.citadel.coder.model.enumeration.BasicPropertyType.Text
import utopia.citadel.coder.model.enumeration.PropertyType.{ClassReference, CreationTime, Optional}
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
	val testClass = Class("Test", Vector(Property("name", Text(128), description = "Name of this test item"),
		Property("creatorId", ClassReference("user"), description = "Id of the user who added this data"),
		Property("additionalInfo", Optional(Text()), description = "Additional information about this item"),
		Property("created", CreationTime, description = "Time when this item was created")), "test",
		description = "A class used for testing code writing")
	println("Writing files...")
	SqlWriter(Vector(testClass), targetDirectory/"db_structure.sql").flatMap { _ =>
		TablesWriter(Vector(testClass)).flatMap { tablesRef =>
			ModelWriter(testClass).flatMap { case (modelRef, dataRef) =>
				FactoryWriter(testClass, tablesRef, modelRef, dataRef).flatMap { factoryRef =>
					DbModelWriter(testClass, modelRef, dataRef, factoryRef).flatMap { dbModelRef =>
						AccessWriter(testClass, modelRef, factoryRef, dbModelRef)
					}
				}
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
