package utopia.vault.coder.controller.writer

import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, ProjectSetup}
import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.{Parameter, Reference, declaration}
import utopia.vault.coder.model.scala.declaration.{ClassDeclaration, File}
import utopia.vault.coder.util.NamingUtils

import scala.io.Codec

/**
  * Used for writing model data from class data
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
object ModelWriter
{
	/**
	  * Writes stored and partial model classes for a class template
	  * @param classToWrite class being written
	  * @param codec Implicit codec used when writing files (implicit)
	  * @param setup Target project -specific settings (implicit)
	  * @return Reference to the stored version, followed by a reference to the data version. Failure if writing failed.
	  */
	def apply(classToWrite: Class)(implicit codec: Codec, setup: ProjectSetup) =
	{
		val dataClassName = classToWrite.name.singular + "Data"
		val dataClassPackage = setup.modelPackage/s"partial.${classToWrite.packageName}"
		val propertyModelWrites = classToWrite.properties.map { prop =>
			prop.toValueCode.withPrefix(NamingUtils.camelToUnderscore(prop.name.singular).quoted + " -> ")
		}
		val propertyModelCode = if (propertyModelWrites.isEmpty) "Model.empty" else
			s"Model(Vector(${ propertyModelWrites.mkString(", ") }))"
		
		// Writes the data model
		File(dataClassPackage, Vector(
			declaration.ClassDeclaration(dataClassName,
				// Accepts a copy of each property. Uses default values where possible.
				classToWrite.properties.map { prop =>
					val defaultValueCode = prop.customDefault match
					{
						case "" => prop.dataType.baseDefault
						case defined => CodePiece(defined)
					}
					Parameter(prop.name.singular, prop.dataType.toScala, defaultValueCode,
						description = prop.description)
				},
				// Extends ModelConvertible
				Vector(Reference.modelConvertible),
				// Implements the toModel -property
				properties = deprecationPropertiesFor(classToWrite) :+
					ComputedProperty("toModel", propertyModelWrites.flatMap { _.references }.toSet + Reference.model,
						isOverridden = true)(propertyModelCode),
				description = classToWrite.description, author = classToWrite.author,
				isCaseClass = true)
		)).write().flatMap { dataClassRef =>
			val storePackage = setup.modelPackage/s"stored.${classToWrite.packageName}"
			// Writes the stored model next
			val storedClass =
			{
				val idType = classToWrite.idType.toScala
				// Accepts id and data -parameters
				val constructionParams = Vector(
					Parameter("id", idType, description = s"id of this ${classToWrite.name} in the database"),
					Parameter("data", dataClassRef, description = s"Wrapped ${classToWrite.name} data")
				)
				val description = s"Represents a ${classToWrite.name} that has already been stored in the database"
				// ModelConvertible extension & implementation differs based on id type
				if (classToWrite.useLongId)
					ClassDeclaration(classToWrite.name.singular, constructionParams,
						Vector(Reference.stored(dataClassRef, idType)),
						properties = Vector(
							ComputedProperty("toModel", Set(Reference.valueConversions, Reference.constant),
								isOverridden = true)("Constant(\"id\", id) + data.toModel")
						), description = description, isCaseClass = true)
				else
					declaration.ClassDeclaration(classToWrite.name.singular, constructionParams,
						Vector(Reference.storedModelConvertible(dataClassRef)),
						description = description, author = classToWrite.author, isCaseClass = true)
			}
			File(storePackage, Vector(storedClass)).write().map { _ -> dataClassRef }
		}
	}
	
	// Deprecation-supporting classes can have custom properties
	private def deprecationPropertiesFor(classToWrite: Class) =
	{
		classToWrite.deprecationProperty match
		{
			case Some(prop) =>
				Vector(
					ComputedProperty("isDeprecated",
						description = s"Whether this ${classToWrite.name} has already been deprecated")(
						s"${prop.name}.isDefined"),
					ComputedProperty("isValid",
						description = s"Whether this ${classToWrite.name} is still valid (not deprecated)")(
						"!isDeprecated")
				)
			case None =>
				classToWrite.expirationProperty match
				{
					case Some(prop) =>
						Vector(
							ComputedProperty("hasExpired", Set(Reference.timeExtensions, Reference.now),
								description = s"Whether this ${
									classToWrite.name} is no longer valid because it has expired")(
								s"${prop.name} <= Now"),
							ComputedProperty("isValid",
								description = s"Whether this ${classToWrite.name} is still valid (hasn't expired yet)")(
								"!hasExpired")
						)
					case None => Vector()
				}
		}
	}
}
