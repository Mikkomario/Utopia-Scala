package utopia.vault.coder.controller.writer

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Enum, ProjectSetup}
import utopia.vault.coder.model.scala.{Extension, Parameter, Reference, ScalaType}
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ImmutableValue
import utopia.vault.coder.model.scala.declaration.{File, MethodDeclaration, ObjectDeclaration, PropertyDeclaration, TraitDeclaration}

import scala.io.Codec

/**
  * Used for writing enumeration files
  * @author Mikko Hilpinen
  * @since 25.9.2021, v1.1
  */
object EnumerationWriter
{
	/**
	  * Writes an enumeration as a scala file
	  * @param enum Enumeration to write
	  * @param setup Project setup to use (implicit)
	  * @param codec Codec to use (implicit)
	  * @return Enum reference on success. Failure if writing failed.
	  */
	def apply(enum: Enum)(implicit setup: ProjectSetup, codec: Codec) =
	{
		// Enumeration doesn't need to be imported in its own file
		val enumDataType = ScalaType.basic(enum.name)
		
		File(enum.packagePath,
			// Writes the enumeration trait first
			TraitDeclaration(enum.name,
				// Each value contains an id so that it can be referred from the database
				properties = Vector(PropertyDeclaration.newAbstract("id", ScalaType.int,
					description = "Id used for this value in database / SQL")),
				description = s"Common trait for all ${enum.name} values", author = enum.author,
				isSealed = true
			),
			// Enumeration values are nested within a companion object
			ObjectDeclaration(enum.name,
				// Contains the .values -property
				properties = Vector(
					ImmutableValue("values", explicitOutputType = Some(ScalaType.vector(enumDataType)),
						description = "All available values of this enumeration")(
						s"Vector(${enum.values.mkString(", ")})")
				),
				// Contains an id to enum value -function (one with Try, another with Option)
				methods = Set(
					MethodDeclaration("findForId",
						returnDescription = s"${enum.name} matching that id. None if the id didn't match any ${enum.name}")(
						Parameter("id", ScalaType.int, description = s"Id representing a ${enum.name}"))(
						"values.find { _.id == id }"),
					MethodDeclaration("forId",
						codeReferences = Set(Reference.collectionExtensions, Reference.noSuchElementException),
						returnDescription = s"${enum.name} matching that id. Failure if no suitable value was found.")(
						Parameter("id", ScalaType.int, description = s"Id matching a ${enum.name}"))(
						s"findForId(id).toTry { new NoSuchElementException(s${
							s"No value of ${enum.name} matches id '${"${id}"}'".quoted}) }")
				),
				// Contains an object for each value
				nested = enum.values.zipWithIndex.map { case (valueName, index) =>
					ObjectDeclaration(valueName, Vector(Extension(enumDataType)),
						// The objects don't contain other properties except for 'id'
						properties = Vector(ImmutableValue("id", isOverridden = true)(s"${index + 1}")),
						isCaseObject = true
					)
				}.toSet
			)
		).write()
	}
}
