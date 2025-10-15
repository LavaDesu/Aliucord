package com.aliucord

import android.content.Context
import android.content.res.AssetManager
import com.aliucord.R.drawable.baseline_poll_24
import java.io.File

object ResourceLoader {
    fun yes(context: Context) {
        val assets = context.resources.assets
        // val dexFile = Utils.appActivity.codeCacheDir.resolve("Aliucord.zip")
        // val dexFile = Utils.appActivity.codeCacheDir.resolve("Aliucord.custom.zip")
        val dexFile = File("/storage/emulated/0/Aliucord/Aliucord.zip")
        val eee = AssetManager::class.java.newInstance()
        AssetManager::class.java.getMethod("addAssetPath", String::class.java)(assets, dexFile.absolutePath)
        AssetManager::class.java.getMethod("addAssetPath", String::class.java)(eee, dexFile.absolutePath)
        // val e = AssetManager::class.java.getMethod("getAssignedPackageIdentifiers")(assets) as SparseArray<String>
        // var il = 0;
        // while (il < e.size()) {
        //     Utils.log("xddd " + e.get(il))
        //     il += 1
        // }


        with(context.resources) {
            Utils.log("hiiii")
            // ContextThemeWrapper::class.java.getDeclaredField("mResources").apply {
            //     isAccessible = true
            //     @Suppress("DEPRECATION")
            //     set(context, Resources(assets, displayMetrics, configuration))
            // }
            try {
                val e = context.resources.getDrawable(baseline_poll_24)
                Utils.log(e.toString())
            } catch (e: Throwable) {
                Utils.log(e.toString())
            }
        }
    }
}
