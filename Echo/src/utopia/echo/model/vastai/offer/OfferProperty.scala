package utopia.echo.model.vastai.offer

import utopia.echo.model.enumeration.NetworkTrafficDirection
import utopia.echo.model.enumeration.NetworkTrafficDirection.{Download, Upload}
import utopia.echo.model.vastai.offer.FilterOperator.{EqualTo, GreaterThan, In, LessThan, NotEqualTo, NotIn}
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.time.Duration
import utopia.flow.time.TimeExtensions._

/**
 * An enumeration for different offer-filtering keys
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.4.1
 */
sealed trait OfferProperty[-V]
{
	// ABSTRACT -----------------------
	
	/**
	 * @return JSON key matching this property
	 */
	def key: String
	
	/**
	 * @param value An accepted input value
	 * @return A value as it should be passed to Vast AI
	 */
	def toValue(value: V): Value
	
	
	// OTHER    ----------------------
	
	/**
	 * @param value A value
	 * @return A filter that requires this property to match that value
	 */
	def apply(value: V) = SearchFilter(this, EqualTo, toValue(value))
	/**
	 * @param value A value
	 * @return A filter that requires this property to not match that value
	 */
	def not(value: V) = SearchFilter(this, NotEqualTo, toValue(value))
	
	/**
	 * @param value A threshold value (exclusive)
	 * @return A filter that requires this property to be greater than the specified value
	 */
	def >(value: V) = SearchFilter(this, GreaterThan, toValue(value))
	/**
	 * @param value A threshold value (exclusive)
	 * @return A filter that requires this property to be smaller than the specified value
	 */
	def <(value: V) = SearchFilter(this, LessThan, toValue(value))
	/**
	 * @param value A threshold value (inclusive)
	 * @return A filter that requires this property to be greater than or equal to the specified value
	 */
	def >=(value: V) = SearchFilter(this, GreaterThan.orEqual, toValue(value))
	/**
	 * @param value A threshold value (inclusive)
	 * @return A filter that requires this property to be smaller than or equal to the specified value
	 */
	def <=(value: V) = SearchFilter(this, LessThan.orEqual, toValue(value))
	
	/**
	 * @param firstValue First accepted value
	 * @param secondValue Second accepted value
	 * @param moreValues More accepted values
	 * @return A filter that requires this property to match one of the specified values
	 */
	def in(firstValue: V, secondValue: V, moreValues: V*): SearchFilter =
		in(Pair(firstValue, secondValue) ++ moreValues)
	/**
	 * @param values Possible property values
	 * @return A filter that requires this property to match one of the specified values
	 */
	def in(values: Seq[V]) = SearchFilter(this, In, values.map(toValue))
	/**
	 * @param firstValue First excluded value
	 * @param secondValue Second excluded value
	 * @param moreValues More excluded values
	 * @return A filter that requires this property to not match any of the specified values
	 */
	def notIn(firstValue: V, secondValue: V, moreValues: V*): SearchFilter =
		notIn(Pair(firstValue, secondValue) ++ moreValues)
	/**
	 * @param values Rejected property values
	 * @return A filter that requires this property to not match any of the specified values
	 */
	def notIn(values: Seq[V]) = SearchFilter(this, NotIn, values.map(toValue))
}

// TODO: At some point, add: driver_version, machine_id, cpu_arch, has_avx, cpu_cores, cpu_cores_effective, cpu_ghz,
//  cpu_ram, external, disk_bw, disk_space, bw_nvlink, gpu_max_power, gpu_max_temp, gpu_mem_bw, gpu_display_active,
//  direct_port_count, host_id, id, min_bid, mobo_name, pci_gen, pcie_bw, static_ip, os_version, ubuntu_version,
//  verification, vms_enabled
object OfferProperty
{
	// NESTED   ----------------------
	
	object BooleanOfferProperty
	{
		implicit def propertyAsFilter(property: BooleanOfferProperty): SearchFilter = property.apply(true)
	}
	sealed trait BooleanOfferProperty extends OfferProperty[Boolean]
	{
		def unary_! = apply(false)
		
		override def toValue(value: Boolean): Value = value
	}
	
	sealed trait IntOfferProperty extends OfferProperty[Int]
	{
		override def toValue(value: Int): Value = value
	}
	sealed trait DoubleOfferProperty extends OfferProperty[Double]
	{
		override def toValue(value: Double): Value = value
	}
	sealed trait StringOfferProperty extends OfferProperty[String]
	{
		override def toValue(value: String): Value = value
	}
	
	sealed trait ScalingOfferProperty extends OfferProperty[Int]
	{
		/**
		 * @return Multiplier applied to accepted values
		 */
		protected def multiplier: Int
		
		override def toValue(value: Int): Value = value * multiplier
	}
	
	
	// VALUES   ----------------------
	
	/**
	 * Machine verification status (true | false)
	 */
	case object Verified extends BooleanOfferProperty
	{
		override val key: String = "verified"
	}
	/**
	 * Machine verification status
	 */
	case object VerificationStatus extends OfferProperty[VerificationStatus]
	{
		override val key: String = "verification"
		
		override def toValue(value: VerificationStatus): Value = value.key
	}
	/**
	 * Whether machine is rentable
	 */
	case object Rentable extends BooleanOfferProperty
	{
		override val key: String = "rentable"
	}
	/**
	 * When set to true, include offers where the calling user already has rented GPUs.
	 * This is useful for finding offers on machines you're already renting.
	 */
	case object Rented extends BooleanOfferProperty
	{
		override val key: String = "rented"
	}
	
	/**
	 * Host machine GPU architecture (e.g. nvidia, amd)
	 */
	case object GpuArchitecture extends StringOfferProperty
	{
		override val key: String = "gpu_arch"
		val nvidia = apply("nvidia")
		val amd = apply("amd")
	}
	/**
	 * GPU model name, such as "RTX_4090" or "RTX_3090"
	 */
	case object GpuName extends StringOfferProperty
	{
		// ATTRIBUTES   -----------------------
		
		override val key: String = "gpu_name"
	}
	/**
	 * Number of GPUs
	 */
	case object NumberOfGpus extends IntOfferProperty
	{
		// ATTRIBUTES   -----------------------
		
		override val key: String = "num_gpus"
	}
	
	object GpuRam
	{
		/**
		 * GPU RAM in MB
		 */
		val megas = apply(1)
		/**
		 * GPU RAM in GB
		 */
		val gigas = apply(1000)
	}
	/**
	 * GPU RAM
	 * @param megaMultiplier A multiplier applied to the accepted value, in order to convert it to MB
	 */
	case class GpuRam(megaMultiplier: Int) extends ScalingOfferProperty
	{
		// ATTRIBUTES   -------------------
		
		override val key: String = "gpu_ram"
		
		
		// COMPUTED -----------------------
		
		def acrossAllGpus = GpuTotalRam(megaMultiplier)
		
		
		// IMPLEMENTED  -------------------
		
		override protected def multiplier: Int = megaMultiplier
	}
	/**
	 * Total GPU RAM across all GPUs
	 * @param megaMultiplier A multiplier applied to the accepted value, in order to convert it to MB
	 */
	case class GpuTotalRam(megaMultiplier: Int) extends ScalingOfferProperty
	{
		override val key: String = "gpu_total_ram"
		
		override protected def multiplier: Int = megaMultiplier
	}
	case object GpuFraction extends DoubleOfferProperty
	{
		override val key: String = "gpu_frac"
		
		/**
		 * @return Requires the full GPU to be available
		 */
		def all = apply(1.0)
	}
	/**
	 * Maximum supported CUDA version
	 */
	case object MaxCudaVersion extends DoubleOfferProperty
	{
		override val key: String = "cuda_max_good"
	}
	/**
	 * Compute capability (CC) defines the hardware features and supported instructions
	 * for each NVIDIA GPU architecture.
	 *
	 * See: https://developer.nvidia.com/cuda/gpus
	 */
	case object CudaComputeCapability extends OfferProperty[Double]
	{
		override val key: String = "compute_cap"
		
		override def toValue(value: Double): Value = (value * 100).round.toInt
	}
	
	trait NetworkPropertyFactory[+P]
	{
		def download = apply(Download)
		def upload = apply(Upload)
		
		def apply(direction: NetworkTrafficDirection): P
	}
	
	object NetworkSpeed
	{
		val megas = NetworkSpeedFactory(1)
		val gigas = NetworkSpeedFactory(1000)
	}
	case class NetworkSpeedFactory(megaMultiplier: Int) extends NetworkPropertyFactory[NetworkSpeed]
	{
		override def apply(direction: NetworkTrafficDirection): NetworkSpeed = NetworkSpeed(megaMultiplier, direction)
	}
	/**
	 * Download or upload bandwidth
	 * @param megaMultiplier Multiplier applied to accepted values, in order to get MB
	 * @param direction Direction of traffic to which this speed applies (default = download)
	 */
	case class NetworkSpeed(megaMultiplier: Int, direction: NetworkTrafficDirection = Download)
		extends ScalingOfferProperty
	{
		override val key: String = s"inet_${ direction.key }"
		
		override protected def multiplier: Int = megaMultiplier
	}
	object NetworkCost
	{
		val perGiga = NetworkCostFactory(1)
		val perTera = NetworkCostFactory(1000)
	}
	case class NetworkCostFactory(perGigaMultiplier: Int) extends NetworkPropertyFactory[NetworkCost]
	{
		override def apply(direction: NetworkTrafficDirection): NetworkCost = NetworkCost(perGigaMultiplier, direction)
	}
	/**
	 * @param perGigaMultiplier Multiplier applied to accepted values in order to convert them to /GB
	 * @param direction Direction of traffic to which this cost applies (default = download)
	 */
	case class NetworkCost(perGigaMultiplier: Int, direction: NetworkTrafficDirection = Download)
		extends ScalingOfferProperty
	{
		override val key: String = s"inet_${direction.key}_cost"
		
		override protected def multiplier: Int = perGigaMultiplier
	}

	/**
	 * Machine reliability score (0-1)
	 */
	case object Reliability extends DoubleOfferProperty
	{
		override val key: String = "reliability"
	}
	/**
	 * Minimum required rental duration in seconds (the offer must be available for at least this long from now).
	 */
	case object OfferDuration extends OfferProperty[Duration]
	{
		// ATTRIBUTES   ---------------------
		
		override val key: String = "duration"
		
		
		// IMPLEMENTED  ---------------------
		
		override def toValue(value: Duration): Value = value.toSeconds
	}
	
	/**
	 * Total rent cost in $/h
	 */
	case object CostPerHour extends DoubleOfferProperty
	{
		override val key: String = "dph_total"
	}
	object StorageCost
	{
		val perTera = StorageCostFactory(1000)
		val perGiga = StorageCostFactory(1)
		val perMega = StorageCostFactory(0.001)
	}
	case class StorageCostFactory(perGigaMultiplier: Double)
	{
		val perMonth = StorageCost(1, perGigaMultiplier)
		
		def perDay = per(1.days)
		def perHour = per(1.hours)
		def perMinute = per(1.minutes)
		
		def per(duration: Duration) = StorageCost(duration / 1.months.toApproximateDuration, perGigaMultiplier)
	}
	case class StorageCost(perMonthMultiplier: Double, perGigaMultiplier: Double) extends OfferProperty[Double]
	{
		override val key: String = "storage_cost"
		
		override def toValue(value: Double): Value = value * perMonthMultiplier * perGigaMultiplier
	}
	
	/**
	 * DLPerf (Deep Learning Performance) is Vast.ai's scoring function that estimates GPU performance for typical
	 * deep learning tasks. It predicts iterations/second for common workloads like training ResNet50 CNNs.
	 *
	 * Example scores: V100 ~21, 2080 Ti ~14, 1080 Ti ~10. It's most accurate for CNN training, Transformer models,
	 * and standard computer vision tasks, but less reliable for non-ML or unusual compute workloads.
	 */
	case object DeepLearningPerformance extends IntOfferProperty
	{
		override val key: String = "dlperf"
		
		def perDph = DeepLearningPerformancePerDollarPerHour
	}
	/**
	 * [[DeepLearningPerformance]]/$/h
	 */
	case object DeepLearningPerformancePerDollarPerHour extends DoubleOfferProperty
	{
		override val key: String = "dlperf_per_dphtotal"
	}
	/**
	 * TFLOP (TeraFLOPS) refers to trillions of floating-point operations per second — a raw measure of GPU compute power.
	 * On Vast.ai, the total_flops field shows total TFLOPs across all GPUs in an offer,
	 * and flops_usd shows TFLOPs per dollar. The documentation notes that DLPerf is generally more useful than raw
	 * TFLOPS for most ML tasks, since it accounts for real-world bottlenecks.
	 *
	 * This property calculates the total TFLOP across all GPUs
	 */
	case object TeraFlops extends IntOfferProperty
	{
		override val key: String = "total_flops"
		
		def perDph = TeraFlopsPerDollarPerHour
	}
	/**
	 * [[TeraFlops]]/$/h
	 */
	case object TeraFlopsPerDollarPerHour extends DoubleOfferProperty
	{
		override val key: String = "flops_per_dphtotal"
	}
	
	/**
	 * Used for representing machine's location using 2-letter country codes
	 */
	case object LocationCode extends StringOfferProperty
	{
		override val key: String = "geolocation"
	}
	/**
	 * Used for limiting the offers to datacenter machines only
	 */
	case object DataCenter extends BooleanOfferProperty
	{
		override val key: String = "datacenter"
	}
}