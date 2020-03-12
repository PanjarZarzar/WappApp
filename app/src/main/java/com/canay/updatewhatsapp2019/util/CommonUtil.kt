package com.canay.updatewhatsapp2019.util

object CommonUtil {

    fun compareVersion(currentVersion: String?, availableVersion: String?): Boolean {
        if (availableVersion.isNullOrBlank() || currentVersion.isNullOrBlank())
            return true

        val currentVersionInt = currentVersion.replace(".", "")
        val availableVersionInt = availableVersion.replace(".", "")

        if (availableVersionInt > currentVersionInt)
            return true

        return false
    }
}