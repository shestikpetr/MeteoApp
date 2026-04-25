package com.shestikpetr.meteoapp.data.repository

sealed class AuthResult {
    data class Success(val username: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
