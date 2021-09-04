package utopia.citadel.coder.controller.writer

import utopia.citadel.coder.model.data.{Class, ProjectSetup}
import utopia.citadel.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.citadel.coder.model.scala.Visibility.Private
import utopia.citadel.coder.model.scala.declaration.{File, MethodDeclaration, ObjectDeclaration}
import utopia.citadel.coder.model.scala.{Parameter, Reference, ScalaType}
import utopia.flow.util.FileExtensions._
import utopia.flow.util.StringExtensions._

import scala.io.Codec

/**
  * Used for writing the tables -file
  * @author Mikko Hilpinen
  * @since 1.9.2021, v0.1
  */
object TablesWriter
{
	/**
	  * Writes the table reference file
	  * @param classes Classes introduced in this project
	  * @param codec Codec to use when writing the file (implicit)
	  * @param setup Target project -specific settings (implicit)
	  * @return Reference to the written object. Failure if writing failed.
	  */
	def apply(classes: Iterable[Class])(implicit codec: Codec, setup: ProjectSetup) =
	{
		val parentPath = s"${setup.projectPackage}.database"
		val objectName = setup.projectPackage.afterLast(".").capitalize + "Tables"
		File(s"$parentPath.$objectName",
			ObjectDeclaration(objectName,
				// Contains a computed property for each class / table
				properties = classes.toVector.sortBy { _.name }.map { c =>
					val baseDescription = s"Table that contains ${c.name.plural}"
					val completeDescription = if (c.description.isEmpty) baseDescription else
						s"$baseDescription (${c.description})"
					ComputedProperty(c.name.singular.uncapitalize, description = completeDescription)(
						s"apply(${c.tableName.quoted})")
				},
				// Uses a private apply method implementation that refers to the Citadel Tables instance
				methods = Set(MethodDeclaration("apply", Set(Reference.citadelTables), Private)(
					Parameter("tableName", ScalaType.string))("Tables(tableName)")),
				description = "Used for accessing the database tables introduced in this project"
			)
		).writeTo(setup.sourceRoot/"database"/s"$objectName.scala").map { _ => Reference(parentPath, objectName) }
	}
}
