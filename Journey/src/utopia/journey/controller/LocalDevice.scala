package utopia.journey.controller

import utopia.flow.collection.template.typeless
import utopia.flow.collection.value.typeless.ModelValidationFailedException
import utopia.flow.parse.file.container.OptionObjectFileContainer
import utopia.flow.datastructure.immutable.Value
import utopia.flow.datastructure.template
import utopia.flow.generic.model
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.{FromModelFactory, FromModelFactoryWithSchema}
import utopia.flow.generic.model.immutable.{Model, ModelValidationFailedException, Value}
import utopia.flow.generic.model.mutable.{IntType, StringType}
import utopia.flow.generic.model.template.{ModelConvertible, Property}
import utopia.flow.util.CollectionExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.metropolis.model.combined.device.DetailedClientDevice
import utopia.journey.util.JourneyContext._

/**
  * Used for accessing information about the local client device
  * @author Mikko Hilpinen
  * @since 21.6.2020, v0.1
  */
object LocalDevice
{
	// ATTRIBUTES	-----------------------------
	
	private val container = new OptionObjectFileContainer(containersDirectory/"device.json", DeviceStatus)
	
	
	// COMPUTED	---------------------------------
	
	/**
	  * @return Whether this device has been initialized / set up
	  */
	def isInitialized = container.nonEmpty
	
	/**
	  * @return Whether this device data hasn't been initialized yet
	  */
	def notInitialized = !isInitialized
	
	/**
	  * @return This device's unique id
	  */
	def id = container.current.map { _.data match
	{
		case Right(full) => full.id
		case Left(partial) => partial.id
	} }
	
	/**
	  * @return Current device authorization key
	  */
	def key = container.current.flatMap { _.key }
	
	/**
	  * Updates the device authorization key. This device must be at least preInitialized at this point
	  * @param newKey Authorization key to store
	  */
	def key_=(newKey: String) = container.current = container.current.map { _.copy(key = Some(newKey)) }
	
	
	// OTHER	---------------------------------
	
	/**
	  * Sets up initial device status
	  * @param deviceId Id of this device
	  * @param deviceName Name of this device in some language
	  * @param firstUserId Id of the first user of this device
	  */
	def preInitialize(deviceId: Int, deviceName: String, firstUserId: Int) =
		container.current = Some(DeviceStatus(Left(PartialDeviceData(deviceId, deviceName, firstUserId))))
	
	/**
	  * Sets up device status
	  * @param data Device data
	  */
	def initialize(data: DetailedClientDevice) = container.current = Some(DeviceStatus(Right(data), key))
	
	
	// NESTED	---------------------------------
	
	private object DeviceStatus extends FromModelFactory[DeviceStatus]
	{
		override def apply(model: model.template.Model[Property]) =
		{
			val key = model("key").string
			
			model("full").model match
			{
				case Some(fullModel) => DetailedClientDevice(fullModel).map { d => DeviceStatus(Right(d), key) }
				case None =>
					model("partial").model
						.toTry { new ModelValidationFailedException(s"Either 'full' or 'partial' required. Provided: $model") }
						.flatMap { PartialDeviceData(_) }
						.map { d => DeviceStatus(Left(d), key) }
			}
		}
	}
	
	private case class DeviceStatus(data: Either[PartialDeviceData, DetailedClientDevice], key: Option[String] = None)
		extends ModelConvertible
	{
		override def toModel =
		{
			val prop: (String, Value) = data match
			{
				case Right(full) => "full" -> full.toModel
				case Left(partial) => "partial" -> partial.toModel
			}
			Model(Vector(prop, "key" -> key))
		}
	}
	
	private object PartialDeviceData extends FromModelFactoryWithSchema[PartialDeviceData]
	{
		override val schema = ModelDeclaration("id" -> IntType, "name" -> StringType, "user_id" -> StringType)
		
		override protected def fromValidatedModel(model: Model) =
			PartialDeviceData(model("id"), model("name"), model("user_id"))
	}
	
	private case class PartialDeviceData(id: Int, name: String, userId: Int) extends ModelConvertible
	{
		override def toModel = Model(Vector("id" -> id, "name" -> name, "user_id" -> userId))
	}
}
