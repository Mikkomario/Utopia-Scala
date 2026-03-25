package utopia.echo.model.enumeration

import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.generic.model.mutable.DataType.{BooleanType, DoubleType, IntType, VectorType}

/**
  * An enumeration for parameters which may be used to customize LLM behavior
  * @author Mikko Hilpinen
  * @since 31.08.2024, v1.1
  */
trait ModelParameter
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return JSON key used for representing this parameter. Lower case.
	  */
	def key: String
	/**
	 * @return Alternative JSON keys for representing this parameter. Lower case.
	 */
	def altKeys: Seq[String]
	/**
	  * @return Data type expected by in this parameter
	  */
	def dataType: DataType
	
	/**
	  * @return Value assigned to this parameter by default
	  */
	def defaultValue: Value
	
	/**
	  * @return Smallest applicable value for this parameter (as a double number)
	  */
	def minValue: Option[Double]
	/**
	  * @return Largest applicable value for this parameter (as a double number)
	  */
	def maxValue: Option[Double]
	
	
	// COMPUTED ---------------------------
	
	/**
	 * @return Keys used for representing this parameter
	 */
	def keys = key +: altKeys
}

object ModelParameter
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * All supported parameters
	  */
	val values = Vector[ModelParameter](
		Seed, ContextTokens, PredictTokens, KeepTokens, RepeatLastTokens,
		RepeatPenalty, PresencePenalty, FrequencyPenalty, PenalizeNewLine,
		Temperature, MiroStat, MiroStatTau, MiroStatEta,
		TopK, TopP, MinP, TypicalP, TailFreeSampling, Stop,
		NumberOfBatches, NumberOfGpus, MainGpuIndex, Numa, LowVRam, Fp16)
	/**
	  * All supported parameters as a map where keys are parameter string keys and values are the parameters themselves.
	  */
	lazy val valueByKey = values.iterator.flatMap { v => v.keys.iterator.map { _ -> v } }.toMap
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param key Targeted parameter key
	  * @return Targeted parameter. None if the specified key didn't match any parameter.
	  */
	def findForKey(key: String) = valueByKey.get(key)
	
	
	// VALUES   ---------------------------
	
	/**
	  * Sets the random number seed to use for generation.
	  * Setting this to a specific number will make the model generate the same text for the same prompt.
	  * Default is 0
	  */
	case object Seed extends ModelParameter
	{
		override val key: String = "seed"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = IntType
		
		override val defaultValue: Value = 0
		
		override val minValue: Option[Double] = Some(0)
		override val maxValue: Option[Double] = None
	}
	
	/**
	  * Sets the size of the context window used to generate the next token.
	  * Determines how much of the previous conversation the machine can remember at once.
	  * Larger numbers mean it can remember more of what was said earlier.
	  *
	  * Default is 2048
	  * Example values: 128-4096
	  */
	case object ContextTokens extends ModelParameter
	{
		override val key: String = "num_ctx"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = IntType
		
		override val defaultValue: Value = 2048
		
		override val minValue: Option[Double] = Some(0)
		override val maxValue: Option[Double] = None
	}
	/**
	  * Maximum number of tokens to predict when generating text.
	  * This limits how much the machine can say in one go. Setting a limit helps keep its responses concise.
	  *
	  * Default = 128, -1 = infinite generation, -2 = fill context
	  *
	  * A reasonable range could be somewhere between 10 and 1000, limited by the context size, obviously.
	  */
	case object PredictTokens extends ModelParameter
	{
		override val key: String = "num_predict"
		override val altKeys: Seq[String] = Single("max_tokens")
		override val dataType: DataType = IntType
		
		override val defaultValue: Value = 128
		
		override val minValue: Option[Double] = Some(-2)
		override val maxValue: Option[Double] = None
	}
	/**
	  * The number of previously seen tokens to keep in memory for language modeling.
	  * A reasonable range could be something between 2 and 50 (a guess).
	  */
	case object KeepTokens extends ModelParameter
	{
		override val key: String = "num_keep"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = IntType
		
		override val defaultValue: Value = 24
		
		override val minValue: Option[Double] = Some(0)
		override val maxValue: Option[Double] = None
	}
	
	/**
	  * Sets how far back for the model to look back to prevent repetition.
	  * Default: 64, 0 = disabled, -1 = num_ctx.
	  */
	case object RepeatLastTokens extends ModelParameter
	{
		override val key: String = "repeat_last_n"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = IntType
		
		override val defaultValue: Value = 64
		
		override val minValue: Option[Double] = Some(-1)
		override val maxValue: Option[Double] = None
	}
	/**
	  * Sets how strongly to penalize repetitions.
	  * If the machine starts repeating itself, this is like a nudge to encourage it to come up with something new.
	  * A higher value (e.g., 1.5) will penalize repetitions more strongly,
	  * while a lower value (e.g., 0.9) will be more lenient.
	  *
	  * Default is 1.1
	  * A reasonable range could be somewhere between 0.5 and 2.0.
	  */
	case object RepeatPenalty extends ModelParameter
	{
		override val key: String = "repeat_penalty"
		override val altKeys: Seq[String] = Single("repetition_penalty")
		override val dataType: DataType = DoubleType
		
		override val defaultValue: Value = 1.1
		
		override val minValue: Option[Double] = Some(0)
		override val maxValue: Option[Double] = None
	}
	/**
	  * A penalty for the absence of words in language modeling.
	  * A reasonable range could be somewhere between 0.5 and 2.0.
	  */
	case object PresencePenalty extends ModelParameter
	{
		override val key: String = "presence_penalty"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = DoubleType
		
		override val defaultValue: Value = 1.1
		
		override val minValue: Option[Double] = Some(0)
		override val maxValue: Option[Double] = None
	}
	/**
	  * A penalty for the frequency of words in language modeling.
	  * A reasonable range could be somewhere between 0.5 and 2.0.
	  */
	case object FrequencyPenalty extends ModelParameter
	{
		override val key: String = "frequency_penalty"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = DoubleType
		
		override val defaultValue: Value = 1.1
		
		override val minValue: Option[Double] = Some(0)
		override val maxValue: Option[Double] = None
	}
	/**
	  * Whether the use of newline characters should be penalized.
	  */
	case object PenalizeNewLine extends ModelParameter
	{
		override val key: String = "penalize_newline"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = BooleanType
		
		override val defaultValue: Value = true
		
		override val minValue: Option[Double] = Some(0)
		override val maxValue: Option[Double] = Some(1)
	}
	
	/**
	  * Controls how “wild” or “safe” the machine’s responses are.
	  * Higher temperatures encourage more creative responses.
	  *
	  * Default is 0.8.
	  * A reasonable range is between 0.0 and 1.0.
	  */
	case object Temperature extends ModelParameter
	{
		override val key: String = "temperature"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = DoubleType
		
		override val defaultValue: Value = 0.8
		
		override val minValue: Option[Double] = Some(0)
		override val maxValue: Option[Double] = Some(1)
	}
	
	/**
	  * Like a volume control for the machine’s “creativity.”
	  * Turning it on makes the machine’s responses less predictable.
	  *
	  * 0 is off, 1 is on, 2 is extra on.
	  */
	case object MiroStat extends ModelParameter
	{
		override val key: String = "mirostat"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = IntType
		
		override val defaultValue: Value = 1
		
		override val minValue: Option[Double] = Some(0)
		override val maxValue: Option[Double] = Some(2)
	}
	/**
	  * Helps decide if the machine should stick closely to the topic (lower values = coherence)
	  * or explore a bit more creatively (higher values = diversity).
	  *
	  * Default is 5.0
	  */
	case object MiroStatTau extends ModelParameter
	{
		override val key: String = "mirostat_tau"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = DoubleType
		
		override val defaultValue: Value = 5.0
		
		override val minValue: Option[Double] = Some(0)
		override val maxValue: Option[Double] = None
	}
	/**
	  * Adjusts how quickly the machine learns from what it’s currently talking about.
	  * Turning it down makes it more cautious; turning it up makes it adapt faster.
	  *
	  * A lower learning rate will result in slower adjustments,
	  * while a higher learning rate will make the algorithm more responsive.
	  *
	  * Default: 0.1
	  * A reasonable rate could be between 0 and 1.5
	  */
	case object MiroStatEta extends ModelParameter
	{
		override val key: String = "mirostat_eta"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = DoubleType
		
		override val defaultValue: Value = 0.1
		
		override val minValue: Option[Double] = Some(0)
		override val maxValue: Option[Double] = None
	}
	
	/**
	  * Limits the machine’s word choices to the top contenders, which helps it stay on topic and make sense.
	  * A higher value (e.g. 100) will give more diverse answers,
	  * while a lower value (e.g. 10) will be more conservative.
	  * Default is 40.
	  */
	case object TopK extends ModelParameter
	{
		override val key: String = "top_k"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = IntType
		
		override val defaultValue: Value = 40
		
		override val minValue: Option[Double] = Some(1)
		override val maxValue: Option[Double] = None
	}
	/**
	  * Determines the ratio of words included in the options. Works together with top-k.
	  * A higher value (e.g., 0.95) will lead to more diverse text,
	  * while a lower value (e.g., 0.5) will generate more focused and conservative text.
	  * Default is 0.9.
	  *
	  * High top-p can be used for introducing word & concept diversity.
	  * For reference on how to combine temperature and top p, see:
	  * https://medium.com/@1511425435311/understanding-openais-temperature-and-top-p-parameters-in-language-models-d2066504684f
	  */
	case object TopP extends ModelParameter
	{
		override val key: String = "top_p"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = DoubleType
		
		override val defaultValue: Value = 0.9
		
		override val minValue: Option[Double] = Some(0)
		override val maxValue: Option[Double] = Some(1)
	}
	/**
	  * A minimum probability threshold for words to be selected during prediction.
	  *
	  * Alternative to the top_p, and aims to ensure a balance of quality and variety.
	  * The parameter p represents the minimum probability for a token to be considered,
	  * relative to the probability of the most likely token.
	  *
	  * For example, with p=0.05 and the most likely token having a probability of 0.9,
	  * logits with a value less than 0.045 are filtered out. (Default: 0.0)
	  */
	case object MinP extends ModelParameter
	{
		override val key: String = "min_p"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = DoubleType
		
		override val defaultValue: Value = 0.0
		
		override val minValue: Option[Double] = Some(0)
		override val maxValue: Option[Double] = Some(1)
	}
	/**
	  * The typical probability threshold for words to be selected during prediction.
	  * A reasonable range could be somewhere between 0.5 and 0.9.
	  */
	case object TypicalP extends ModelParameter
	{
		override val key: String = "typical_p"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = DoubleType
		
		override val defaultValue: Value = 0.5
		
		override val minValue: Option[Double] = Some(0)
		override val maxValue: Option[Double] = Some(1)
	}
	
	/**
	  * Tail free sampling is used to reduce the impact of less probable tokens from the output.
	  * A higher value (e.g., 2.0) will reduce the impact more,
	  * while a value of 1.0 disables this setting.
	  *
	  * Aims to reduce randomness in the machine’s responses, keeping its “thoughts” more focused.
	  *
	  * Default is 1
	  */
	case object TailFreeSampling extends ModelParameter
	{
		override val key: String = "tfs_z"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = DoubleType
		
		override val defaultValue: Value = 1
		
		override val minValue: Option[Double] = Some(0)
		override val maxValue: Option[Double] = None
	}
	
	/**
	  * Sets the stop sequences to use. When this pattern is encountered the LLM will stop generating text and return.
	  * Supports multiple separate values.
	  */
	case object Stop extends ModelParameter
	{
		override val key: String = "stop"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = VectorType
		
		override val defaultValue: Value = Value.empty
		
		override val minValue: Option[Double] = None
		override val maxValue: Option[Double] = None
	}
	
	/**
	  * The number of batches of data to process during training.
	  * A reasonable range could be between 1 and 1024.
	  */
	case object NumberOfBatches extends ModelParameter
	{
		override val key: String = "num_batch"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = IntType
		
		override val defaultValue: Value = 256
		
		override val minValue: Option[Double] = Some(1)
		override val maxValue: Option[Double] = None
	}
	
	/**
	  * Sets how many “brains” (or parts of the computer’s graphics card / GPUs) the machine uses to think.
	  * More brains can mean faster or more detailed thinking.
	  *
	  * A reasonable range could be between 1 and 32 (inclusive).
	  */
	case object NumberOfGpus extends ModelParameter
	{
		override val key: String = "num_gpu"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = IntType
		
		override val defaultValue: Value = 8
		
		override val minValue: Option[Double] = Some(0)
		override val maxValue: Option[Double] = None
	}
	/**
	  * The index of the main GPU to use for parallel processing during training.
	  * If num_gpu > 1, this parameter specifies which GPU should be used as the primary GPU.
	  *
	  * The default value is 0,
	  * which means that all GPUs are equally important and will compete with each other for resources.
	  */
	case object MainGpuIndex extends ModelParameter
	{
		override val key: String = "main_gpu"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = IntType
		
		override val defaultValue: Value = 0
		
		override val minValue: Option[Double] = Some(0)
		override val maxValue: Option[Double] = None
	}
	
	/**
	  * Whether to use numa threads for parallel processing during training. False by default.
	  */
	case object Numa extends ModelParameter
	{
		override val key: String = "numa"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = BooleanType
		
		override val defaultValue: Value = false
		
		override val minValue: Option[Double] = Some(0)
		override val maxValue: Option[Double] = Some(1)
	}
	
	/**
	  * Whether to limit memory usage to prevent OOM errors during training.
	  */
	case object LowVRam extends ModelParameter
	{
		override val key: String = "low_vram"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = BooleanType
		
		override val defaultValue: Value = true
		
		override val minValue: Option[Double] = Some(0)
		override val maxValue: Option[Double] = Some(1)
	}
	
	/**
	  * Whether to use FP16 computation in the Kernel Vectorization (KV) operator of TensorFlow.
	  */
	object Fp16 extends ModelParameter
	{
		override val key: String = "fp16_kv"
		override val altKeys: Seq[String] = Empty
		override val dataType: DataType = BooleanType
		
		override val defaultValue: Value = false
		
		override val minValue: Option[Double] = Some(0)
		override val maxValue: Option[Double] = Some(1)
	}
}