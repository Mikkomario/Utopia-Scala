package utopia.echo.model.llm

import utopia.echo.model.enumeration.ModelParameter
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.Mutate

import scala.language.implicitConversions

object ModelSettings
{
	// ATTRIBUTES   -------------------------
	
	/**
	  * Empty set of model settings
	  */
	lazy val empty = ModelSettings()
	
	
	// IMPLICIT ----------------------------
	
	implicit def collToSettings(settings: Iterable[(ModelParameter, Value)]): ModelSettings =
		apply(settings.toMap)
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
{
	// OTHER    -------------------------
	
	/**
	  * @param param Targeted parameter
	  * @return Value of that parameter
	  */
	def apply(param: ModelParameter): Value = defined.getOrElse(param, default.getOrElse(param, param.defaultValue))
	
	/**
	  * @param params New defined parameters. Overwrites all current values.
	  * @return Copy of these settings preserving the defaults but overwriting the definitions
	  */
	def withDefinedParams(params: Map[ModelParameter, Value]) = copy(defined = params)
	/**
	  * @param f A mapping function for all the defined parameters
	  * @return Copy of these settings with mapped parameter-definitions
	  */
	def mapDefinedParams(f: Mutate[Map[ModelParameter, Value]]) = withDefinedParams(f(defined))
	
	/**
	  * @param params New parameters to apply by default. Overwrites current defaults.
	  * @return Copy of these settings with new defaults.
	  */
	def withDefaultParams(params: Map[ModelParameter, Value]) = copy(default = params)
	
	/**
	  * @param parameter Targeted parameter
	  * @param value Assigned value
	  * @return Copy of these parameters with the specified parameter-assignment
	  */
	def withParam(parameter: ModelParameter, value: Value) =
		mapDefinedParams { _ + (parameter -> value) }
	
	/**
	  * @param param Parameter to undefine
	  * @return Copy of these parameters with the specified parameter undefined
	  */
	def without(param: ModelParameter) = mapDefinedParams { _ - param }
	/**
	  * @param params Parameters to undefine
	  * @return Copy of these parameters with the specified parameters undefined
	  */
	def without(params: IterableOnce[ModelParameter]) = mapDefinedParams { _ -- params }
	def without(first: ModelParameter, second: ModelParameter, more: ModelParameter*): ModelSettings =
		without(Pair(first, second) ++ more)
	
	/**
	  * @param param Targeted parameter
	  * @param f A mapping function that accepts the current parameter value (or default)
	  *          and yields the new value to assign
	  * @return Copy of these parameters with the specified parameter modified
	  */
	def mapParam(param: ModelParameter)(f: Mutate[Value]) = withParam(param, f(apply(param)))
	
	/**
	  * @param param A parameter assignment (key + value)
	  * @return Copy of these parameters with the specified assignment included
	  */
	def +(param: (ModelParameter, Value)) = mapDefinedParams { _ + param }
	/**
	  * @param params Parameter assignments (key + value pairs)
	  * @return Copy of these parameters with the specified assignments included
	  */
	def ++(params: IterableOnce[(ModelParameter, Value)]) = mapDefinedParams { _ ++ params }
	/**
	  * @param other Another set of parameters
	  * @return Copy of these parameters with the values from 'other' appended.
	  *         Affects both the definitions and the defaults.
	  */
	def ++(other: ModelSettings) = ModelSettings(defined ++ other.defined, default ++ other.default)
	
	/**
	  * @param param Parameter to undefine
	  * @return Copy of these parameters with the specified parameter undefined
	  */
	def -(param: ModelParameter) = without(param)
	/**
	  * @param params Parameters to undefine
	  * @return Copy of these parameters with the specified parameters undefined
	  */
	def --(params: IterableOnce[ModelParameter]) = without(params)
}
