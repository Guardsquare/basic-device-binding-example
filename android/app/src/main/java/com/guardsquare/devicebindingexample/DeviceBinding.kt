package com.guardsquare.devicebindingexample

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProperties.PURPOSE_SIGN
import android.security.keystore.KeyProperties.PURPOSE_VERIFY
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.InvalidAlgorithmParameterException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature


class DeviceBinding() {
    private var ks: KeyStore = KeyStore.getInstance("AndroidKeyStore")
    private val KEY_ALIAS = "DEV_SIGN"

    @Throws(
        NoSuchAlgorithmException::class,
        NoSuchProviderException::class,
        InvalidAlgorithmParameterException::class
    )
    fun initializeKeyStore(context: Context): KeyStore {

        ks.load(null)
        if (!ks.containsAlias(KEY_ALIAS)) {
            generateECKeyPair(KEY_ALIAS, context)
        }
        return ks

    }


    @Throws(
        NoSuchAlgorithmException::class,
        NoSuchProviderException::class,
        InvalidAlgorithmParameterException::class
    )
    private fun generateECKeyPair(alias: String, context: Context): KeyPair? {
        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance("EC", "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(
            alias,
            PURPOSE_SIGN or PURPOSE_VERIFY
        )

        builder.setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .setDigests(
                KeyProperties.DIGEST_SHA256 /* DIGEST_SHA512 causes crashes on some devices */
            )
            .setKeySize(256)

        // Disabled for compatibility reasons. Enable this feature in your project for compatible devices
        //        builder.setIsStrongBoxBacked(true)
        kpg.initialize(builder.build())

        return kpg.generateKeyPair()
    }


    fun getDevicePublicKey(): PublicKey? {
        return try {
            ks.getCertificate(KEY_ALIAS).publicKey
        } catch (e: Exception) {
            e.printStackTrace()
            throw java.lang.RuntimeException("Exception: ", e)
        }
    }

    fun getDevicePrivateKey(): PrivateKey? {
        return try {
            ks.getKey(KEY_ALIAS, null) as PrivateKey
        } catch (e: Exception) {
            e.printStackTrace()
            throw java.lang.RuntimeException("Exception: ", e)
        }
    }

    fun signSHA256withECDSA(key: PrivateKey?, input: ByteArray?): ByteArray? {
        val signatureBytes: ByteArray
        try {
            val signature: Signature = Signature.getInstance("SHA256withECDSA")
            signature.initSign(key)
            val inStream: InputStream = ByteArrayInputStream(input)
            val buff = ByteArray(256)
            var read: Int
            while (inStream.read(buff).also { read = it } > 0) {
                signature.update(buff, 0, read)
            }
            signatureBytes = signature.sign()
        } catch (e: Exception) {
            e.printStackTrace()
            throw java.lang.RuntimeException("Singing failed", e)
        }
        return signatureBytes
    }

    fun resetDeviceBinding(context: Context){
        ks.deleteEntry(KEY_ALIAS)
    }

}