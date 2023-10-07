package com.masar.download

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.SparseArray
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.masar.download.databinding.ActivityMainBinding
import com.squareup.picasso.Picasso
import java.io.File
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val SHARED_TEXT_KEY = "shared_text_key"

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handleSharedText(intent.getStringExtra(Intent.EXTRA_TEXT))

        binding.buttonDownload.setOnClickListener {
            val videoUrl = binding.linkVideo.text.toString()
            if (videoUrl.isNotEmpty()) {
                downloadVideo(videoUrl)
            } else {
                Toast.makeText(this@MainActivity, "ادخل رابط الفيديو", Toast.LENGTH_SHORT).show()
            }
        }
//        sharedLink()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedText(intent?.getStringExtra(Intent.EXTRA_TEXT))
    }

    private fun handleSharedText(sharedText: String?) {
        if (sharedText != null && isValidYouTubeUrl(sharedText)) {
            binding.linkVideo.setText(extractYouTubeVideoUrl(sharedText))
            val thumbnailUrl = extractYouTubeThumbnailUrl(sharedText)
            if (!thumbnailUrl.isNullOrEmpty()) {
                Picasso.get().load(thumbnailUrl).into(binding.video)
            }
        }
    }

    private fun isValidYouTubeUrl(url: String): Boolean {
        val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%2F|youtu.be%2F|%2Fv%2F|www.youtube.com%2Fwatch%3Fv%3D)[^&=%?\\n]*"
        return Pattern.compile(pattern).matcher(url).find()
    }

    private fun extractYouTubeVideoUrl(text: String?): String? {
        val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^&=%?\\n]*"
        val compiledPattern = Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(text)
        return if (matcher.find()) {
            "https://www.youtube.com/watch?v=${matcher.group()}"
        } else {
            null
        }
    }

    private fun extractYouTubeThumbnailUrl(videoUrl: String): String? {
        val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%2F|youtu.be%2F|%2Fv%2F|www.youtube.com%2Fwatch%3Fv%3D)[^&=%?\\n]*"
        val compiledPattern = Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(videoUrl)

        return if (matcher.find()) {
            val videoId = matcher.group()
            "https://img.youtube.com/vi/$videoId/0.jpg" // Default thumbnail quality
        } else {
            null
        }
    }

    private fun downloadVideo(videoUrl: String) {
        val youtubeExtractor = object : YouTubeExtractor(this) {
            override fun onExtractionComplete(ytFiles: SparseArray<YtFile>?, videoMeta: VideoMeta?) {
                if (ytFiles != null) {
                    val formatItems = ArrayList<String>()
                    val formats = HashMap<String, YtFile>()

                    // Populate formatItems and formats map
                    for (i in 0 until ytFiles.size()) {
                        val itag = ytFiles.keyAt(i)
                        val ytFile = ytFiles.get(itag)
                        if (ytFile != null && ytFile.format.height != -1) {
                            val formatKey = "${ytFile.format.height}"
                            if (!formats.containsKey(formatKey)) {
                                formats[formatKey] = ytFile
                                formatItems.add("Quality: $formatKey")
                                Log.d("Download1dd1", "Added format: $formatKey")
                            }
                        }
                    }

                    // Create a dialog to display available video qualities
                    val dialogBuilder = AlertDialog.Builder(this@MainActivity)
                    dialogBuilder.setTitle("اختر جودة تحميل الفيديو")
                    dialogBuilder.setItems(formatItems.toTypedArray()) { dialogInterface: DialogInterface, which: Int ->
                        val selectedFormatKey = formatItems[which].split(" ")[1].replace("p", "")
                        val selectedFormat = formats[selectedFormatKey]

                        if (selectedFormat != null) {
                            val downloadUrl = selectedFormat.url
                            val fileName = videoMeta?.title + ".mp4"
                            val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val outputFile = File(storageDir, fileName)

                            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                            request.setTitle(videoMeta?.title)
                            request.setDestinationUri(Uri.fromFile(outputFile))

                            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            val downloadId = downloadManager.enqueue(request)
                            Toast.makeText(this@MainActivity, "بدأ التحميل...", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, "فشل التحميل", Toast.LENGTH_SHORT).show()
                            Log.d("DownloadFailed", "Selected Format Index: $selectedFormatKey")
                        }
                        dialogInterface.dismiss()
                    }

                    val dialog = dialogBuilder.create()
                    dialog.show()
                } else {
                    Toast.makeText(this@MainActivity, "فشل استخراج الملفات", Toast.LENGTH_SHORT).show()
                    Log.e("DownloadFailed", "Failed to extract video information: ytFiles is null")
                }
            }
        }
        youtubeExtractor.extract(videoUrl)
    }
}
