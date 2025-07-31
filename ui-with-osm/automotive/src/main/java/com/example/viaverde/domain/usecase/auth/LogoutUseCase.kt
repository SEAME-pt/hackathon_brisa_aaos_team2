package com.example.viaverde.domain.usecase.auth

import com.example.viaverde.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for user logout
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {

    /**
     * Execute logout
     */
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.logout()
    }
}
