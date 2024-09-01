package utopia.echo.model.request

import utopia.echo.model.LlmDesignator
import utopia.echo.model.enumeration.ModelParameter
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

/**
  * Common trait for sets of parameters used to construct a request to an Ollama chat or generate endpoint.
  * @author Mikko Hilpinen
  * @since 31.08.2024, v1.1
  */
trait RequestParams[+Repr]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return Name of the targeted LLM
	  */
	def llm: LlmDesignator
	/**
	  * @return Behavioral parameters included in this request
	  */
	def options: Map[ModelParameter, Value]
	/**
	  * @return A view which contains true once/if this request gets deprecated and should be retracted
	  *         (if not yet sent out)
	  */
	def deprecationView: View[Boolean]
	
	/**
	  * @param llm Targeted LLM
	  * @return Copy of these parameters with the specified LLM assigned as the target
	  */
	def toLlm(llm: LlmDesignator): Repr
	/**
	  * @param options Options to specify in this request
	  * @return Copy of these parameters with the specified options. Note: The existing options are overwritten.
	  */
	def withOptions(options: Map[ModelParameter, Value]): Repr
	/**
	  * Replaces the deprecation view listed in these parameters
	  * @param condition A view which contains true once or if this request deprecates.
	  *                  Deprecations are only applied until this request is sent.
	  * @return Copy of these parameters with the specified deprecation condition
	  */
	def withDeprecationView(condition: View[Boolean]): Repr
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Copy of these parameters with no options / parameters assigned
	  */
	def withoutOptions = withOptions(Map())
	/**
	  * @return Copy of these parameters with no deprecation condition
	  */
	def neverDeprecating = withDeprecationView(AlwaysFalse)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param condition A call-by-name function used for testing whether this request is deprecated.
	  *                  Deprecations are only applied until this request is sent.
	  * @return Copy of these parameters with the specified deprecation condition
	  */
	def withDeprecationCondition(condition: => Boolean) = withDeprecationView(View(condition))
	
	/**
	  * @param f A mapping function for the applied parameters / options
	  * @return Copy of these parameters with mapped parameters
	  */
	def mapOptions(f: Mutate[Map[ModelParameter, Value]]) = withOptions(f(options))
	/**
	  * Modifies the value of an individual option within these parameters
	  * @param option Option to modify
	  * @param substituteWithDefault Whether, if there is not yet a value defined for that option,
	  *                              the option's default value should be passed to the function.
	  *                              Default = false =
	  *                              empty value will be passed if the specified option has not been specified.
	  * @param f A mapping function for an individual option value
	  * @return Copy of these parameters with a modified option value
	  */
	def mapOption(option: ModelParameter, substituteWithDefault: Boolean = false)(f: Value => Value) =
		mapOptions { options =>
			val existingValue = options
				.getOrElse(option, if (substituteWithDefault) option.defaultValue else Value.empty)
			options + (option -> f(existingValue))
		}
	
	/**
	  * @param parameter Parameter to assign
	  * @param value Value assigned to the specified parameter
	  * @return Copy of these parameters with the specified value assigned
	  */
	def withOption(parameter: ModelParameter, value: Value) = mapOptions { _ + (parameter -> value) }
	/**
	  * @param parameter A parameter value pair to assign
	  * @return Copy of these parameters with the specified parameter assigned
	  */
	def +(parameter: (ModelParameter, Value)) = mapOptions { _ + parameter }
}