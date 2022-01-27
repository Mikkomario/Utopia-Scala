package utopia.citadel.importer.controller

import utopia.citadel.database.Tables
import utopia.citadel.database.access.many.description.{DbClientDeviceDescriptions, DbDescriptionRoleDescriptions, DbDescriptionRoles, DbLanguageDescriptions, DbLanguageFamiliarityDescriptions, DbOrganizationDescriptions, DbTaskDescriptions, DbUserRoleDescriptions, LinkedDescriptionsAccess}
import utopia.citadel.database.access.many.language.DbLanguages
import utopia.citadel.database.model.description.DescriptionModel
import utopia.citadel.model.cached.DescriptionLinkTable
import utopia.flow.datastructure.immutable.{Model, Value}
import utopia.flow.generic.ValueConversions._
import utopia.flow.parse.JsonParser
import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.vault.database.Connection

import java.nio.file.Path
import scala.util.{Failure, Success, Try}

/**
  * Reads descriptions from a json file
  * @author Mikko Hilpinen
  * @since 26.6.2021, v1.0
  */
object ReadDescriptions
{
	/**
	  * Imports description data from the specified file
	  * @param path Path to the json file from which data is read
	  * @param connection Implicit db connection
	  * @param jsonParser Implicit json parser
	  * @return Success or a failure
	  */
	def apply(path: Path)(implicit connection: Connection, jsonParser: JsonParser): Try[Unit] =
	{
		jsonParser(path).flatMap { input =>
			// Checks whether the input consists of a single object or multiple objects
			val targetObjects = input.vector match
			{
				case Some(values) => values.flatMap { _.model }
				case None => input.model.toVector
			}
			// Prints a warning if there is no data to process
			if (targetObjects.isEmpty)
				println(s"Warning: No descriptions to read from $path")
			apply(targetObjects)
		}
	}
	
	/**
	  * Imports description data from the specified target objects
	  * @param targets Target objects, where each must contain a valid "target" property and should contain
	  *                descriptions within them
	  * @param connection Implicit DB Connection
	  * @return Success or failure
	  */
	def apply(targets: Vector[Model])(implicit connection: Connection) =
	{
		// Makes sure all targets are valid
		targets.tryMap { m => targetFrom(m("target")).map { _ -> m } }.map { targets =>
			// Reads required data (languages and description roles)
			val languageIds = DbLanguages.all.map { l => l.isoCode -> l.id }.toMap
			val descriptionRoles = DbDescriptionRoles.pull
			
			// Processes the input data
			targets.foreach { case (target, model) => handleTarget(target, model, languageIds, descriptionRoles) }
		}
	}
	
	private def handleTarget(access: LinkedDescriptionsAccess, model: Model, languageIds: Map[String, Int],
	                         descriptionRoles: Vector[DescriptionRole])
	                        (implicit connection: Connection) =
	{
		// Finds the roles that are mentioned
		descriptionRoles.view.foreach { role =>
			model(role.jsonKeyPlural).model.foreach { input =>
				// Expect model keys to be language codes
				(input.attributeNames & languageIds.keySet).foreach { languageCode =>
					val languageId = languageIds(languageCode)
					
					// Searches for the existing descriptions
					val descriptionsModel = input(languageCode).getModel
					val existingDescriptions = access(descriptionsModel.attributeNames.flatMap { _.int })
						.inLanguageWithId(languageId).withRoleId(role.id).pull
						.map { link => link.targetId -> link }.toMap
					// Checks which of the descriptions were updated and which are completely new
					val (newDescriptions, updates) = descriptionsModel.attributesWithValue
						.flatMap { att => att.name.int.flatMap { targetId =>
							att.value.string.filter { _.nonEmpty }.map { targetId -> _ } } }
						.divideWith { case (targetId, description) =>
							existingDescriptions.get(targetId) match
							{
								case Some(existing) => Right(existing -> description)
								case None => Left(targetId -> DescriptionData(role.id, languageId, description))
							}
						}
					
					// Deprecates the versions that will overwritten
					val changingUpdates = updates
						.filter { case (existing, newDescription) => existing.description.text != newDescription }
					if (changingUpdates.nonEmpty)
						DescriptionModel.deprecateIds(changingUpdates.map { _._1.id })
					
					// Inserts new / updated descriptions
					if (newDescriptions.nonEmpty || changingUpdates.nonEmpty)
					{
						access.linkModel.insertDescriptions(newDescriptions ++
							changingUpdates.map { case (description, newDescription) =>
								description.targetId -> DescriptionData(role.id, languageId, newDescription) })
						
						// Prints an update to the console
						val targetName = model("target").getString
						if (changingUpdates.nonEmpty)
						{
							if (changingUpdates.size == 1)
							{
								val (old, description) = changingUpdates.head
								println(s"Updated one $targetName ${
									role.jsonKeySingular} from '${old.description.text}' to '$description'")
							}
							else
								println(s"Updated ${changingUpdates.size} ${role.jsonKeyPlural} of $targetName")
						}
						if (newDescriptions.nonEmpty)
						{
							if (newDescriptions.size == 1)
								println(s"Added new $targetName ${role.jsonKeySingular}: '${
									newDescriptions.head._2.text}'")
							else
								println(s"Added ${newDescriptions.size} new ${role.jsonKeyPlural} to $targetName")
						}
					}
				}
			}
		}
	}
	
	private def targetFrom(value: Value) =
	{
		value.model match {
			// Case: Target is a model => searches for "table" and "column" properties
			case Some(model) => model("table").trySting
				.flatMap { tableName => model("column").trySting.map { c =>
					val table = DescriptionLinkTable(Tables(tableName), c)
					LinkedDescriptionsAccess(table)
				} }
			// Otherwise expects the target to be one of specified values
			case None =>
				value.trySting.map { _.toLowerCase }.flatMap {
					case "description_role" => Success(DbDescriptionRoleDescriptions)
					case "language" => Success(DbLanguageDescriptions)
					case "language_familiarity" => Success(DbLanguageFamiliarityDescriptions)
					case "organization" => Success(DbOrganizationDescriptions)
					case "task" => Success(DbTaskDescriptions)
					case "user_role" => Success(DbUserRoleDescriptions)
					case "device" => Success(DbClientDeviceDescriptions)
					case s: String =>
						Failure(new NoSuchElementException(s"No standard description factory for key: $s"))
				}
		}
	}
}
