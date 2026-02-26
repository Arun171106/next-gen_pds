package com.example.nextgen_pds_kiosk.di

import android.content.Context
import com.example.nextgen_pds_kiosk.voice.IntentParser
import com.example.nextgen_pds_kiosk.voice.VoiceManager
import com.example.nextgen_pds_kiosk.voice.VoiceSetupHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VoiceModule {

    @Provides
    @Singleton
    fun provideVoiceAssistant(
        @ApplicationContext context: Context,
        intentParser: IntentParser,
        voiceSetupHelper: VoiceSetupHelper
    ): VoiceManager {
        return VoiceManager(context, intentParser, voiceSetupHelper)
    }
}
