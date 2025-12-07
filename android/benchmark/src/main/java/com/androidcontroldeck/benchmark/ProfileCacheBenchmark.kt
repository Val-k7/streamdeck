package com.androidcontroldeck.benchmark

import android.content.Intent
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MacrobenchmarkRule
import androidx.benchmark.macro.TraceSectionMetric
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileCacheBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun profileAndIconCache() = benchmarkRule.measureRepeated(
        packageName = "com.androidcontroldeck",
        metrics = listOf(
            TraceSectionMetric("ProfileCache.load"),
            TraceSectionMetric("AssetCache.icon"),
        ),
        iterations = 3,
        setupBlock = {
            pressHome()
            val intent = Intent(Intent.ACTION_MAIN).apply {
                setPackage(packageName)
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivityAndWait(intent)
        }
    ) {
        device.waitForIdle()
    }
}
