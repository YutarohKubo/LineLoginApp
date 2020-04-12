package chom.arikui.waffle.snstestapp

import android.content.Context
import android.os.AsyncTask
import android.widget.Toast
import java.io.IOException
import java.io.PrintStream
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL

class AsyncNetworkTaskPut(private val context: Context, private val postExecuteCallback: (String) -> Unit) : AsyncTask<String, Int, String>() {

    companion object {
        //const val SERVER_URL = "http://10.89.250.110:8080/SNSDataServlet/sns_serv"
        const val SERVER_URL = "http://www.arikui.1strentalserver.info/SNSDataServlet/sns_serv"
    }

    override fun doInBackground(vararg params: String?) = StringBuilder().apply {
        try {
            val url = URL(SERVER_URL + "?token=" + params[0] + "&point=" + params[1])
            val con = url.openConnection() as HttpURLConnection
            con.requestMethod = "PUT"
            con.setRequestProperty("Content-type", "application/json; charset=utf-8")
            con.doOutput = true

            val os = con.outputStream
            val ps = PrintStream(os)
            ps.print(params[2])
            ps.close()

            val lines = con.inputStream.bufferedReader().useLines { it.toList() }
            for (line in lines) {
                append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }.toString()

    override fun onPostExecute(result: String?) {
        result?.let{ postExecuteCallback(result) }
    }
}