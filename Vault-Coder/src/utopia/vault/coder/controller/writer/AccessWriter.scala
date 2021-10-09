package utopia.vault.coder.controller.writer

import utopia.vault.coder.model.scala.Visibility.Protected
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, ProjectSetup}
import utopia.vault.coder.model.scala.{Extension, Parameter, Reference, ScalaType}
import utopia.vault.coder.model.scala.declaration.{ClassDeclaration, File, MethodDeclaration, ObjectDeclaration, TraitDeclaration}

import scala.io.Codec

/**
  * Used for writing database access templates
  * @author Mikko Hilpinen
  * @since 2.9.2021, v0.1
  */
object AccessWriter
{
	/**
	  * Writes database access point objects and traits
	  * @param classToWrite Class based on which these access points are written
	  * @param modelRef Reference to the stored model class
	  * @param factoryRef Reference to the from DB factory object
	  * @param dbModelRef Reference to the database model class
	  * @param codec Implicit codec to use when writing files
	  * @param setup Implicit project-specific setup to use
	  * @return References to<br>
	  *         1) trait common for distinct single model access points<br>
	  *         2) Single model access point object<br>
	  *         3) Trait common for many model access points<br>
	  *         4) Many model access point object
	  */
	def apply(classToWrite: Class, modelRef: Reference, factoryRef: Reference, dbModelRef: Reference)
	         (implicit codec: Codec, setup: ProjectSetup) =
	{
		val connectionParam = Parameter("connection", Reference.connection)
		// Writes a trait common for unique model access points
		val singleAccessPackage =  setup.singleAccessPackage/classToWrite.packageName
		val uniqueAccessName = s"Unique${classToWrite.name}Access"
		// Standard access point properties (factory, model & defaultOrdering)
		// are the same for both single and many model access points
		// (except that defaultOrdering is missing from non-distinct access points)
		val baseProperties = Vector(
			ComputedProperty("factory", Set(factoryRef), isOverridden = true)(factoryRef.target),
			ComputedProperty("model", Set(dbModelRef), Protected,
				description = "Factory used for constructing database the interaction models")(dbModelRef.target)
		)
		// Property setters are common for both distinct access points (unique & many)
		val propertySetters = classToWrite.properties.map { prop =>
			val paramName = s"new${prop.name.singular.capitalize}"
			val paramType = prop.dataType.notNull
			val valueConversionCode = paramType.toValueCode(paramName)
			MethodDeclaration(s"${prop.name}_=", valueConversionCode.references,
				description = s"Updates the ${prop.name} of the targeted ${classToWrite.name} instance(s)",
				returnDescription = s"Whether any ${classToWrite.name} instance was affected")(
				Parameter(paramName, paramType.toScala, description = s"A new ${prop.name} to assign")
					.withImplicits(connectionParam))(s"putColumn(model.${prop.name}Column, $valueConversionCode)")
		}.toSet
		val pullIdCode = classToWrite.idType.nullable.fromValueCode(s"pullColumn(index)")
		File(singleAccessPackage,
			TraitDeclaration(uniqueAccessName,
				// Extends SingleRowModelAccess, DistinctModelAccess and Indexed
				Vector(Reference.singleRowModelAccess(modelRef),
					Reference.distinctModelAccess(modelRef, ScalaType.option(modelRef), Reference.value),
					Reference.indexed),
				// Provides computed accessors for individual properties
				baseProperties ++ classToWrite.properties.map { prop =>
					val pullCode = prop.dataType.nullable
						.fromValueCode(s"pullColumn(model.${prop.name}Column)")
					ComputedProperty(prop.name.singular, pullCode.references,
						description = prop.description.notEmpty.getOrElse(s"The ${prop.name} of this instance") +
							". None if no instance (or value) was found.", implicitParams = Vector(connectionParam))(
						pullCode.text)
				} :+ ComputedProperty("id", pullIdCode.references, implicitParams = Vector(connectionParam))(
					pullIdCode.text),
				propertySetters,
				description = s"A common trait for access points that return individual and distinct ${
					classToWrite.name.plural}.", author = classToWrite.author
			)
		).write().flatMap { uniqueAccessRef =>
			// Writes the single model access point
			val singleAccessName = s"Db${classToWrite.name}"
			val singleIdAccessName = s"DbSingle${classToWrite.name}"
			// Root access points extend either the UnconditionalView or the NonDeprecatedView -trait,
			// depending on whether deprecation is supported
			val rootViewExtension: Extension = {
				if (classToWrite.isDeprecatable)
					Reference.nonDeprecatedView(modelRef)
				else
					Reference.unconditionalView
			}
			// This access point is used for accessing individual items based on their id
			val singleIdAccess = ClassDeclaration(singleIdAccessName,
				Vector(Parameter("id", classToWrite.idType.toScala, prefix = "val")),
				Vector(uniqueAccessRef, Reference.uniqueModelAccess(modelRef)),
				// Implements the .condition property
				properties = Vector(
					ComputedProperty("condition", Set(Reference.valueConversions), isOverridden = true)("index <=> id")
				)
			)
			File(singleAccessPackage,
				ObjectDeclaration(singleAccessName,
					Vector(Reference.singleRowModelAccess(modelRef), rootViewExtension, Reference.indexed),
					properties = baseProperties,
					// Defines an .apply(id) method for accessing individual items
					methods = Set(MethodDeclaration("apply",
						returnDescription = s"An access point to that ${classToWrite.name}")(
						Parameter("id", classToWrite.idType.toScala,
							description = s"Database id of the targeted ${classToWrite.name} instance"))(
						s"new $singleIdAccessName(id)")),
					nested = Set(singleIdAccess),
					description = s"Used for accessing individual ${classToWrite.name.plural}",
					author = classToWrite.author
				)
			).write().flatMap { singleAccessRef =>
				// Writes a trait common for the many model access points
				val manyAccessPackage =  setup.manyAccessPackage/classToWrite.packageName
				val manyAccessTraitName = s"Many${classToWrite.name.plural}Access"
				File(manyAccessPackage,
					TraitDeclaration(manyAccessTraitName,
						Vector(Reference.manyRowModelAccess(modelRef), Reference.indexed),
						// Contains computed properties to access class properties
						baseProperties ++ classToWrite.properties.map { prop =>
							ComputedProperty(prop.name.plural,
								description = s"${prop.name.plural} of the accessible ${classToWrite.name.plural}",
								implicitParams = Vector(connectionParam))(
								s"pullColumn(model.${prop.name}Column).flatMap { value => ${
									prop.dataType.nullable.fromValueCode("value")} }")
						} :+ ComputedProperty("ids", implicitParams = Vector(connectionParam))(
							s"pullColumn(index).flatMap { id => ${
								classToWrite.idType.nullable.fromValueCode("id")} }") :+
							ComputedProperty("defaultOrdering", Set(factoryRef), Protected, isOverridden = true)(
								if (classToWrite.recordsCreationTime) "Some(factory.defaultOrdering)" else "None"),
						propertySetters,
						description = s"A common trait for access points which target multiple ${
							classToWrite.name.plural} at a time", author = classToWrite.author
					)
				).write().flatMap { manyAccessTraitRef =>
					// Writes the many model access point
					val manyAccessName = s"Db${classToWrite.name.plural}"
					File(manyAccessPackage,
						ObjectDeclaration(manyAccessName, Vector(manyAccessTraitRef, rootViewExtension),
							description = s"The root access point when targeting multiple ${
								classToWrite.name.plural} at a time", author = classToWrite.author
						)
					).write().map { manyAccessRef =>
						(uniqueAccessRef, singleAccessRef, manyAccessTraitRef, manyAccessRef)
					}
				}
			}
		}
	}
}
