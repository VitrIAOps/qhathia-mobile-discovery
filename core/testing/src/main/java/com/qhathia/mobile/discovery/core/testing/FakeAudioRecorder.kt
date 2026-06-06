package com.qhathia.mobile.discovery.core.testing

import com.qhathia.mobile.discovery.core.domain.audio.AudioLevel
import com.qhathia.mobile.discovery.core.domain.audio.AudioRecorder
import com.qhathia.mobile.discovery.core.domain.model.RecordedAudio

/**
 * In-memory [AudioRecorder] for tests. [recorded] is what [stop] returns (set to `null` to model the
 * no-audio outcome); [level] is returned by every [pollLevel] call and can be flipped between time
 * advances to drive end-of-speech detection.
 */
class FakeAudioRecorder(
    var recorded: RecordedAudio? = VoiceSearchFixtures.recordedAudio(),
    var level: AudioLevel = AudioLevel.Speech,
) : AudioRecorder {

    var isRecording: Boolean = false
        private set
    var cancelled: Boolean = false
        private set

    override suspend fun start() {
        isRecording = true
        cancelled = false
    }

    override fun pollLevel(): AudioLevel = level

    override suspend fun stop(): RecordedAudio? {
        isRecording = false
        return recorded
    }

    override fun cancel() {
        isRecording = false
        cancelled = true
    }
}
