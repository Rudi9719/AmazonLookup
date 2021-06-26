package dev.synople.amazonlookup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.zxing.integration.android.IntentIntegrator
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        IntentIntegrator(this).initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                val upc = result.contents
                Log.v("MainActivity", "UPC: $upc")

                thread {
                    val asin = getAsin(upc)
                    if ("" == asin) {
                        finishAffinity()
                        return@thread
                    }
                    Log.v("MainActivity", "ASIN: $asin")
                    val detailPage = getDetailPage(asin)
                    runOnUiThread {
                        if (detailPage.contains("Amazon's Choice")) {
                            Toast.makeText(this, "Amazon's Choice!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Not Amazon's Choice!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    finishAffinity()

                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun getDetailPage(asin: String): String {
        if ("" == asin) {
            return ""
        }
        val url = URL("https://www.amazon.com/dp/$asin")
        val urlConnection = url.openConnection()
        try {
            val bufferedReader = BufferedReader(InputStreamReader(urlConnection.getInputStream()))
            var inputLine = bufferedReader.readLine()
            val responseBuilder = StringBuilder()

            while (inputLine != null) {
                responseBuilder.append(inputLine)
                inputLine = bufferedReader.readLine()
            }

            bufferedReader.close()

            return responseBuilder.toString()
        } catch (e: Exception) {
            Log.v("MainActiity.getAsin", "Exception in getting website", e.cause)
            runOnUiThread {
                Toast.makeText(this, "Unable to get Detail Page", Toast.LENGTH_LONG).show()
            }

        }
        return ""
    }

    private fun getAsin(upc: String): String {
        val url = URL("https://www.amazon.com/s?k=$upc")
        val urlConnection = url.openConnection()
        try {
            val bufferedReader = BufferedReader(InputStreamReader(urlConnection.getInputStream()))
            var inputLine = bufferedReader.readLine()
            val responseBuilder = StringBuilder()

            while (inputLine != null) {
                responseBuilder.append(inputLine)
                inputLine = bufferedReader.readLine()
            }

            bufferedReader.close()

            val website = responseBuilder.toString()
            // (?<=asin=").{10}

            for (i in 0..website.length - 20) {
                if (website[i] == 'a' && website[i + 1] == 's' && website[i + 2] == 'i' && website[i + 3] == 'n' && website[i + 4] == '=' && website[i + 5] == '"') {
                    return website.substring(i + 6, i + 16)
                }
            }
        } catch (e: Exception) {
            Log.v("MainActiity.getAsin", "Exception in getting website: " + e.localizedMessage, e.cause)
            runOnUiThread {
                Toast.makeText(this, "Unable to get ASIN, check internet connection.", Toast.LENGTH_LONG).show()
            }
        }
        return ""
    }

}
