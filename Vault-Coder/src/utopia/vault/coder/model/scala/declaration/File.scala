package utopia.vault.coder.model.scala.declaration

import utopia.flow.util.CollectionExtensions._
import utopia.vault.coder.model.data.ProjectSetup
import utopia.vault.coder.model.scala.{Package, Reference}
import utopia.vault.coder.model.scala.template.CodeConvertible

import scala.collection.immutable.VectorBuilder
import scala.io.Codec

object File
{
	/**
	  * Creates a new file
	  * @param packagePath Package for the contents of this file
	  * @param firstDeclaration First instance declaration
	  * @param moreDeclarations More instance declarations
	  * @return A new file containing the specified declarations
	  */
	def apply(packagePath: Package, firstDeclaration: InstanceDeclaration,
	          moreDeclarations: InstanceDeclaration*): File =
		apply(packagePath, firstDeclaration +: moreDeclarations.toVector)
}

/**
  * Represents a scala file
  * @author Mikko Hilpinen
  * @since 31.8.2021, v0.1
  */
case class File(packagePath: Package, declarations: Vector[InstanceDeclaration])
	extends CodeConvertible
{
	// COMPUTED --------------------------------------
	
	/**
	  * @return A reference to this file / primary instance in this file
	  */
	def reference = Reference(packagePath, declarations.head.name)
	
	
	// IMPLEMENTED  ----------------------------------
	
	override def toCodeLines =
	{
		val builder = new VectorBuilder[String]()
		
		// Writes the package declaration
		builder += s"package $packagePath"
		builder += ""
		
		// Writes the imports
		// Those of the imports which can be grouped, are grouped
		val (individualReferences, groupableReferences) = declarations.flatMap { _.references }.toSet
			.divideBy { _.canBeGrouped }
		val importTargets = (individualReferences.toVector.map { _.toScala } ++
			groupableReferences.groupBy { _.packagePath }.map { case (packagePath, refs) =>
				s"$packagePath.{${refs.map { _.target }.toVector.sorted.mkString(", ")}"
			}).sorted
		builder ++= importTargets.map { target => s"import $target" }
		if (importTargets.nonEmpty)
			builder += ""
		
		// Writes the objects, then classes
		declarations.foreach { declaration =>
			builder ++= declaration.toCodeLines
			builder += ""
		}
		
		builder.result()
	}
	
	
	// OTHER    ----------------------------
	
	/**
	  * Writes this file to the disk
	  * @param codec Implicit codec to use
	  * @param setup Implicit project setup
	  * @return Main reference in this file. Failure if writing failed.
	  */
	def write()(implicit codec: Codec, setup: ProjectSetup) =
	{
		val ref = reference
		writeTo(ref.path).map { _ => ref }
	}
}
