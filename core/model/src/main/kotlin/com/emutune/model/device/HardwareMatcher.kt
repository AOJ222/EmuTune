package com.emutune.model.device

/**
 * Compares an observation's device fingerprint against the current user's device and
 * returns how closely they match. Matching fails closed: an absent SoC identity never
 * upgrades a comparison, it degrades to [HardwareMatchQuality.UNKNOWN_HARDWARE].
 */
class HardwareMatcher {

    fun match(target: DeviceFingerprint, sample: DeviceFingerprint): HardwareMatchQuality {
        if (isSameDevice(target, sample)) return HardwareMatchQuality.EXACT_DEVICE
        if (isSameSocAndGpu(target, sample)) return HardwareMatchQuality.EXACT_SOC_GPU
        if (isSameVendor(target, sample)) return HardwareMatchQuality.RELATED_HARDWARE
        return HardwareMatchQuality.UNKNOWN_HARDWARE
    }

    private fun isSameDevice(target: DeviceFingerprint, sample: DeviceFingerprint): Boolean {
        val targetModel = target.model ?: return false
        val targetCodename = target.deviceCodename ?: return false
        return targetModel == sample.model && targetCodename == sample.deviceCodename
    }

    private fun isSameSocAndGpu(target: DeviceFingerprint, sample: DeviceFingerprint): Boolean {
        val targetSoc = target.socModel ?: return false
        val sampleSoc = sample.socModel ?: return false
        val targetGpu = targetSoc.gpu ?: return false
        return targetSoc.name == sampleSoc.name && targetGpu == sampleSoc.gpu
    }

    private fun isSameVendor(target: DeviceFingerprint, sample: DeviceFingerprint): Boolean {
        val targetSoc = target.socModel ?: return false
        val sampleSoc = sample.socModel ?: return false
        if (targetSoc.vendor == UNKNOWN_VENDOR || sampleSoc.vendor == UNKNOWN_VENDOR) return false
        return targetSoc.vendor == sampleSoc.vendor
    }

    private companion object {
        const val UNKNOWN_VENDOR = "Unknown"
    }
}
