package utopia.coder.model.scala.datatype

import utopia.coder.model.data.ProjectSetup
import utopia.flow.util.StringExtensions._
import utopia.coder.model.scala.Package
import utopia.coder.model.scala.code.CodePiece
import utopia.coder.model.scala.template.ScalaConvertible

import java.nio.file.Path
import scala.collection.StringOps

object Reference
{
	// ATTRIBUTES   -------------------------
	
	import Package._
	
	// Scala time
	
	lazy val duration = apply(scalaDuration, "Duration")
	lazy val finiteDuration = apply(scalaDuration, "FiniteDuration")
	
	// Java time
	
	lazy val instant = apply(javaTime, "Instant")
	lazy val localDate = apply(javaTime, "LocalDate")
	lazy val localTime = apply(javaTime, "LocalTime")
	lazy val timeUnit = apply(java / "util.concurrent", "TimeUnit")
	
	// Other Scala
	
	lazy val success = apply("scala.util", "Success")
	
	// Other Java
	
	lazy val noSuchElementException = apply("java.util", "NoSuchElementException")
	
	
	// COMPUTED -------------------------------
	
	/**
	  * @return Access to Flow references
	  */
	def flow = Flow
	
	
	// OTHER    -------------------------------
	
	def apply(pck: Package, target: String): Reference = apply(pck, Vector(target))
	
	/**
	  * Converts a string into a reference
	  * @param ref A string representing a reference
	  * @return A reference parsed from that string
	  */
	def apply(ref: String): Reference = {
		// Case: Empty reference
		if (ref.isEmpty)
			apply(empty, "")
		else {
			val parts = ref.split(separatorRegex).toVector.filter { s => (s: StringOps).nonEmpty }
			// Case: Extensions reference or import all -reference
			if (parts.last == "_" && parts.size > 1)
				extensions(Package(parts.dropRight(2)), parts(parts.size - 2))
			// Case: Single property reference or a sub-reference
			else if (parts.last.head.isLower && parts.size > 1)
				apply(Package(parts.dropRight(2)), Vector(parts(parts.size - 2)), parts.last)
			// Case: Standard reference
			else
				apply(Package(parts.dropRight(1)), parts.last)
		}
	}
	
	/**
	  * Creates a reference to implicit extensions
	  * @param packagePath Package leading to the target
	  * @param target File / object that contains the implicits
	  * @return A reference to the implicits in that file / object
	  */
	def extensions(packagePath: Package, target: String) = apply(packagePath, s"$target._")
	
	
	// NESTED   ----------------------------
	
	object Flow
	{
		import Package.Flow._
		
		lazy val valueConversions = extensions(typeCasting, "ValueConversions")
		lazy val valueUnwraps = extensions(typeCasting, "ValueUnwraps")
		lazy val collectionExtensions = extensions(collection, "CollectionExtensions")
		lazy val timeExtensions = extensions(time, "TimeExtensions")
		
		lazy val value = apply(immutableGenericModels, "Value")
		lazy val property = apply(genericModelTemplates, "Property")
		lazy val constant = apply(immutableGenericModels, "Constant")
		lazy val templateModel = apply(genericModelTemplates, "ModelLike")
		lazy val model = apply(immutableGenericModels, "Model")
		lazy val valueConvertible = apply(genericModelTemplates, "ValueConvertible")
		lazy val modelConvertible = apply(genericModelTemplates, "ModelConvertible")
		lazy val modelDeclaration = apply(immutableGenericModels, "ModelDeclaration")
		lazy val propertyDeclaration = apply(immutableGenericModels, "PropertyDeclaration")
		lazy val fromModelFactory = apply(generics / "factory", "FromModelFactory")
		lazy val fromModelFactoryWithSchema = apply(generics / "factory", "FromModelFactoryWithSchema")
		
		lazy val dataType = apply(genericModels / "mutable", "DataType")
		lazy val stringType = dataType / "StringType"
		lazy val instantType = dataType / "InstantType"
		
		lazy val now = apply(time, "Now")
		lazy val today = apply(time, "Today")
		lazy val days = apply(time, "Days")
		
		lazy val extender = apply(viewTemplate, "Extender")
	}
}

/**
  * Represents an imported external class or object etc.
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  * @param packagePath Path leading to the imported item. E.g. "utopia.vault.coder.model.scala"
  * @param importTarget The imported item which is present when referring to the referenced item,
  *                     as well as in the import
  * @param subReference Item referred to under the imported item. E.g. a specific property
  */
case class Reference private(packagePath: Package, importTarget: Vector[String], subReference: String = "")
	extends ScalaConvertible
{
	// COMPUTED --------------------------------
	
	/**
	  * @return The target of this reference when referred from code (doesn't contain the package path,
	  *         expects an import)
	  */
	def target = {
		val base = importTarget.last
		if (subReference.isEmpty) base else s"$base.$subReference"
	}
	/**
	  * @return A code referring to the target of this reference using its name
	  */
	def targetCode = CodePiece(target, Set(this))
	
	/**
	  * @return Whether it is possible to group this reference with other references using { ... } syntax
	  */
	def canBeGrouped = subReference.isEmpty && importTarget.size == 1 && importTarget.forall { !_.contains('.') }
	
	/**
	  * @param setup Implicit project setup
	  * @return Path to the referenced file
	  */
	def path(implicit setup: ProjectSetup) = pathIn(setup.sourceRoot)
	
	
	// OTHER    --------------------------------
	
	/**
	  * @param typeParam1 First generic type parameter
	  * @param moreTypeParams Additional generic type parameters
	  * @return A generic type based on this reference
	  */
	def apply(typeParam1: ScalaType, moreTypeParams: ScalaType*) =
		ScalaType.generic(this, typeParam1, moreTypeParams: _*)
	
	/**
	  * @param newTarget Another target under this reference
	  * @return Reference to that sub-item
	  */
	def /(newTarget: String) = subReference.notEmpty match {
		case Some(oldSubRef) => copy(subReference = s"$oldSubRef.$newTarget")
		case None => copy(importTarget = importTarget :+ newTarget)
	}
	
	/**
	  * @param packagePath A package from which this reference is viewed from
	  * @return A copy of this reference, relative to that package
	  */
	def from(packagePath: Package) = copy(packagePath = this.packagePath.fromPerspectiveOf(packagePath))
	
	/**
	  * @param sourceRoot Root directory
	  * @return Path to the referenced file
	  */
	def pathIn(sourceRoot: Path) = {
		val (packagePart, classPart) = importTarget.mkString(".").splitAtLast(".")
		// Case: There are no two parts in the target => Uses the only part as the file name
		if (classPart.isEmpty)
			packagePath.pathTo(packagePart, sourceRoot)
		// Case: Target consists of multiple parts => appends the package part to the package path (directory path)
		else
			(packagePath/packagePart).pathTo(classPart, sourceRoot)
	}
	
	
	// IMPLEMENTED  ----------------------------
	
	override def toScala = {
		if (importTarget.isEmpty)
			packagePath.toScala
		else if (packagePath.isEmpty)
			importTarget.mkString(".")
		else
			s"${packagePath.toScala}.${ importTarget.mkString(".") }"
	}
}
