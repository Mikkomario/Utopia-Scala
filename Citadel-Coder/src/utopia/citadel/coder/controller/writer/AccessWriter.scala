package utopia.citadel.coder.controller.writer

import utopia.citadel.coder.model.data.{Class, ProjectSetup}
import utopia.citadel.coder.model.scala.PropertyDeclarationType.ComputedProperty
import utopia.citadel.coder.model.scala.Visibility.Private
import utopia.citadel.coder.model.scala.{ClassDeclaration, File, MethodDeclaration, ObjectDeclaration, Parameter, Reference, ScalaType, TraitDeclaration}
import utopia.flow.util.FileExtensions._
import utopia.flow.util.StringExtensions._

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
		val accessPackage = setup.projectPackage + ".database.access"
		val accessDirectory = setup.sourceRoot/"database/access"
		val connectionParam = Parameter("connection", Reference.connection)
		// Writes a trait common for unique model access points
		val singleAccessPackage =  s"$accessPackage.single.${classToWrite.packageName}"
		val singleAccessDirectory = accessDirectory/"single"/classToWrite.packageName
		val uniqueAccessName = s"Unique${classToWrite.name}Access"
		// Standard access point properties (factory, model & defaultOrdering)
		// are the same for both single and many model access points
		// (except that defaultOrdering is missing from non-distinct access points)
		val baseProperties = Vector(
			ComputedProperty("factory", Set(factoryRef), isOverridden = true)(factoryRef.target),
			ComputedProperty("model", Set(dbModelRef), Private)(dbModelRef.target)
		)
		val distinctAccessBaseProperties = baseProperties :+
			ComputedProperty("defaultOrdering", Set(factoryRef), isOverridden = true)(
				if (classToWrite.recordsCreationTime) "Some(factory.defaultOrdering)" else "None")
		// Property setters are common for both distinct access points (unique & many)
		val propertySetters = classToWrite.properties.map { prop =>
			MethodDeclaration(s"${prop.name}_=")(
				Parameter(s"new${prop.name.capitalize}", prop.dataType.notNull.toScala).withImplicits(connectionParam))(
				s"putColumn(model.${prop.name}Column, new${prop.name.capitalize})")
		}.toSet
		File(singleAccessPackage,
			TraitDeclaration(uniqueAccessName,
				// Extends SingleRowModelAccess, DistinctModelAccess and Indexed
				Vector(Reference.singleRowModelAccess(modelRef),
					Reference.distinctModelAccess(modelRef, ScalaType.option(modelRef), Reference.value),
					Reference.indexed),
				// Provides computed accessors for individual properties
				distinctAccessBaseProperties ++ classToWrite.properties.map { prop =>
					ComputedProperty(prop.name, implicitParams = Vector(connectionParam))(
						s"pullColumn(model.${prop.name}Column).${prop.dataType.notNull.toScala.toScala.uncapitalize}")
				} :+ ComputedProperty("id", implicitParams = Vector(connectionParam))(
					s"pullColumn(index).${classToWrite.idType.toScala.toScala.uncapitalize}"),
				propertySetters
			)
		).writeTo(singleAccessDirectory/s"$uniqueAccessName.scala").flatMap { _ =>
			val uniqueAccessRef = Reference(singleAccessPackage, uniqueAccessName)
			
			// Writes the single model access point
			val singleAccessName = s"Db${classToWrite.name}"
			val singleIdAccessName = s"DbSingle${classToWrite.name}"
			// This access point is used for accessing individual items based on their id
			val singleIdAccess = ClassDeclaration(singleIdAccessName,
				Vector(Parameter("id", classToWrite.idType.toScala, prefix = "override val")),
				Vector(uniqueAccessRef, Reference.uniqueModelAccess(modelRef)),
				// Implements the .condition property
				properties = Vector(
					ComputedProperty("condition", Set(Reference.valueConversions, Reference.sqlExtensions),
						isOverridden = true)("index <=> id")
				)
			)
			File(singleAccessPackage,
				ObjectDeclaration(singleAccessName,
					Vector(Reference.singleRowModelAccess(modelRef), Reference.unconditionalView, Reference.indexed),
					properties = baseProperties,
					// Defines an .apply(id) method for accessing individual items
					methods = Set(MethodDeclaration("apply")(Parameter("id", classToWrite.idType.toScala))(
						s"new $singleIdAccessName(id)")),
					nested = Set(singleIdAccess)
				)
			).writeTo(singleAccessDirectory/s"$singleAccessName.scala").flatMap { _ =>
				// Writes a trait common for the many model access points
				val manyAccessPackage =  s"$accessPackage.many.${classToWrite.packageName}"
				val manyAccessDirectory = accessDirectory/"many"/classToWrite.packageName
				val manyAccessTraitName = s"Many${classToWrite.name}Access"
				File(manyAccessPackage,
					TraitDeclaration(manyAccessTraitName,
						Vector(Reference.manyRowModelAccess(modelRef), Reference.indexed),
						// TODO: It's probably better to add support for plural property names
						// Contains computed properties to access class properties
						distinctAccessBaseProperties ++ classToWrite.properties.map { prop =>
							ComputedProperty(s"${prop.name}s", implicitParams = Vector(connectionParam))(
								s"pullColumn(model.${prop.name}Column).flatMap { _.${
									prop.dataType.notNull.toScala.toScala.uncapitalize} }")
						} :+ ComputedProperty("ids", implicitParams = Vector(connectionParam))(
							s"pullColumn(index).flatMap { _.${classToWrite.idType.toScala.toScala.uncapitalize} }"),
						propertySetters
					)
				).writeTo(manyAccessDirectory/s"$manyAccessTraitName.scala").flatMap { _ =>
					val manyAccessTraitRef = Reference(manyAccessPackage, manyAccessTraitName)
					
					// Writes the many model access point
					val manyAccessName = s"Db${classToWrite.name}s"
					File(manyAccessPackage,
						ObjectDeclaration(manyAccessName, Vector(manyAccessTraitRef, Reference.unconditionalView))
					).writeTo(manyAccessDirectory/s"$manyAccessName.scala")
						.map { _ => (uniqueAccessRef, Reference(singleAccessPackage, singleAccessName),
							manyAccessTraitRef, Reference(manyAccessPackage, manyAccessName))
						}
				}
			}
		}
	}
}
