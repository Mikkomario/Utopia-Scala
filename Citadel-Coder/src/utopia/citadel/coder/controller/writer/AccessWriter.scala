package utopia.citadel.coder.controller.writer

import utopia.citadel.coder.model.data.{Class, ProjectSetup}
import utopia.citadel.coder.model.scala.PropertyDeclarationType.ComputedProperty
import utopia.citadel.coder.model.scala.Visibility.Private
import utopia.citadel.coder.model.scala.{ClassDeclaration, Extension, File, MethodDeclaration, ObjectDeclaration, Parameter, Reference, ScalaType, TraitDeclaration}
import utopia.flow.util.FileExtensions._

import scala.io.Codec

/**
  * Used for writing database access templates
  * @author Mikko Hilpinen
  * @since 2.9.2021, v0.1
  */
object AccessWriter
{
	def apply(classToWrite: Class, modelRef: Reference, factoryRef: Reference, dbModelRef: Reference)
	         (implicit codec: Codec, setup: ProjectSetup) =
	{
		val accessPackage = setup.projectPackage + ".database.access"
		val accessDirectory = setup.sourceRoot/"database/access"
		// Writes a trait common for unique model access points
		val singleAccessPackage =  s"$accessPackage.single.${classToWrite.packageName}"
		val singleAccessDirectory = accessDirectory/"single"/classToWrite.packageName
		val uniqueAccessName = s"Unique${classToWrite.name}Access"
		File(singleAccessPackage,
			TraitDeclaration(uniqueAccessName,
				// Extends SingleRowModelAccess, DistinctModelAccess and Indexed
				Vector(Reference.singleRowModelAccess(modelRef),
					Reference.distinctModelAccess(modelRef, ScalaType.option(modelRef), Reference.value),
					Reference.indexed),
				// Implements .factory and .defaultOrdering. Adds .model for utility
				// and provides computed accessors for individual properties
				Vector(
					ComputedProperty("factory", Set(factoryRef), isOverridden = true)(factoryRef.target),
					ComputedProperty("model", Set(dbModelRef), Private)(dbModelRef.target),
					ComputedProperty("defaultOrdering", Set(factoryRef), isOverridden = true)(
						if (classToWrite.recordsCreationTime) "factory.defaultOrdering" else "None")
				) ++ classToWrite.properties.map { prop =>
					ComputedProperty(prop.name)(
						s"pullColumn(model.${prop.name}Column).${prop.dataType.notNull.toScala}")
				} :+ ComputedProperty("id")(s"index.${classToWrite.idType.toScala}"),
				// Provides setters to defined properties
				classToWrite.properties.map { prop => MethodDeclaration(s"${prop.name}_=")(
					Parameter(s"new${prop.name.capitalize}", prop.dataType.notNull.toScala))(
					s"putColumn(model.${prop.name}Column, new${prop.name.capitalize})") }.toSet
			)
		).writeTo(singleAccessDirectory/s"$uniqueAccessName.scala")
		val uniqueAccessRef = Reference(singleAccessPackage, uniqueAccessName)
		
		// Writes the single model access point
		val singleAccessName = s"Db${classToWrite.name}"
		val singleIdAccessName = s"DbSingle${classToWrite.name}"
		val singleIdAccess = ClassDeclaration(singleIdAccessName,
			Vector(Parameter("id", classToWrite.idType.toScala)),
			Vector(
				Extension(Reference.singleIdModelAccess(modelRef), Vector("id", s"$singleAccessName.factory"),
					Set(Reference.valueConversions)),
				uniqueAccessRef
			)
		)
		File(singleAccessPackage,
			ObjectDeclaration(singleAccessName,
				Vector(Reference.singleRowModelAccess(modelRef), Reference.unconditionalView, Reference.indexed),
				properties = Vector(
					ComputedProperty("factory", Set(factoryRef), isOverridden = true)(factoryRef.target),
					ComputedProperty("model", Set(dbModelRef), visibility = Private)(dbModelRef.target)
				),
				methods = Set(MethodDeclaration("apply")(Parameter("id", classToWrite.idType.toScala))(
					s"new $singleIdAccessName(id)")),
				nested = Set(singleIdAccess)
			)
		).writeTo(singleAccessDirectory/s"$singleAccessName.scala")
	}
}
