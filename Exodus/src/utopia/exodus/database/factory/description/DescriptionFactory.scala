package utopia.exodus.database.factory.description

import utopia.exodus.database.Tables
import utopia.exodus.database.model.description.DescriptionModel
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.metropolis.model.enumeration.DescriptionRole
import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.stored.description.Description
import utopia.vault.nosql.factory.FromRowModelFactory

/**
  * Used for reading description data from the DB
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
object DescriptionFactory extends FromRowModelFactory[Description]
{
	// IMPLEMENTED	--------------------------------
	
	override def apply(model: template.Model[Property]) =
	{
		table.requirementDeclaration.validate(model).toTry.flatMap { valid =>
			DescriptionRole.forId(valid(this.model.descriptionRoleIdAttName).getInt).map { role =>
				Description(valid("id").getInt, DescriptionData(role, valid("languageId").getInt,
					valid("text").getString, valid("authorId").int))
			}
		}
	}
	
	override def table = Tables.description
	
	
	// COMPUTED	-------------------------------------
	
	def model = DescriptionModel
}
