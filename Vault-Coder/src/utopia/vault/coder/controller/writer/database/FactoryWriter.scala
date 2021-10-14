package utopia.vault.coder.controller.writer.database

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, ProjectSetup}
import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.vault.coder.model.scala.declaration.{File, ObjectDeclaration, PropertyDeclaration}
import utopia.vault.coder.model.scala.{Extension, Reference}
import utopia.vault.coder.util.ClassMethodFactory

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
	         (implicit codec: Codec, setup: ProjectSetup) =
	{
		val parentPackage = setup.factoryPackage / classToWrite.packageName
		val objectName = s"${ classToWrite.name }Factory"
		File(parentPackage,
			ObjectDeclaration(objectName, extensionsFor(classToWrite, modelRef),
				properties = propertiesFor(classToWrite, tablesRef),
				methods = methodsFor(classToWrite, modelRef, dataRef),
				description = s"Used for reading ${ classToWrite.name } data from the DB", author = classToWrite.author
			)
		).write()
	}
	
	private def extensionsFor(classToWrite: Class, modelRef: Reference): Vector[Extension] =
	{
		val builder = new VectorBuilder[Extension]()
		
		// If no enumerations are included, the inheritance is more specific (=> uses automatic validation)
		if (classToWrite.refersToEnumerations)
			builder += Reference.fromRowModelFactory(modelRef)
		else
			builder += Reference.fromValidatedRowModelFactory(modelRef)
		
		// For tables which contain a creation time index, additional inheritance is added
		if (classToWrite.recordsIndexedCreationTime)
			builder += Reference.fromRowFactoryWithTimestamps(modelRef)
		
		// If the class supports deprecation, it is reflected in this factory also
		if (classToWrite.isDeprecatable)
			builder += Reference.deprecatable
		
		builder.result()
	}
	
	private def propertiesFor(classToWrite: Class, tablesRef: Reference)(implicit setup: ProjectSetup) =
	{
		val builder = new VectorBuilder[PropertyDeclaration]()
		
		// All objects define the table property (implemented)
		builder += ComputedProperty("table", Set(tablesRef), isOverridden = true)(
			s"${ tablesRef.target }.${ classToWrite.name.singular.uncapitalize }")
		// Timestamp-based factories also specify a creation time property name
		classToWrite.creationTimeProperty.foreach { createdProp =>
			builder += ComputedProperty("creationTimePropertyName", isOverridden = true)(
				createdProp.name.singular.quoted)
		}
		// Deprecatable factories specify the deprecation condition (read from the database model)
		if (classToWrite.isDeprecatable) {
			val dbModelName = s"${ classToWrite.name }Model"
			val dbModelRef = Reference(setup.dbModelPackage / classToWrite.packageName, dbModelName)
			builder += ComputedProperty("nonDeprecatedCondition", Set(dbModelRef), isOverridden = true)(
				s"$dbModelName.nonDeprecatedCondition")
		}
		
		builder.result()
	}
	
	private def methodsFor(classToWrite: Class, modelRef: Reference, dataRef: Reference) =
	{
		def _modelFromAssignments(assignments: CodePiece) =
			modelRef.targetCode +
				classToWrite.idType.fromValueCode("valid(\"id\")")
					.append(dataRef.targetCode + assignments.withinParenthesis, ", ")
					.withinParenthesis
		
		val fromModelMethod = {
			if (classToWrite.refersToEnumerations)
				ClassMethodFactory.classFromModel(classToWrite, "table.validate(model)") {
					_.name.singular
				}(_modelFromAssignments)
			else
				ClassMethodFactory.classFromValidatedModel(classToWrite) { _.name.singular }(_modelFromAssignments)
		}
		
		Set(fromModelMethod)
	}
}
