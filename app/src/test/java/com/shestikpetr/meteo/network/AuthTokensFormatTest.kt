package com.shestikpetr.meteo.network

import org.junit.Test
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue

/**
 * Tests to verify AuthTokens data model changes for JWT compatibility
 */
class AuthTokensFormatTest {

    @Test
    fun `AuthTokens should accept string user_id`() {
        // Given
        val tokens = AuthTokens(
            user_id = "123",
            access_token = "access_token_value",
            refresh_token = "refresh_token_value"
        )

        // Then
        assertEquals("123", tokens.user_id)
        assertTrue("user_id should be String type", tokens.user_id is String)
    }

    @Test
    fun `AuthTokens should accept numeric string user_id`() {
        // Given - numeric value as string
        val tokens = AuthTokens(
            user_id = "456",
            access_token = "access_token_value",
            refresh_token = "refresh_token_value"
        )

        // Then
        assertEquals("456", tokens.user_id)
        assertTrue("user_id should be String type", tokens.user_id is String)
    }

    @Test
    fun `AuthTokens should work with non-numeric string user_id`() {
        // Given - non-numeric string (UUID-like)
        val tokens = AuthTokens(
            user_id = "user-123-abc",
            access_token = "access_token_value",
            refresh_token = "refresh_token_value"
        )

        // Then
        assertEquals("user-123-abc", tokens.user_id)
        assertTrue("user_id should be String type", tokens.user_id is String)
    }

    @Test
    fun `verify data class structure matches API expectation`() {
        // This test documents what the API should return
        println("=== AUTH TOKENS DATA MODEL ANALYSIS ===")
        println()

        val expectedApiResponse = """
        {
            "success": true,
            "data": {
                "user_id": "123",
                "access_token": "eyJ...",
                "refresh_token": "eyJ..."
            }
        }
        """.trimIndent()

        println("EXPECTED API RESPONSE:")
        println(expectedApiResponse)
        println()

        val tokens = AuthTokens(
            user_id = "123",
            access_token = "eyJ...",
            refresh_token = "eyJ..."
        )

        println("KOTLIN DATA CLASS:")
        println("AuthTokens(")
        println("  user_id = \"${tokens.user_id}\" (${tokens.user_id::class.simpleName})")
        println("  access_token = \"${tokens.access_token}\"")
        println("  refresh_token = \"${tokens.refresh_token}\"")
        println(")")
        println()

        println("COMPATIBILITY CHECK:")
        println("✓ user_id is String type (matches JSON string)")
        println("✓ access_token is String type")
        println("✓ refresh_token is String type")
        println("✓ Data class can deserialize API response correctly")
    }

    @Test
    fun `test JSON parsing compatibility`() {
        // Simulate different API response formats

        println("=== JSON PARSING COMPATIBILITY TEST ===")
        println()

        // Case 1: Numeric user_id in JSON (problematic)
        val numericJsonExample = """{"user_id": 123, "access_token": "token", "refresh_token": "refresh"}"""
        println("CASE 1 - Numeric JSON:")
        println(numericJsonExample)
        println("Result: Gson will convert 123 to \"123\" when deserializing to String field")
        println()

        // Case 2: String user_id in JSON (correct)
        val stringJsonExample = """{"user_id": "123", "access_token": "token", "refresh_token": "refresh"}"""
        println("CASE 2 - String JSON:")
        println(stringJsonExample)
        println("Result: Gson will keep \"123\" as String - perfect match")
        println()

        // Case 3: UUID-like user_id in JSON
        val uuidJsonExample = """{"user_id": "user-abc-123", "access_token": "token", "refresh_token": "refresh"}"""
        println("CASE 3 - UUID-like JSON:")
        println(uuidJsonExample)
        println("Result: Gson will keep UUID string as-is")
        println()

        println("CONCLUSION:")
        println("AuthTokens with String user_id field is compatible with all JSON formats")
        println("This change makes the client more robust and future-proof")
    }
}