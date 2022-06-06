package utopia.vault.coder.controller.writer.model

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Enum, ProjectSetup}
import utopia.vault.coder.model.scala.datatype.{Extension, Reference, ScalaType}
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ImmutableValue
import utopia.vault.coder.model.scala.declaration.{File, MethodDeclaration, ObjectDeclaration, PropertyDeclaration, TraitDeclaration}
import utopia.vault.coder.model.scala.{DeclarationDate, Parameter}

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
	  * @param e  Enumeration to write
	  * @param setup Project setup to use (implicit)
	  * @param codec Codec to use (implicit)
	  * @return Enum reference on success. Failure if writing failed.
	  */
	def apply(e: Enum)(implicit setup: ProjectSetup, codec: Codec) =
	{
		// Enumeration doesn't need to be imported in its own file
		val enumDataType = ScalaType.basic(e.name)
		
		File(e.packagePath,
			// Writes the enumeration trait first
			TraitDeclaration(e.name,
				// Each value contains an id so that it can be referred from the database
				properties = Vector(PropertyDeclaration.newAbstract("id", ScalaType.int,
					description = "Id used for this value in database / SQL")),
				description = s"Common trait for all ${ e.name } values", author = e.author,
				since = DeclarationDate.versionedToday, isSealed = true
			),
			// Enumeration values are nested within a companion object
			ObjectDeclaration(e.name,
				// Contains the .values -property
				properties = Vector(
					ImmutableValue("values", explicitOutputType = Some(ScalaType.vector(enumDataType)),
						description = "All available values of this enumeration")(
						s"Vector(${ e.values.mkString(", ") })")
				),
				// Contains an id to enum value -function (one with Try, another with Option)
				methods = Set(
					MethodDeclaration("findForId",
						returnDescription = s"${ e.name } matching that id. None if the id didn't match any ${ e.name }")(
						Parameter("id", ScalaType.int, description = s"Id representing a ${ e.name }"))(
						"values.find { _.id == id }"),
					MethodDeclaration("forId",
						codeReferences = Set(Reference.collectionExtensions, Reference.noSuchElementException),
						returnDescription = s"${ e.name } matching that id. Failure if no suitable value was found.")(
						Parameter("id", ScalaType.int, description = s"Id matching a ${ e.name }"))(
						s"findForId(id).toTry { new NoSuchElementException(s${
							s"No value of ${ e.name } matches id '${ "$id" }'".quoted
						}) }")
				),
				// Contains an object for each value
				nested = e.values.zipWithIndex.map { case (valueName, index) =>
					ObjectDeclaration(valueName, Vector(Extension(enumDataType)),
						// The objects don't contain other properties except for 'id'
						properties = Vector(ImmutableValue("id", isOverridden = true)(s"${ index + 1 }")),
						isCaseObject = true
					)
				}.toSet
			)
		).write()
	}
}
