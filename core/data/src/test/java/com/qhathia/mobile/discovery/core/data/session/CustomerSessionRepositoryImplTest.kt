package com.qhathia.mobile.discovery.core.data.session

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CustomerSessionRepositoryImplTest {

    private val repository = CustomerSessionRepositoryImpl()

    @Test
    fun same_id_until_the_session_ends() = runTest {
        val first = repository.currentSessionId()

        assertEquals(first, repository.currentSessionId())
        assertTrue(first.isNotBlank())
        assertTrue(first.length <= SERVER_MAX_LENGTH)
    }

    @Test
    fun the_next_customer_gets_a_new_id() = runTest {
        val first = repository.currentSessionId()

        repository.end()

        assertNotEquals(first, repository.currentSessionId())
    }

    @Test
    fun first_calls_from_several_threads_agree() = runTest {
        val ids = withContext(Dispatchers.Default) {
            (1..32).map { async { repository.currentSessionId() } }.awaitAll()
        }

        assertEquals(1, ids.toSet().size)
    }

    private companion object {
        // biz and vo accept session ids up to 200 characters (api-contract §24, §41).
        const val SERVER_MAX_LENGTH = 200
    }
}
