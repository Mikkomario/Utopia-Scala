package utopia.vault.coder.model.scala

import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.string.Regex
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.ProjectSetup
import utopia.vault.coder.model.scala.Package.separatorRegex
import utopia.vault.coder.model.scala.template.ScalaConvertible

import java.nio.file.Path
import scala.collection.StringOps
import scala.language.implicitConversions

object Package
{
	// ATTRIBUTES   ---------------------------
	
	/**
	  * A regular expression that matches package separators (ie. '.')
	  */
	val separatorRegex = Regex.escape('.')
	
	val empty = apply(Vector())
	
	// Java & Scala
	
	lazy val java = apply("java")
	lazy val scalaDuration = apply("scala.concurrent.duration")
	lazy val javaTime = java/"time"
	
	// Utopia base packages
	
	lazy val utopia = apply("utopia")
	lazy val flow = utopia/"flow"
	lazy val vault = utopia/"vault"
	lazy val metropolis = utopia/"metropolis"
	lazy val citadel = utopia/"citadel"
	
	// Flow
	
	lazy val flowGenerics = flow/"generic"
	lazy val typeCasting = flowGenerics/"casting"
	lazy val genericModels = flowGenerics/"model"
	lazy val immutableGenericModels = genericModels/"immutable"
	lazy val genericModelTemplates = genericModels/"template"
	lazy val flowTime = flow/"time"
	lazy val flowUtils = flow/"util"
	@deprecated
	lazy val struct = flow/"datastructure"
	@deprecated
	lazy val immutableStruct = struct/"immutable"
	
	// Vault
	
	lazy val database = vault/"database"
	lazy val vaultModels = vault/"model"
	lazy val sql = vault/"sql"
	lazy val noSql = vault/"nosql"
	lazy val deprecation = noSql/"storable.deprecation"
	
	lazy val factories = noSql/"factory"
	lazy val fromRowFactories = factories/".row"
	lazy val singleLinkedFactories = fromRowFactories/"linked"
	
	lazy val access = noSql/"access"
	lazy val viewAccess = noSql/"view"
	lazy val singleModelAccess = access/"single.model"
	lazy val manyModelAccess = access/"many.model"
	
	// Metropolis
	
	lazy val metropolisModel = metropolis/"model"
	lazy val description = metropolisModel/"stored.description"
	lazy val combinedDescription = metropolisModel/"combined.description"
	
	// Citadel
	
	lazy val citadelDatabase = citadel/"database"
	lazy val citadelAccess = citadelDatabase/"access"
	lazy val descriptionsAccess = citadelAccess/"many.description"
	lazy val descriptionAccess = citadelAccess/"single.description"
	
	
	// IMPLICIT -------------------------------
	
	implicit def stringToPackage(packagePath: String): Package = apply(packagePath)
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param path Package path (E.g. "utopia.vault.coder")
	  * @return That path as a package
	  */
	def apply(path: String): Package =
		apply(path.split(separatorRegex).toVector.filter { s => (s: StringOps).nonEmpty })
}

/**
  * Represents a package which contains many classes
  * @author Mikko Hilpinen
  * @since 26.9.2021, v1.1
  */
case class Package(parts: Vector[String]) extends ScalaConvertible
{
	// COMPUTED ---------------------------
	
	/**
	  * @return Whether this package is empty ("")
	  */
	def isEmpty = parts.isEmpty
	/**
	  * @return Whether this package path contains at least one package
	  */
	def nonEmpty = !isEmpty
	
	/**
	  * @return The parent package of this package - this package if this is the root package
	  */
	def parent = if (isEmpty) this else Package(parts.dropRight(1))
	
	/**
	  * @param setup Implicit project-specific setup
	  * @return A path to the directory matching this package
	  */
	def toPath(implicit setup: ProjectSetup) = toPathIn(setup.sourceRoot)
	
	
	// IMPLEMENTED  -----------------------
	
	override def toScala = parts.mkString(".")
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param more Extension to this package
	  * @return A sub-package of this package
	  */
	def /(more: String) = Package(parts ++ more.split(separatorRegex).filter { _.nonEmpty })
	
	/**
	  * @param sourceRoot Source root directory
	  * @return A path to the directory matching this package
	  */
	def toPathIn(sourceRoot: Path) = parts.foldLeft(sourceRoot) { _ / _ }
	
	/**
	  * @param fileName Name of the targeted file (no .scala required)
	  * @param setup Implicit project setup
	  * @return Path to the specified file within this package (directory)
	  */
	def pathTo(fileName: String)(implicit setup: ProjectSetup): Path = pathTo(fileName, setup.sourceRoot)
	/**
	  * @param fileName Name of the targeted file (no .scala required)
	  * @param inSourceRoot Source root directory
	  * @return Path to the specified file within this package (directory)
	  */
	def pathTo(fileName: String, inSourceRoot: Path) =
	{
		val fullFileName = if (fileName.contains('.')) fileName else s"$fileName.scala"
		toPathIn(inSourceRoot)/fullFileName
	}
	
	/**
	  * Checks whether this package resides under the specified package
	  * @param another Another package
	  * @return Whether this package is relative to that one
	  */
	def isRelativeTo(another: Package) = parts.take(another.parts.size) == another.parts
	
	/**
	  * Updates this package by referencing it from the other package. If this package is not relative to the
	  * other package, this is returned. If these two packages are equal, an empty path is returned.
	  * E.g. "vault.coder.test" relativeTo "vault" would return "coder.test".
	  * "vault.coder.test" relativeTo "vault.database" would return "vault.coder.test"
	  * @param another Another package
	  * @return A copy of this package from the perspective of the other package.
	  *         This package if not relative to the other.
	  */
	def fromPerspectiveOf(another: Package) =
	{
		if (isRelativeTo(another))
			Package(parts.drop(another.parts.size))
		else
			this
	}
}
