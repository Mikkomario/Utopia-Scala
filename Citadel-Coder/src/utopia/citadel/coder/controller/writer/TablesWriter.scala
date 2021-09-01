package utopia.citadel.coder.controller.writer

import utopia.citadel.coder.model.data.{Class, ProjectSetup}
import utopia.citadel.coder.model.scala.PropertyDeclarationType.ComputedProperty
import utopia.citadel.coder.model.scala.Visibility.Private
import utopia.citadel.coder.model.scala.{File, MethodDeclaration, ObjectDeclaration, Parameter, Reference, ScalaType}
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
		File(s"$parentPath.$objectName", objects = Vector(
			ObjectDeclaration(objectName,
				// Contains a computed property for each class / table
				properties = classes.toVector.sortBy { _.name }
					.map { c => ComputedProperty(c.name.uncapitalize)(s"apply(${c.tableName.quoted})") },
				// Uses a private apply method implementation that refers to the Citadel Tables instance
				methods = Set(MethodDeclaration("apply", Set(Reference.citadelTables), Private)(
					Parameter("tableName", ScalaType.string))("Tables(tableName)")))))
			.writeTo(setup.sourceRoot/"database"/s"$objectName.scala")
			.map { _ => Reference(parentPath, objectName) }
	}
}
