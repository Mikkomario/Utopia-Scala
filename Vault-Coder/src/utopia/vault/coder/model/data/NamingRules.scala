package utopia.vault.coder.model.data

import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Model
import utopia.flow.generic.FromModelFactory
import utopia.flow.util.CollectionExtensions._
import utopia.vault.coder.model.enumeration.NamingConvention
import utopia.vault.coder.model.enumeration.NamingConvention.{CamelCase, UnderScore}

import scala.util.Success

object NamingRules extends FromModelFactory[NamingRules]
{
	/**
	  * The default naming rules
	  */
	val default = NamingRules()
	
	override def apply(model: Model[template.Property]) = {
		def rule(default: => NamingConvention, propNames: String*) =
			propNames.findMap { model(_).string.flatMap(NamingConvention.forName) }.getOrElse(default)
		Success(NamingRules(
			// Table names
			rule(UnderScore, "table", "sql", "db"),
			// Column names
			rule(UnderScore, "column", "col", "sql", "db"),
			// Class names
			rule(CamelCase.capitalized, "class", "instance", "object"),
			// Property names
			rule(CamelCase.lower, "property", "prop", "attribute", "att", "code"),
			// Class properties as json properties
			rule(UnderScore, "json", "model"),
			// Class properties in db models
			rule(CamelCase.lower, "db_prop", "db_model_prop", "db_model", "db", "prop", "model")
		))
	}
}

/**
  * An object that describes how properties and classes should be named in different contexts
  * @author Mikko Hilpinen
  * @since 3.2.2022, v1.4.1
  */
case class NamingRules(table: NamingConvention = UnderScore, column: NamingConvention = UnderScore,
                       className: NamingConvention = CamelCase.capitalized,
                       classProp: NamingConvention = CamelCase.lower, jsonProp: NamingConvention = UnderScore,
                       dbModelProp: NamingConvention = CamelCase.lower)
