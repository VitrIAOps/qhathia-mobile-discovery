package com.qhathia.mobile.discovery.core.data.session

/** Reversible stand-in for the Keystore cipher; [failure] makes [decrypt] throw it. */
internal class FakeTokenCipher : TokenCipher {
    var failure: Exception? = null

    override fun encrypt(plaintext: String): String = "enc(" + plaintext.reversed() + ")"

    override fun decrypt(ciphertext: String): String {
        failure?.let { throw it }
        if (!ciphertext.startsWith("enc(") || !ciphertext.endsWith(")")) throw UnreadableTokenException()
        return ciphertext.removePrefix("enc(").removeSuffix(")").reversed()
    }
}
