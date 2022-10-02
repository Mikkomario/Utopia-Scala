package utopia.vault.coder.controller.writer.database

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model}
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, Name, NamingRules}
import utopia.vault.coder.model.enumeration.NameContext.DatabaseName

import java.nio.file.Path
import scala.io.Codec
import scala.util.Success

/**
  * Used for writing a .json document describing column length rules to apply
  * @author Mikko Hilpinen
  * @since 10.2.2022, v1.5
  */
object ColumnLengthRulesWriter
{
	/**
	  * Writes column length rules .json document
	  * @param databaseName Name of the database to target (optional)
	  * @param classes Classes to write
	  * @param path Path where the document will be written (call-by-name)
	  * @param codec Implicit codec to use
	  * @param naming Implicit naming rules to apply
	  * @return Path to the written file on success. Failure if writing failed. Success(None) if writing was skipped
	  *         (because there were no rules to write).
	  */
	def apply(databaseName: Option[Name], classes: Seq[Class], path: => Path)
	         (implicit codec: Codec, naming: NamingRules) =
	{
		// Generates the table & column rule properties to write
		val ruleProps = classes.flatMap { classToWrite =>
			val ruleProps = classToWrite.dbProperties.flatMap { prop =>
				prop.overrides.lengthRule.notEmpty.map { rule => Constant(prop.modelName, rule) }
			}.toVector
			if (ruleProps.nonEmpty)
				Some(Constant(classToWrite.tableName, Model.withConstants(ruleProps.sortBy { _.name })))
			else
				None
		}.sortBy { _.name }
		
		// Only writes the document if at least one rule has been specified
		if (ruleProps.nonEmpty) {
			// Written document style depends on whether the database name has been specified or not
			val json = databaseName match {
				case Some(dbName) => Model(Vector(dbName(DatabaseName) -> Model.withConstants(ruleProps)))
				case None => Model.withConstants(ruleProps)
			}
			path.createParentDirectories().flatMap { _.writeJson(json).map { Some(_) } }
		}
		else
			Success(None)
	}
}
