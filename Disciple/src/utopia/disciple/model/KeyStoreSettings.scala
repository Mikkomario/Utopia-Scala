package utopia.disciple.model

import java.nio.file.Path

/**
  * Settings used for accessing a keystore instance
  * @author Mikko Hilpinen
  * @since 3.12.2021, v1.5
  * @param storePath Path to a keystore file
  * @param storeType Type of keystore used. E.g. "JKS" or "PKCS12".
  *                  None indicates that KeyStore.getDefaultType() should be used (default)
  * @param storePassword Password used when accessing the key store (default = None = store doesn't use password)
  * @param keyPassword Password used when accessing the specific key (default = None = no specific key password used)
  * @param throwErrors Whether keystore initialization should throw (true) or catch & print (false) errors.
  *                    Default = false.
  */
case class KeyStoreSettings(storePath: Path, storeType: Option[String] = None, storePassword: Option[String] = None,
                            keyPassword: Option[String] = None, throwErrors: Boolean = false)
