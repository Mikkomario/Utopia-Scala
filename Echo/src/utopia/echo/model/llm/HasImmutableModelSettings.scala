package utopia.echo.model.llm

import utopia.echo.model.enumeration.ModelParameter
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.Mutate

import scala.language.implicitConversions


/**
  * Common trait for read-accesses to model parameter -assignments
  * @tparam Repr Type of the implementing class
  * @author Mikko Hilpinen
  * @since 22.09.2024, v1.1
  */
trait HasImmutableModelSettings[+Repr] extends HasModelSettings
{
	// ABSTRACT -------------------------
	
	/**
	  * @param settings New settings
	  * @return Copy of this instance with the specified settings
	  */
	def withSettings(settings: ModelSettings): Repr
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Copy of these settings where all defined fields have been cleared
	  */
	def withoutSettings: Repr = mapSettings { _.withoutDefinitions }
	/**
	  * @return Copy of these settings without any default values
	  */
	def withoutDefaultSettings = mapSettings { _.withoutDefaults }
	
	
	// OTHER    -------------------------
	
	/**
	  * @param f A mapping function for model settings
	  * @return A copy of this instance with mapped settings
	  */
	def mapSettings(f: Mutate[ModelSettings]): Repr = withSettings(f(settings))
	
	/**
	  * @param settings New defined parameters. Overwrites all current values.
	  * @return Copy of these settings preserving the defaults but overwriting the definitions
	  */
	def withDefinedSettings(settings: Map[ModelParameter, Value]) = mapSettings { _.withDefined(settings) }
	/**
	  * @param params New parameters to apply by default. Overwrites current defaults.
	  * @return Copy of these settings with new defaults.
	  */
	def withDefaultSettings(params: Map[ModelParameter, Value]) = mapSettings { _.withDefaults(params) }
	
	/**
	  * @param setting Targeted parameter
	  * @param value Assigned value
	  * @return Copy of these parameters with the specified parameter-assignment
	  */
	def withSetting(setting: ModelParameter, value: Value) = mapSettings { _.withSetting(setting, value) }
	
	/**
	  * @param setting Parameter to undefine
	  * @return Copy of these parameters with the specified parameter undefined
	  */
	def without(setting: ModelParameter) = mapSettings { _.without(setting) }
	/**
	  * @param settings Parameters to undefine
	  * @return Copy of these parameters with the specified parameters undefined
	  */
	def without(settings: IterableOnce[ModelParameter]) = mapSettings { _.without(settings) }
	def without(first: ModelParameter, second: ModelParameter, more: ModelParameter*): Repr =
		without(Pair(first, second) ++ more)
	
	/**
	  * @param param Targeted parameter
	  * @param f A mapping function that accepts the current parameter value (or default)
	  *          and yields the new value to assign
	  * @return Copy of these parameters with the specified parameter modified
	  */
	def mapSetting(param: ModelParameter)(f: Mutate[Value]) = withSetting(param, f(apply(param)))
	
	/**
	  * @param param A parameter assignment (key + value)
	  * @return Copy of these parameters with the specified assignment included
	  */
	def +(param: (ModelParameter, Value)) = mapSettings { _ + param }
	/**
	  * @param params Parameter assignments (key + value pairs)
	  * @return Copy of these parameters with the specified assignments included
	  */
	def ++(params: IterableOnce[(ModelParameter, Value)]) = mapSettings { _ ++ params }
	/**
	  * @param settings Another set of parameters
	  * @return Copy of these parameters with the values from 'other' appended.
	  *         Affects both the definitions and the defaults.
	  */
	def ++(settings: ModelSettings) = mapSettings { _ ++ settings }
	
	/**
	  * @param setting Parameter to undefine
	  * @return Copy of these parameters with the specified parameter undefined
	  */
	def -(setting: ModelParameter) = without(setting)
	/**
	  * @param settings Parameters to undefine
	  * @return Copy of these parameters with the specified parameters undefined
	  */
	def --(settings: IterableOnce[ModelParameter]) = without(settings)
}
