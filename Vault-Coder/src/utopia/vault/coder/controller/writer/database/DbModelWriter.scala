package utopia.vault.coder.controller.writer.database

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, ProjectSetup}
import utopia.vault.coder.model.scala
import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.{ComputedProperty, ImmutableValue}
import utopia.vault.coder.model.scala.declaration.{ClassDeclaration, File, MethodDeclaration, ObjectDeclaration, PropertyDeclaration}
import utopia.vault.coder.model.scala.{Extension, Parameter, Reference, ScalaType}
import utopia.vault.coder.util.NamingUtils

import _root_.scala.io.Codec

/**
  * Used for writing database model scala files
  * @author Mikko Hilpinen
  * @since 1.9.2021, v0.1
  */
object DbModelWriter
{
	// OTHER    -----------------------------------------
	
	/**
	  * Generates the DB model class and the associated companion object
	  * @param classToWrite The base class
	  * @param modelRef     Reference to the stored model class
	  * @param dataRef      Reference to the data class
	  * @param factoryRef   Reference to the factory class
	  * @param codec        Implicit codec used when writing the file
	  * @param setup        Target project -specific setup (implicit)
	  * @return Reference to the generated class. Failure if writing failed.
	  */
	def apply(classToWrite: Class, modelRef: Reference, dataRef: Reference, factoryRef: Reference)
	         (implicit codec: Codec, setup: ProjectSetup) =
	{
		val parentPackage = setup.dbModelPackage / classToWrite.packageName
		val className = classToWrite.name.singular + "Model"
		val deprecation = DeprecationStyle.of(classToWrite)
		
		// The generated file contains the model class and the associated companion object
		File(parentPackage,
			ObjectDeclaration(className,
				factoryExtensionsFor(className, modelRef, dataRef, deprecation),
				// Contains xAttName and xColumn for each property, as well as factory and table -properties
				properties = classToWrite.properties.flatMap { prop =>
					Vector(
						ImmutableValue(s"${ prop.name }AttName",
							description = s"Name of the property that contains ${ classToWrite.name } ${ prop.name }")(
							NamingUtils.underscoreToCamel(prop.columnName).uncapitalize.quoted),
						ComputedProperty(s"${ prop.name }Column",
							description = s"Column that contains ${ classToWrite.name } ${ prop.name }")(
							s"table(${ prop.name }AttName)")
					)
				} ++ Vector(
					ComputedProperty("factory", Set(factoryRef),
						description = "The factory object used by this model type")(factoryRef.target),
					ComputedProperty("table", isOverridden = true)("factory.table")
				) ++ deprecation.iterator.flatMap { _.properties },
				// Implements .apply(...) and .complete(id, data)
				// Also includes withX(...) methods for each property
				methods = Set(
					MethodDeclaration("apply", isOverridden = true)(Parameter("data", dataRef))(
						s"apply(${
							("None" +: classToWrite.properties.map { prop =>
								if (prop.dataType.isNullable) s"data.${ prop.name }" else s"Some(data.${ prop.name })"
							})
								.mkString(", ")
						})"),
					MethodDeclaration("complete", Set(modelRef), isOverridden = true)(
						Vector(Parameter("id", Reference.value), Parameter("data", dataRef)))(
						s"${ modelRef.target }(id.get${ if (classToWrite.useLongId) "Long" else "Int" }, data)"),
					MethodDeclaration("withId", returnDescription = "A model with that id")(
						Parameter("id", classToWrite.idType.toScala, description = s"A ${ classToWrite.name } id"))(
						"apply(Some(id))")
				) ++ classToWrite.properties.map { prop =>
					MethodDeclaration(s"with${ prop.name.singular.capitalize }",
						returnDescription = s"A model containing only the specified ${ prop.name }")(
						scala.Parameter(prop.name.singular, prop.dataType.notNull.toScala, description = prop.description))(
						s"apply(${ prop.name } = Some(${ prop.name }))")
				} ++ deprecation.iterator.flatMap { _.methods },
				description = s"Used for constructing $className instances and for inserting ${
					classToWrite.name
				}s to the database", author = classToWrite.author
			),
			ClassDeclaration(className,
				// Accepts a copy of all properties where each is wrapped in option (unless already an option)
				Parameter("id", classToWrite.idType.nullable.toScala, "None",
					description = s"${ classToWrite.name } database id") +:
					classToWrite.properties.map { prop =>
						val inputType = prop.dataType.nullable
						val defaultValue = inputType.baseDefault.notEmpty.getOrElse(CodePiece("None"))
						Parameter(prop.name.singular, inputType.toScala, defaultValue, description = prop.description)
					},
				// Extends StorableWithFactory[A]
				Vector(Reference.storableWithFactory(modelRef)),
				// Implements the required properties: factory & valueProperties
				properties = Vector(
					ComputedProperty("factory", isOverridden = true)(s"$className.factory"),
					valuePropertiesPropertyFor(classToWrite, className)
				),
				// adds withX(...) -methods for convenience
				methods = classToWrite.properties.map { prop =>
					MethodDeclaration(s"with${ prop.name.singular.capitalize }",
						returnDescription = s"A new copy of this model with the specified ${ prop.name }")(
						Parameter(prop.name.singular, prop.dataType.notNull.toScala,
							description = s"A new ${ prop.name }"))(
						s"copy(${ prop.name } = Some(${ prop.name }))")
				}.toSet,
				description = s"Used for interacting with ${ classToWrite.name.plural } in the database",
				author = classToWrite.author, isCaseClass = true)
		).write()
	}
	
	private def factoryExtensionsFor(className: String, modelRef: Reference, dataRef: Reference,
	                                 deprecation: Option[DeprecationStyle]): Vector[Extension] =
	{
		// The class itself doesn't need to be imported (same file)
		val classType = ScalaType.basic(className)
		// All factories extend the DataInserter trait
		val dataInserter = Reference.dataInserter(classType, modelRef, dataRef)
		// They may also extend a deprecation-related trait
		deprecation match {
			case Some(deprecation) => Vector(dataInserter, deprecation.extensionFor(classType))
			case None => Vector(dataInserter)
		}
	}
	
	private def valuePropertiesPropertyFor(classToWrite: Class, className: String) =
	{
		if (classToWrite.properties.isEmpty)
			ComputedProperty("valueProperties", Set(Reference.valueConversions), isOverridden = true)(
				s"Vector(${ "\"id\" -> id" })"
			)
		else {
			val propsPart = classToWrite.properties
				.map { prop => prop.nullable.toValueCode.withPrefix(s"${ prop.name }AttName -> ") }
				.reduceLeft { _.append(_, ", ") }
			ComputedProperty("valueProperties", propsPart.references + Reference.valueConversions, isOverridden = true)(
				s"import $className._",
				s"Vector(${ "\"id\" -> id" }, $propsPart)"
			)
		}
	}
	
	
	// NESTED   --------------------------------------
	
	private sealed trait DeprecationStyle
	{
		def extensionFor(dbModelClass: ScalaType): Extension
		
		def properties: Vector[PropertyDeclaration]
		
		def methods: Set[MethodDeclaration]
	}
	
	private object DeprecationStyle
	{
		def of(c: Class) =
			c.deprecationProperty.map { prop => NullDeprecates(prop.name.singular) }
				.orElse { c.expirationProperty.map { prop => Expires(prop.name.singular) } }
	}
	
	private case class NullDeprecates(propertyName: String) extends DeprecationStyle
	{
		// ATTRIBUTES   -----------------------------
		
		// Whether this deprecation matches the expected default
		val isDefault = propertyName == "deprecatedAfter"
		
		
		// IMPLEMENTED  -----------------------------
		
		override def extensionFor(dbModelClass: ScalaType) =
			(if (isDefault) Reference.deprecatableAfter else Reference.nullDeprecatable) (dbModelClass)
		
		override def properties = if (isDefault) Vector() else Vector(
			ImmutableValue("deprecationAttName", isOverridden = true)(propertyName.quoted)
		)
		
		// withDeprecatedAfter(...) must be provided separately for custom property names
		override def methods = if (isDefault) Set() else Set(
			MethodDeclaration("withDeprecatedAfter", isOverridden = true)(
				Parameter("deprecationTime", Reference.instant))(s"with${ propertyName.capitalize }(deprecationTime)")
		)
	}
	
	private case class Expires(propertyName: String) extends DeprecationStyle
	{
		override def extensionFor(dbModelClass: ScalaType) = Reference.expiring
		
		override def properties = Vector(ImmutableValue("deprecationAttName", isOverridden = true)(propertyName.quoted))
		
		override def methods = Set()
	}
}
