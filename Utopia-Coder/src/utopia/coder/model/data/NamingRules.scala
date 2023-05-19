package utopia.coder.model.data

import utopia.coder.model.enumeration.{NameContext, NamingConvention}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.factory.SureFromModelFactory
import utopia.flow.generic.model.template.{ModelLike, Property}

object NamingRules extends SureFromModelFactory[NamingRules]
{
	/**
	  * The default naming rules
	  */
	val default = apply(Map[NameContext, NamingConvention]())
	
	override def parseFrom(model: ModelLike[Property]) =
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
