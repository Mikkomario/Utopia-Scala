package utopia.echo.model.llm

import utopia.echo.model.enumeration.ModelParameter
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.SureFromModelFactory
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.flow.util.Mutate

import scala.language.implicitConversions

object ModelSettings extends SureFromModelFactory[ModelSettings]
{
	// ATTRIBUTES   -------------------------
	
	/**
	  * Empty set of model settings
	  */
	lazy val empty = ModelSettings()
	
	
	// IMPLICIT ----------------------------
	
	implicit def collToSettings(settings: Iterable[(ModelParameter, Value)]): ModelSettings =
		apply(settings.toMap)
		
	
	// IMPLEMENTED  ------------------------
	
	override def parseFrom(model: ModelLike[Property]): ModelSettings = {
		val params = Pair("defined", "defaults").map { key =>
			model(key).getModel.properties.view
				.flatMap { p => ModelParameter.findForKey(p.name).map { _ -> p.value } }
				.toMap
		}
		apply(params.first, params.second)
	}
}

/**
  * Common trait for read-accesses to model parameter -assignments
  * @author Mikko Hilpinen
  * @since 22.09.2024, v1.1
  *
  * @param defined Specifically defined parameter values
  * @param default Default values used in mapping, but not considered specifically defined
  */
case class ModelSettings(defined: Map[ModelParameter, Value] = Map(),
                         default: Map[ModelParameter, Value] = Map())
	extends ModelConvertible
{
	// COMPUTED -------------------------
	
	/**
	  * @return Copy of these settings where all defined fields have been cleared
	  */
	def withoutDefinitions = copy(defined = Map())
	/**
	  * @return Copy of these settings without any default values
	  */
	def withoutDefaults = copy(default = Map())
	
	
	// IMPLEMENTED  ---------------------
	
	override def toModel: Model = {
		Model(Pair("defined" -> defined, "defaults" -> default).map { case (key, params) =>
			key -> Model(params.map { case (k, v) => k.key -> v })
		})
	}
	
	
	// OTHER    -------------------------
	
	/**
	  * @param setting A setting
	  * @return Whether that setting has been defined
	  */
	def contains(setting: ModelParameter) = defined.contains(setting)
	
	/**
	  * @param setting Targeted parameter
	  * @return Value of that parameter. Empty if this value has not been specifically defined.
	  */
	def get(setting: ModelParameter) = defined.getOrElse(setting, Value.empty)
	/**
	  * @param setting Targeted parameter
	  * @return Value of that parameter
	  */
	def apply(setting: ModelParameter): Value =
		defined.getOrElse(setting, default.getOrElse(setting, setting.defaultValue))
	
	/**
	  * @param settings New defined parameters. Overwrites all current values.
	  * @return Copy of these settings preserving the defaults but overwriting the definitions
	  */
	def withDefined(settings: Map[ModelParameter, Value]) = copy(defined = settings)
	/**
	  * @param f A mapping function for all the defined parameters
	  * @return Copy of these settings with mapped parameter-definitions
	  */
	def mapDefined(f: Mutate[Map[ModelParameter, Value]]) = withDefined(f(defined))
	
	/**
	  * @param params New parameters to apply by default. Overwrites current defaults.
	  * @return Copy of these settings with new defaults.
	  */
	def withDefaults(params: Map[ModelParameter, Value]) = copy(default = params)
	
	/**
	  * @param setting Targeted parameter
	  * @param value Assigned value
	  * @return Copy of these parameters with the specified parameter-assignment
	  */
	def withSetting(setting: ModelParameter, value: Value) =
		mapDefined { _ + (setting -> value) }
	
	/**
	  * @param setting Parameter to undefine
	  * @return Copy of these parameters with the specified parameter undefined
	  */
	def without(setting: ModelParameter) = mapDefined { _ - setting }
	/**
	  * @param settings Parameters to undefine
	  * @return Copy of these parameters with the specified parameters undefined
	  */
	def without(settings: IterableOnce[ModelParameter]) = mapDefined { _ -- settings }
	def without(first: ModelParameter, second: ModelParameter, more: ModelParameter*): ModelSettings =
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
	def +(param: (ModelParameter, Value)) = mapDefined { _ + param }
	/**
	  * @param params Parameter assignments (key + value pairs)
	  * @return Copy of these parameters with the specified assignments included
	  */
	def ++(params: IterableOnce[(ModelParameter, Value)]) = mapDefined { _ ++ params }
	/**
	  * @param other Another set of parameters
	  * @return Copy of these parameters with the values from 'other' appended.
	  *         Affects both the definitions and the defaults.
	  */
	def ++(other: ModelSettings) = ModelSettings(defined ++ other.defined, default ++ other.default)
	
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
