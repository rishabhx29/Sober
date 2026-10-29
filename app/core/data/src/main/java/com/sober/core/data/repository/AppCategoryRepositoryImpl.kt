package com.sober.core.data.repository

import android.content.Context
import com.sober.core.common.presets.PresetResolver
import com.sober.core.domain.repository.AppCategoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppCategoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppCategoryRepository {

    override fun getSocialPackages(): Set<String> = PresetResolver.socialPackages(context)
}
