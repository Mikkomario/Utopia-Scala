package utopia.citadel.coder.controller.writer

import utopia.citadel.coder.model.data.{Class, ProjectSetup}
import utopia.citadel.coder.model.enumeration.PropertyType.Optional
import utopia.citadel.coder.model.scala.PropertyDeclarationType.{ComputedProperty, ImmutableValue}
import utopia.citadel.coder.model.scala.{ClassDeclaration, File, MethodDeclaration, ObjectDeclaration, Parameter, Reference, ScalaType}
import utopia.flow.util.FileExtensions._
import utopia.flow.util.StringExtensions._

import scala.io.Codec

/**
  * Used for writing database model scala files
  * @author Mikko Hilpinen
  * @since 1.9.2021, v0.1
  */
object DbModelWriter
{
	/**
	  * Generates the DB model class and the associated companion object
	  * @param classToWrite The base class
	  * @param modelRef Reference to the stored model class
	  * @param dataRef Reference to the data class
	  * @param factoryRef Reference to the factory class
	  * @param codec Implicit codec used when writing the file
	  * @param setup Target project -specific setup (implicit)
	  * @return Reference to the generated class. Failure if writing failed.
	  */
	def apply(classToWrite: Class, modelRef: Reference, dataRef: Reference, factoryRef: Reference)
	         (implicit codec: Codec, setup: ProjectSetup) =
	{
		val parentPackage = s"${setup.projectPackage}.database.model.${classToWrite.packageName}"
		val className = classToWrite.name + "Model"
		// The generated file contains the model class and the associated companion object
		File(parentPackage, Vector(
			ClassDeclaration(className,
				// Accepts a copy of all properties where each is wrapped in option (unless already an option)
				Parameter("id", Optional(classToWrite.idType).toScala, "None") +:
					classToWrite.properties.map { prop => Parameter(prop.name,
						if (prop.dataType.isNullable) prop.dataType.toScala else ScalaType.option(prop.dataType.toScala),
						"None") },
				// Extends StorableWithFactory[A]
				Vector(Reference.storableWithFactory(modelRef)),
				// Implements the required properties: factory & valueProperties
				properties = Vector(
					ComputedProperty("factory", isOverridden = true)(s"$className.factory"),
					ComputedProperty("valueProperties", Set(Reference.valueConversions), isOverridden = true)(
						s"import $className._",
						s"Vector(${"\"id\" -> id"}, ${
							classToWrite.properties.map { prop => s"${prop.name}AttName -> ${prop.name}" }
								.mkString(", ")})")
				),
				// adds withX(...) -methods for convenience
				methods = classToWrite.properties.map { prop =>
					MethodDeclaration(s"with${prop.name.capitalize}")(
						Parameter(prop.name, prop.dataType.notNull.toScala))(
						s"copy(${prop.name} = Some(${prop.name}))")
				}.toSet, isCaseClass = true)
		), Vector(
			ObjectDeclaration(className,
				// Extends the DataInserter trait
				Vector(Reference.dataInserter(ScalaType.basic(className), modelRef, dataRef)),
				// Contains xAttName and xColumn for each property, as well as factory and table -properties
				properties = classToWrite.properties.flatMap { prop => Vector(
					ImmutableValue(s"${prop.name}AttName")(prop.name.quoted),
					ComputedProperty(s"${prop.name}Column")(s"table(${prop.name}AttName)")
				) } ++ Vector(
					ComputedProperty("factory", Set(factoryRef))(factoryRef.target),
					ComputedProperty("table", isOverridden = true)("factory.table")
				),
				// Implements .apply(...) and .complete(id, data)
				// Also includes withX(...) methods for each property
				methods = Set(
					MethodDeclaration("apply", isOverridden = true)(Parameter("data", dataRef))(
						s"apply(None, ${classToWrite.properties.map { prop =>
							if (prop.dataType.isNullable) s"data.${prop.name}" else s"Some(data.${prop.name})" }
							.mkString(", ")})"),
					MethodDeclaration("complete", Set(modelRef), isOverridden = true)(
						Parameter("id", Reference.value), Parameter("data", dataRef))(
						s"${modelRef.target}(id.get${if (classToWrite.useLongId) "Long" else "Int"}, data)"),
					MethodDeclaration("withId")(Parameter("id", classToWrite.idType.toScala))("apply(Some(id))")
				) ++ classToWrite.properties.map { prop =>
					MethodDeclaration(s"with${prop.name.capitalize}")(
						Parameter(prop.name, prop.dataType.notNull.toScala))(
						s"apply(${prop.name} = Some(${prop.name}))")
				}
			)
		)).writeTo(setup.sourceRoot/"database/model"/classToWrite.packageName/s"$className.scala")
			.map { _ => Reference(parentPackage, className) }
	}
}
