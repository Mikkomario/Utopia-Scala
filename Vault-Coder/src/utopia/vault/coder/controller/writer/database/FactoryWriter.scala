package utopia.vault.coder.controller.writer.database

import utopia.coder.model.data
import utopia.coder.model.data.NamingRules
import utopia.coder.model.enumeration.NamingConvention.CamelCase
import utopia.coder.model.scala.code.CodePiece
import utopia.coder.model.scala.datatype.{Extension, Reference}
import utopia.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.coder.model.scala.declaration.{File, ObjectDeclaration, PropertyDeclaration}
import utopia.coder.model.scala.{DeclarationDate, datatype}
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, VaultProjectSetup}
import utopia.vault.coder.util.ClassMethodFactory
import utopia.vault.coder.util.VaultReferences.Vault._

import scala.collection.immutable.VectorBuilder
import scala.io.Codec

/**
  * Used for writing standard model (from DB) factory objects
  * @author Mikko Hilpinen
  * @since 1.9.2021, v0.1
  */
object FactoryWriter
{
	/**
	  * A suffix added to class names in order to make them factory class names
	  */
	val classNameSuffix = data.Name("Factory", "Factories", CamelCase.capitalized)
	
	/**
	  * Writes a factory used for processing database object data
	  * @param classToWrite Class data based on which the factory is created
	  * @param tablesRef    Reference to the tables object
	  * @param modelRef     Reference to the read model class
	  * @param dataRef      Reference to the partial model data class
	  * @param codec        Implicit codec to use when writing the document
	  * @param setup        Implicit project-specific setup
	  * @return Reference to the new written factory object. Failure if writing failed.
	  */
	def apply(classToWrite: Class, tablesRef: Reference, modelRef: Reference, dataRef: Reference)
	         (implicit codec: Codec, setup: VaultProjectSetup, naming: NamingRules) =
	{
		val parentPackage = setup.factoryPackage / classToWrite.packageName
		val objectName = (classToWrite.name + classNameSuffix).className
		File(parentPackage,
			ObjectDeclaration(objectName, extensionsFor(classToWrite, modelRef),
				properties = propertiesFor(classToWrite, tablesRef),
				methods = methodsFor(classToWrite, modelRef, dataRef),
				description = s"Used for reading ${ classToWrite.name.doc } data from the DB",
				author = classToWrite.author, since = DeclarationDate.versionedToday
			)
		).write()
	}
	
	private def extensionsFor(classToWrite: Class, modelRef: Reference): Vector[Extension] =
	{
		val builder = new VectorBuilder[Extension]()
		
		// If no enumerations are included, the inheritance is more specific (=> uses automatic validation)
		if (classToWrite.fromDbModelConversionMayFail)
			builder += fromRowModelFactory(modelRef)
		else
			builder += fromValidatedRowModelFactory(modelRef)
		
		// For tables which contain a creation time index, additional inheritance is added
		if (classToWrite.recordsIndexedCreationTime)
			builder += fromRowFactoryWithTimestamps(modelRef)
		
		// If the class supports deprecation, it is reflected in this factory also
		if (classToWrite.isDeprecatable)
			builder += deprecatable
		
		builder.result()
	}
	
	private def propertiesFor(classToWrite: Class, tablesRef: Reference)
	                         (implicit setup: VaultProjectSetup, naming: NamingRules) =
	{
		val builder = new VectorBuilder[PropertyDeclaration]()
		
		// All objects define the table property (implemented)
		builder += ComputedProperty("table", Set(tablesRef), isOverridden = true)(
			s"${ tablesRef.target }.${ classToWrite.name.prop }")
		// Timestamp-based factories also specify a creation time property name
		if (classToWrite.recordsIndexedCreationTime)
			classToWrite.timestampProperty.foreach { createdProp =>
				builder += ComputedProperty("creationTimePropertyName", isOverridden = true)(createdProp.modelName.quoted)
			}
		// Non-timestamp-based factories need to specify default ordering
		else
			builder += ComputedProperty("defaultOrdering", isOverridden = true, isLowMergePriority = true)("None")
		// Deprecatable factories specify the deprecation condition (read from the database model)
		if (classToWrite.isDeprecatable) {
			val dbModelName = (classToWrite.name + DbModelWriter.classNameSuffix).className
			val dbModelRef = datatype.Reference(setup.dbModelPackage / classToWrite.packageName, dbModelName)
			builder += ComputedProperty("nonDeprecatedCondition", Set(dbModelRef), isOverridden = true)(
				s"$dbModelName.nonDeprecatedCondition")
		}
		
		builder.result()
	}
	
	private def methodsFor(classToWrite: Class, modelRef: Reference, dataRef: Reference)
	                      (implicit naming: NamingRules) =
	{
		def _modelFromAssignments(assignments: CodePiece) =
			modelRef.targetCode +
				classToWrite.idType.fromValueCode(s"valid(${ classToWrite.idDatabasePropName.quoted })")
					.append(dataRef.targetCode + assignments.withinParenthesis, ", ")
					.withinParenthesis
		
		val fromModelMethod = {
			if (classToWrite.fromDbModelConversionMayFail)
				ClassMethodFactory
					.classFromModel(classToWrite, "table.validate(model)")(_modelFromAssignments)
			else
				ClassMethodFactory
					.classFromValidatedModel(classToWrite)(_modelFromAssignments)
		}
		
		Set(fromModelMethod)
	}
}
