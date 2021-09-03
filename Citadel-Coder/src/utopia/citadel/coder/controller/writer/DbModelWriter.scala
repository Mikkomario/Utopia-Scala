package utopia.citadel.coder.controller.writer

import utopia.citadel.coder.model.data.{Class, ProjectSetup}
import utopia.citadel.coder.model.enumeration.PropertyType.Optional
import utopia.citadel.coder.model.scala.declaration.PropertyDeclarationType.{ComputedProperty, ImmutableValue}
import utopia.citadel.coder.model.scala.declaration.{ClassDeclaration, File, MethodDeclaration, ObjectDeclaration}
import utopia.citadel.coder.model.scala.{Parameter, Reference, ScalaType}
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
		File(parentPackage,
			ObjectDeclaration(className,
				// Extends the DataInserter trait
				Vector(Reference.dataInserter(ScalaType.basic(className), modelRef, dataRef)),
				// Contains xAttName and xColumn for each property, as well as factory and table -properties
				properties = classToWrite.properties.flatMap { prop => Vector(
					ImmutableValue(s"${prop.name}AttName",
						description = s"Name of the property that contains ${classToWrite.name} ${prop.name}")(
						prop.name.quoted),
					ComputedProperty(s"${prop.name}Column",
						description = s"Column that contains ${classToWrite.name} ${prop.name}")(
						s"table(${prop.name}AttName)")
				) } ++ Vector(
					ComputedProperty("factory", Set(factoryRef),
						description = "The factory object used by this model type")(factoryRef.target),
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
						Vector(Parameter("id", Reference.value), Parameter("data", dataRef)))(
						s"${modelRef.target}(id.get${if (classToWrite.useLongId) "Long" else "Int"}, data)"),
					MethodDeclaration("withId", returnDescription = "A model with that id")(
						Parameter("id", classToWrite.idType.toScala, description = s"A ${classToWrite.name} id"))(
						"apply(Some(id))")
				) ++ classToWrite.properties.map { prop =>
					MethodDeclaration(s"with${prop.name.capitalize}",
						returnDescription = s"A model containing only the specified ${prop.name}")(
						Parameter(prop.name, prop.dataType.notNull.toScala, description = prop.description))(
						s"apply(${prop.name} = Some(${prop.name}))")
				},
				description = s"Used for constructing $className instances and for inserting ${
					classToWrite.name}s to the database"
			),
			ClassDeclaration(className,
				// Accepts a copy of all properties where each is wrapped in option (unless already an option)
				Parameter("id", Optional(classToWrite.idType).toScala, "None", s"${classToWrite.name} database id") +:
					classToWrite.properties.map { prop => Parameter(prop.name,
						if (prop.dataType.isNullable) prop.dataType.toScala else ScalaType.option(prop.dataType.toScala),
						"None", description = prop.description) },
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
					MethodDeclaration(s"with${prop.name.capitalize}",
						returnDescription = s"A new copy of this model with the specified ${prop.name}")(
						Parameter(prop.name, prop.dataType.notNull.toScala, description = s"A new ${prop.name}"))(
						s"copy(${prop.name} = Some(${prop.name}))")
				}.toSet,
				description = s"Used for interacting with ${classToWrite.name} instances in the database",
				isCaseClass = true)
		).writeTo(setup.sourceRoot/"database/model"/classToWrite.packageName/s"$className.scala")
			.map { _ => Reference(parentPackage, className) }
	}
}
