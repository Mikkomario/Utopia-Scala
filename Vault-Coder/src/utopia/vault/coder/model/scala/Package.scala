package utopia.vault.coder.model.scala

import utopia.flow.util.FileExtensions._
import utopia.vault.coder.model.data.ProjectSetup
import utopia.vault.coder.model.scala.template.ScalaConvertible

import scala.language.implicitConversions

object Package
{
	// ATTRIBUTES   ---------------------------
	
	lazy val javaTime = apply("java.time")
	
	lazy val utopia = apply("utopia")
	lazy val flow = utopia/"flow"
	lazy val vault = utopia/"vault"
	
	lazy val flowGenerics = flow/"generic"
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
	lazy val viewAccess = access/"view"
	lazy val singleModelAccess = access/"single.model"
	lazy val manyModelAccess = access/"many.model"
	
	
	// IMPLICIT -------------------------------
	
	implicit def stringToPackage(packagePath: String): Package = apply(packagePath)
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param path Package path (E.g. "utopia.vault.coder")
	  * @return That path as a package
	  */
	def apply(path: String): Package = apply(path.split('.').toVector.filter { _.nonEmpty })
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
}
