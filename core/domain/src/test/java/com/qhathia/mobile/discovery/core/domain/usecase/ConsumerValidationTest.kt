package com.qhathia.mobile.discovery.core.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ConsumerValidationTest {

    private val validateName = ValidateConsumerNameUseCase()
    private val validatePhone = ValidateConsumerPhoneUseCase()

    @Test
    fun `blank name is Empty`() {
        assertEquals(ConsumerNameError.Empty, validateName("   "))
    }

    @Test
    fun `single letter token has no referential name`() {
        assertEquals(ConsumerNameError.NoReferentialName, validateName("a"))
    }

    @Test
    fun `name with a referential name is valid`() {
        assertNull(validateName("María"))
        assertNull(validateName("Ana Pérez"))
    }

    @Test
    fun `blank phone is Empty`() {
        assertEquals(ConsumerPhoneError.Empty, validatePhone(""))
    }

    @Test
    fun `peruvian mobile with spaces is valid`() {
        assertNull(validatePhone("987 654 321"))
    }

    @Test
    fun `peruvian mobile with country code is valid`() {
        assertNull(validatePhone("+51987654321"))
    }

    @Test
    fun `landline or short number is invalid`() {
        assertEquals(ConsumerPhoneError.InvalidFormat, validatePhone("12345"))
        assertEquals(ConsumerPhoneError.InvalidFormat, validatePhone("887654321"))
    }
}
