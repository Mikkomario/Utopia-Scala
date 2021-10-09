package utopia.vault.coder.model.scala

import utopia.flow.util.FileExtensions._
import utopia.vault.coder.model.data.ProjectSetup
import utopia.vault.coder.model.scala.template.ScalaConvertible

import scala.collection.StringOps
import scala.language.implicitConversions

object Package
{
	// ATTRIBUTES   ---------------------------
	
	lazy val java = apply("java")
	lazy val scalaDuration = apply("scala.concurrent.duration")
	lazy val javaTime = java/"time"
	
	lazy val utopia = apply("utopia")
	lazy val flow = utopia/"flow"
	lazy val vault = utopia/"vault"
	
	lazy val flowGenerics = flow/"generic"
	lazy val flowTime = flow/"time"
	lazy val flowUtils = flow/"util"
	lazy val struct = flow/"datastructure"
	lazy val immutableStruct = struct/"immutable"
	
	lazy val database = vault/"database"
	lazy val vaultModels = vault/"model"
	lazy val sql = vault/"sql"
	lazy val noSql = vault/"nosql"
	lazy val fromRowFactories = noSql/"factory.row"
	lazy val deprecation = noSql/"storable.deprecation"
	lazy val access = noSql/"access"
	lazy val viewAccess = noSql/"view"
	lazy val singleModelAccess = access/"single.model"
	lazy val manyModelAccess = access/"many.model"
	
	
	// IMPLICIT -------------------------------
	
	implicit def stringToPackage(packagePath: String): Package = apply(packagePath)
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param path Package path (E.g. "utopia.vault.coder")
	  * @return That path as a package
	  */
	def apply(path: String): Package = apply(path.split('.').toVector.filter { s => (s: StringOps).nonEmpty })
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
	  * @param setup Implicit project-specific setup
	  * @return A path to the directory matching this package
	  */
	def toPath(implicit setup: ProjectSetup) = parts.foldLeft(setup.sourceRoot) { _ / _ }
	
	
	// IMPLEMENTED  -----------------------
	
	override def toScala = parts.mkString(".")
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param more Extension to this package
	  * @return A sub-package of this package
	  */
	def /(more: String) = Package(parts ++ more.split('.').filter { _.nonEmpty })
	
	/**
	  * @param fileName Name of the targeted file (no .scala required)
	  * @param setup Implicit project setup
	  * @return Path to the specified file within this package (directory)
	  */
	def pathTo(fileName: String)(implicit setup: ProjectSetup) =
	{
		val fullFileName = if (fileName.contains('.')) fileName else s"$fileName.scala"
		toPath/fullFileName
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
