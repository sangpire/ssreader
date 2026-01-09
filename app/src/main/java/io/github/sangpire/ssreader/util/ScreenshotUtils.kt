package io.github.sangpire.ssreader.util

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.core.graphics.applyCanvas
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 스크린샷 캡처 및 저장 유틸리티
 */
object ScreenshotUtils {

    /**
     * View를 Bitmap으로 캡처
     *
     * @param view 캡처할 View
     * @return 캡처된 Bitmap
     */
    fun captureView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        bitmap.applyCanvas {
            view.draw(this)
        }
        return bitmap
    }

    /**
     * Window 전체를 Bitmap으로 캡처 (SurfaceView 포함)
     * Android 8.0 이상에서 PixelCopy API 사용
     *
     * @param window Activity Window
     * @return 캡처된 Bitmap, 실패 시 null
     */
    suspend fun captureWindow(window: Window): Bitmap? = suspendCoroutine { continuation ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val view = window.decorView.rootView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

            try {
                PixelCopy.request(
                    window,
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            continuation.resume(bitmap)
                        } else {
                            continuation.resume(null)
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } catch (e: Exception) {
                e.printStackTrace()
                continuation.resume(null)
            }
        } else {
            // Android 8.0 미만에서는 일반 View 캡처 사용
            continuation.resume(captureView(window.decorView.rootView))
        }
    }

    /**
     * Bitmap을 갤러리에 저장
     *
     * @param context Context
     * @param bitmap 저장할 Bitmap
     * @param fileName 파일 이름 (확장자 제외)
     * @return 저장 성공 여부
     */
    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String = generateFileName()
    ): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 이상: MediaStore 사용
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SSReader")
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                uri?.let { imageUri ->
                    context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                    }
                    true
                } ?: false
            } else {
                // Android 9 이하: 직접 파일 저장
                val picturesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                )
                val appDir = File(picturesDir, "SSReader")
                if (!appDir.exists()) {
                    appDir.mkdirs()
                }

                val imageFile = File(appDir, "$fileName.jpg")
                FileOutputStream(imageFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                }

                // 갤러리에 알림
                @Suppress("DEPRECATION")
                context.sendBroadcast(
                    android.content.Intent(
                        android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        android.net.Uri.fromFile(imageFile)
                    )
                )
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 타임스탬프 기반 파일 이름 생성
     *
     * @return "SSReader_yyyyMMdd_HHmmss" 형식의 파일 이름
     */
    private fun generateFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "SSReader_$timestamp"
    }
}
