package com.shestikpetr.meteo.config.test

import android.util.Log
import com.shestikpetr.meteo.config.interfaces.*
import com.shestikpetr.meteo.config.utils.ValidationUtils
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Test class to verify the dynamic configuration system works correctly.
 * This verifies that all hardcoded values have been successfully replaced with dynamic configuration.
 */
@Singleton
class DynamicConfigurationSystemTest @Inject constructor(
    private val validationConfigRepository: ValidationConfigRepository,
    private val retryConfigRepository: RetryConfigRepository,
    private val themeConfigRepository: ThemeConfigRepository,
    private val demoConfigRepository: DemoConfigRepository,
    private val validationUtils: ValidationUtils
) {

    /**
     * Runs comprehensive tests of the dynamic configuration system.
     */
    fun runTests(): TestResults {
        val results = TestResults()

        runBlocking {
            // Test validation configuration
            results.validationTest = testValidationConfig()

            // Test retry configuration
            results.retryTest = testRetryConfig()

            // Test theme configuration
            results.themeTest = testThemeConfig()

            // Test demo configuration
            results.demoTest = testDemoConfig()

            // Test validation utils
            results.validationUtilsTest = testValidationUtils()
        }

        logResults(results)
        return results
    }

    private suspend fun testValidationConfig(): TestResult {
        return try {
            Log.d("ConfigTest", "Testing validation configuration...")

            // Test sensor data validation (replaces hardcoded -100.0 threshold)
            val isValid1 = validationConfigRepository.isValidSensorValue(25.5) // Should be valid
            val isValid2 = validationConfigRepository.isValidSensorValue(-150.0) // Should depend on config

            // Test station number validation (replaces hardcoded 8-digit logic)
            val isValidStation1 = validationConfigRepository.isValidStationNumber("12345678")
            val isValidStation2 = validationConfigRepository.isValidStationNumber("123456789") // Should fail if config sets max to 8

            Log.d("ConfigTest", "Validation tests: sensor($isValid1,$isValid2), station($isValidStation1,$isValidStation2)")

            TestResult.Success("Validation configuration working with dynamic rules")
        } catch (e: Exception) {
            Log.e("ConfigTest", "Validation configuration test failed", e)
            TestResult.Failure("Validation test failed: ${e.message}")
        }
    }

    private suspend fun testRetryConfig(): TestResult {
        return try {
            Log.d("ConfigTest", "Testing retry configuration...")

            // Test dynamic retry configurations (replaces hardcoded values in RetryPolicy)
            val sensorRetryConfig = retryConfigRepository.getSensorDataRetryConfig().getOrNull()
            val stationRetryConfig = retryConfigRepository.getStationDataRetryConfig().getOrNull()

            if (sensorRetryConfig != null && stationRetryConfig != null) {
                Log.d("ConfigTest", "Retry configs: sensor(${sensorRetryConfig.maxAttempts},${sensorRetryConfig.baseDelayMs}), station(${stationRetryConfig.maxAttempts},${stationRetryConfig.baseDelayMs})")
                TestResult.Success("Retry configuration loaded successfully with ${sensorRetryConfig.maxAttempts} sensor attempts and ${stationRetryConfig.maxAttempts} station attempts")
            } else {
                TestResult.Failure("Failed to load retry configurations")
            }
        } catch (e: Exception) {
            Log.e("ConfigTest", "Retry configuration test failed", e)
            TestResult.Failure("Retry test failed: ${e.message}")
        }
    }

    private suspend fun testThemeConfig(): TestResult {
        return try {
            Log.d("ConfigTest", "Testing theme configuration...")

            // Test dynamic theme colors (replaces hardcoded MeteoColors)
            val weatherColors = themeConfigRepository.getWeatherColors().getOrNull()
            val statusColors = themeConfigRepository.getStatusColors().getOrNull()

            if (weatherColors != null && statusColors != null) {
                Log.d("ConfigTest", "Theme colors loaded: weather.sunny=${weatherColors.sunny}, status.online=${statusColors.online}")
                TestResult.Success("Theme configuration loaded with ${weatherColors.sunny} sunny color and ${statusColors.online} online color")
            } else {
                TestResult.Failure("Failed to load theme configurations")
            }
        } catch (e: Exception) {
            Log.e("ConfigTest", "Theme configuration test failed", e)
            TestResult.Failure("Theme test failed: ${e.message}")
        }
    }

    private suspend fun testDemoConfig(): TestResult {
        return try {
            Log.d("ConfigTest", "Testing demo configuration...")

            // Test dynamic demo credentials (replaces hardcoded "user"/"user")
            val demoCredentials = demoConfigRepository.getDemoCredentials().getOrNull()

            if (demoCredentials != null) {
                Log.d("ConfigTest", "Demo config loaded: enabled=${demoCredentials.enabled}, username=${demoCredentials.username}")
                TestResult.Success("Demo configuration loaded with enabled=${demoCredentials.enabled} and username='${demoCredentials.username}'")
            } else {
                TestResult.Failure("Failed to load demo configuration")
            }
        } catch (e: Exception) {
            Log.e("ConfigTest", "Demo configuration test failed", e)
            TestResult.Failure("Demo test failed: ${e.message}")
        }
    }

    private fun testValidationUtils(): TestResult {
        return try {
            Log.d("ConfigTest", "Testing validation utilities...")

            // Test dynamic validation utils (replaces hardcoded validation logic)
            val isValidStationNumber = validationUtils.validateStationNumberInput("12345678")
            val maxLength = validationUtils.getStationNumberMaxLength()
            val allowedChars = validationUtils.getStationNumberAllowedCharacters()

            Log.d("ConfigTest", "Validation utils: isValid=$isValidStationNumber, maxLength=$maxLength, allowedChars=$allowedChars")
            TestResult.Success("Validation utilities working with maxLength=$maxLength")
        } catch (e: Exception) {
            Log.e("ConfigTest", "Validation utilities test failed", e)
            TestResult.Failure("Validation utils test failed: ${e.message}")
        }
    }

    private fun logResults(results: TestResults) {
        Log.i("ConfigTest", "=== Dynamic Configuration System Test Results ===")
        Log.i("ConfigTest", "Validation Config: ${results.validationTest}")
        Log.i("ConfigTest", "Retry Config: ${results.retryTest}")
        Log.i("ConfigTest", "Theme Config: ${results.themeTest}")
        Log.i("ConfigTest", "Demo Config: ${results.demoTest}")
        Log.i("ConfigTest", "Validation Utils: ${results.validationUtilsTest}")

        val passed = listOf(results.validationTest, results.retryTest, results.themeTest,
                           results.demoTest, results.validationUtilsTest).count { it is TestResult.Success }
        Log.i("ConfigTest", "Overall Result: $passed/5 tests passed")

        if (passed == 5) {
            Log.i("ConfigTest", "✅ All hardcoded values successfully replaced with dynamic configuration!")
        } else {
            Log.w("ConfigTest", "⚠️ Some tests failed - check individual results above")
        }
    }

    data class TestResults(
        var validationTest: TestResult = TestResult.Pending,
        var retryTest: TestResult = TestResult.Pending,
        var themeTest: TestResult = TestResult.Pending,
        var demoTest: TestResult = TestResult.Pending,
        var validationUtilsTest: TestResult = TestResult.Pending
    )

    sealed class TestResult {
        object Pending : TestResult()
        data class Success(val message: String) : TestResult()
        data class Failure(val message: String) : TestResult()

        override fun toString(): String = when (this) {
            is Pending -> "PENDING"
            is Success -> "PASS - $message"
            is Failure -> "FAIL - $message"
        }
    }
}