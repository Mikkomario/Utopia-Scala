package utopia.echo.model.llm

import utopia.echo.model.enumeration.ModelParameter
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.Mutate

/**
  * Common trait for interfaces that provide read and write access to model settings
  * @author Mikko Hilpinen
  * @since 22.09.2024, v1.1
  */
trait HasMutableModelSettings extends HasModelSettings
{
	// ABSTRACT -----------------------
	
	/**
	  * Updates the used model settings
	  * @param newSettings New settings to use
	  */
	def settings_=(newSettings: ModelSettings): Unit
	
	
	// OTHER    ----------------------
	
	/**
	  * Modifies the currently used model settings
	  * @param f A mapping function applied to the current settings
	  */
	def mapSettings(f: Mutate[ModelSettings]) = settings = f(settings)
	
	/**
	  * Updates the value of a model parameter
	  * @param param Modified parameter
	  * @param value New value assigned for the targeted parameter
	  */
	def update(param: ModelParameter, value: Value) = mapSettings { _ + (param -> value) }
	/**
	  * Modifies the value of a single model parameter
	  * @param setting Targeted parameter
	  * @param f Function for mapping the current parameter value.
	  *          If this parameter has not yet been defined, receives that option's default value.
	  */
	def mapSetting(setting: ModelParameter)(f: Mutate[Value]) = mapSettings { _.mapSetting(setting)(f) }
	
	/**
	  * Clears all custom LLM parameter definitions
	  */
	def clearSettings() = mapSettings { _.withoutDefinitions }
	/**
	  * Clears the value of a single option-definition, returning it to its default value
	  * @param option Option to clear
	  */
	def clear(option: ModelParameter) = mapSettings { _ - option }
	/**
	  * Clears the value of 0-n option-definitions, returning them to their default values
	  * @param options Options to clear
	  */
	def clearSettings(options: IterableOnce[ModelParameter]) = mapSettings { _ -- options }
	def clearSettings(option1: ModelParameter, option2: ModelParameter, moreOptions: ModelParameter*): Unit =
		clearSettings(Pair(option1, option2) ++ moreOptions)
	
	/**
	  * Updates the value of a model parameter
	  * @param param Modified parameter + assigned value
	  */
	def +=(param: (ModelParameter, Value)) = mapSettings { _ + param }
	/**
	  * Clears the value of a single option-definition, returning it to its default value
	  * @param param Option / parameter to clear
	  */
	def -=(param: ModelParameter) = clear(param)
	
	/**
	  * Updates 0-n LLM parameters
	  * @param params New parameters to assign
	  */
	def ++=(params: IterableOnce[(ModelParameter, Value)]) = mapSettings { _ ++ params }
	/**
	  * Clears 0-n LLM parameters, so that they won't be specified in future requests
	  * @param params Parameters to clear
	  */
	def --=(params: IterableOnce[ModelParameter]) = clearSettings(params)
}
