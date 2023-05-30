package utopia.coder.model.scala.declaration

import utopia.coder.controller.parsing.scala.ScalaParser
import utopia.coder.model.data.ProjectSetup
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.coder.model.scala.code.CodeBuilder
import utopia.coder.model.merging.{MergeConflict, Mergeable}
import utopia.coder.model.scala.{Package, datatype}
import utopia.coder.model.scala.datatype.Reference
import utopia.coder.model.scala.template.CodeConvertible

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
		apply(packagePath, firstDeclaration +: moreDeclarations.toVector, firstDeclaration.name, Set[Reference]())
}

/**
  * Represents a scala file
  * @author Mikko Hilpinen
  * @since 31.8.2021, v0.1
  */
case class File(packagePath: Package, declarations: Vector[InstanceDeclaration], fileName: String,
                extraReferences: Set[Reference])
	extends CodeConvertible with Mergeable[File, File]
{
	// COMPUTED --------------------------------------
	
	/**
	  * @return A reference to this file / primary instance in this file
	  */
	def reference = datatype.Reference(packagePath, declarations.head.name)
	
	
	// IMPLEMENTED  ----------------------------------
	
	override def toCode = {
		// Writes the code first in order to access the references from there
		val codeBuilder = new CodeBuilder()
		// Writes the objects, then classes
		declarations.foreach { declaration =>
			codeBuilder ++= declaration.toCode
			codeBuilder.addEmptyLine()
		}
		val mainCode = codeBuilder.result()
		
		// Next writes the references part
		val refsBuilder = new CodeBuilder()
		
		// Writes the package declaration
		refsBuilder += s"package $packagePath"
		refsBuilder.addEmptyLine()
		
		// Writes the imports
		// Doesn't write references that are in the same package. Also simplifies imports in nested packages
		val referencesToWrite = (mainCode.references ++ extraReferences)
			.filter { ref => ref.packagePath != packagePath || !ref.canBeGrouped }
			.map { _.from(packagePath) }
		// Writes java.* and scala.* references separately
		val (javaScalaRefs, customRefs) = referencesToWrite.divideWith { ref =>
			ref.packagePath.parts.headOption match {
				case Some(firstPart) =>
					if (firstPart == "scala")
						Left(Right(ref))
					else if (firstPart == "java")
						Left(Left(ref))
					else
						Right(ref)
				case None => Right(ref)
			}
		}
		val (javaRefs, scalaRefs) = javaScalaRefs.divided
		
		def writeRefGroup(refs: Set[Reference]) = {
			if (refs.nonEmpty) {
				refsBuilder ++= importTargetsFrom(refs).map { target => s"import $target" }
				refsBuilder.addEmptyLine()
			}
		}
		
		writeRefGroup(customRefs.toSet)
		writeRefGroup(scalaRefs.toSet)
		writeRefGroup(javaRefs.toSet)
		
		// Combines the two parts together. Splits the main code where possible.
		refsBuilder.result() ++ mainCode.split
	}
	
	override def matches(other: File): Boolean =
		packagePath == other.packagePath && declarations.head.name == other.declarations.head.name
	
	override def mergeWith(other: File) = {
		val conflictsBuilder = new VectorBuilder[MergeConflict]()
		
		if (fileName.nonEmpty && other.fileName.nonEmpty && fileName != other.fileName)
			conflictsBuilder += MergeConflict.line(other.fileName, fileName, "File names differ")
		
		if (packagePath != other.packagePath)
			conflictsBuilder += MergeConflict.line(other.packagePath.toString, packagePath.toString,
				"Package differs")
		
		// Merges instances with same names
		val newDeclarations = declarations.map { my =>
			other.declarations.find { theirs => theirs.name == my.name && theirs.keyword == my.keyword } match {
				case Some(otherVersion) =>
					val (merged, conflicts) = my.mergeWith(otherVersion)
					conflictsBuilder ++= conflicts
					merged
				case None => my
			}
		} ++ other.declarations
			.filterNot { their => declarations.exists { my => my.name == their.name && my.keyword == their.keyword } }
		
		File(packagePath, newDeclarations, fileName.nonEmptyOrElse(other.fileName),
			extraReferences ++ other.extraReferences) -> conflictsBuilder.result()
	}
	
	
	// OTHER    ----------------------------
	
	/**
	  * Writes this file to the disk
	  * @param codec Implicit codec to use
	  * @param setup Implicit project setup
	  * @return Main reference in this file. Failure if writing failed.
	  */
	def write()(implicit codec: Codec, setup: ProjectSetup) = {
		val ref = reference
		val actualFileName = fileName.notEmpty match {
			case Some(fileName) => fileName.endingWith(".scala")
			case None =>
				declarations.headOption match {
					case Some(declaration) => s"${declaration.name}.scala"
					case None => "EmptyFile.scala"
				}
		}
		// Checks whether this file needs to be merged with an existing file
		val fileToWrite = setup.mergeSourceRoots
			.findMap { root => Some(ref.pathIn(root).withFileName(actualFileName)).filter { _.exists } }
			.flatMap { ScalaParser(_).toOption } match
		{
			case Some(readVersion) =>
				// Merges these two files.
				// Records conflicts, also
				val (newFile, conflicts) = mergeWith(readVersion)
				setup.recordConflicts(conflicts, s"${ref.target} in $packagePath")
				newFile
			case None => this
		}
		fileToWrite.writeTo(ref.path.withFileName(actualFileName)).map { _ => ref }
	}
	
	private def importTargetsFrom(references: Set[Reference]) = {
		// Those of the imports which can be grouped, are grouped
		val (individualReferences, groupableReferences) = references.divideBy { _.canBeGrouped }
		(individualReferences.toVector.map { _.toScala.text } ++
			groupableReferences.groupBy { _.packagePath }.map { case (packagePath, refs) =>
				if (refs.size > 1)
					s"$packagePath.{${refs.map { _.target }.toVector.sorted.mkString(", ")}}"
				else
					s"$packagePath.${refs.head.target}"
			}).sorted
	}
}
