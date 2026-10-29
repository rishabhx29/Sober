package com.sober.system.services.di

import com.sober.core.domain.repository.AlertNotifier
import com.sober.core.domain.repository.EnforcementController
import com.sober.system.services.core.AlertNotifierImpl
import com.sober.system.services.core.EnforcementControllerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EnforcementModule {

    @Binds
    @Singleton
    abstract fun bindEnforcementController(
        impl: EnforcementControllerImpl
    ): EnforcementController

    @Binds
    @Singleton
    abstract fun bindAlertNotifier(
        impl: AlertNotifierImpl
    ): AlertNotifier
}
