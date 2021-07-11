package utopia.ambassador.database.factory.scope

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.database.model.scope.TaskScopeLinkModel
import utopia.ambassador.model.partial.scope.TaskScopeLinkData
import utopia.ambassador.model.stored.scope.TaskScopeLink
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.{Deprecatable, FromValidatedRowModelFactory}

/**
  * Used for reading task-scope-links from the DB
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
object TaskScopeLinkFactory extends FromValidatedRowModelFactory[TaskScopeLink] with Deprecatable
{
	// COMPUTED --------------------------------
	
	/**
	  * @return Model matching this factory
	  */
	def model = TaskScopeLinkModel
	
	
	// IMPLEMENTED  ----------------------------
	
	override def table = AmbassadorTables.taskScope
	
	override def nonDeprecatedCondition = model.nonDeprecatedCondition
	
	override protected def fromValidatedModel(model: Model[Constant]) = TaskScopeLink(model("id"),
		TaskScopeLinkData(model("taskId"), model("scopeId"), model("created"), model("deprecatedAfter"),
			model("isRequired")))
}
