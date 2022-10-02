package utopia.vault.coder.model.data

import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Model
import utopia.flow.generic.SureFromModelFactory
import utopia.flow.util.CollectionExtensions._
import utopia.vault.coder.model.enumeration.{NameContext, NamingConvention}

object NamingRules extends SureFromModelFactory[NamingRules]
{
	/**
	  * The default naming rules
	  */
	val default = apply(Map[NameContext, NamingConvention]())
	
	override def parseFrom(model: Model[template.Property]) =
		apply(NameContext.values.flatMap { c =>
			model(c.jsonProps).string.flatMap(NamingConvention.forName).map { c -> _ }
		}.toMap)
}

/**
  * An object that describes how properties and classes should be named in different contexts
  * @author Mikko Hilpinen
  * @since 3.2.2022, v1.4.1
  */
case class NamingRules(rules: Map[NameContext, NamingConvention])
{
	/**
	  * @param context Implicit name context
	  * @return Most appropriate naming convention for that context
	  */
	def contextual(implicit context: NameContext) = apply(context)
	
	/**
	  * Finds either a custom-specified naming convention for the specified context, or returns context default
	  * @param context A name context
	  * @return Naming convention most appropriate for that context
	  */
	def apply(context: NameContext) =
		rules.get(context).orElse { context.parentsIterator.findMap(rules.get) }.getOrElse(context.defaultNaming)
}
