package chom.arikui.waffle.snstestapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.linecorp.linesdk.LineApiResponseCode
import com.linecorp.linesdk.LineCredential
import com.linecorp.linesdk.LoginDelegate
import com.linecorp.linesdk.Scope
import com.linecorp.linesdk.auth.LineAuthenticationParams
import com.linecorp.linesdk.auth.LineLoginApi
import com.linecorp.linesdk.auth.LineLoginResult
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
        const val REQUEST_CODE_LINE_LOGIN = 1
        const val TEN_SECOND = 10000
    }

    private lateinit var mImageProfile: ImageView
    private lateinit var mTextUserName: TextView
    private lateinit var mButtonPoint: Button
    private lateinit var mTextPoint: TextView

    private var isLoginUser = false
    private var credential: LineCredential? = null
    private var point = 0

    private var pleaseLoginToast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val buttonLineLogin = line_login_btn
        buttonLineLogin.setChannelId(resources.getString(R.string.line_login_channel_id))
        buttonLineLogin.enableLineAppAuthentication(true)
        buttonLineLogin.setAuthenticationParams(
            LineAuthenticationParams.Builder().scopes(
                listOf(
                    Scope.PROFILE
                )
            ).build()
        )
        val loginDelegate = LoginDelegate.Factory.create()
        buttonLineLogin.setLoginDelegate(loginDelegate)
        /*buttonLineLogin.setOnClickListener {
            try {
                val loginIntent = LineLoginApi.getLoginIntent(
                    this,
                    resources.getString(R.string.line_login_channel_id),
                    LineAuthenticationParams.Builder().scopes(listOf(Scope.PROFILE)).build()
                )
                startActivityForResult(loginIntent, REQUEST_CODE_LINE_LOGIN)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }*/

        mImageProfile = image_profile as ImageView
        mTextUserName = text_name as TextView
        mButtonPoint = button_point as Button
        mTextPoint = text_point as TextView

        mButtonPoint.setOnClickListener {
            //ログイン済みであるとき
            if (isLoginUser) {
                val accessToken = credential?.accessToken
                Log.i(TAG, "expiresInMillis of accessToken = " + accessToken?.expiresInMillis)
                //accessTokenの有効期限が残り10秒以上であれば、ポイントの更新を行う
                if (accessToken != null && accessToken.expiresInMillis > TEN_SECOND) {
                    val taskPut = AsyncNetworkTaskPut(this) { str ->
                        when(str) {
                            "Success in updating your point" -> {
                                mTextPoint.text = point.toString()
                            }
                        }
                    }
                    point++
                    taskPut.execute(accessToken.tokenString, (point).toString(), "")
                }
                //accessTokenの有効期限が残り10秒以下であれば、再ログインを促す
                else {
                    pleaseLoginToast?.cancel()
                    pleaseLoginToast = Toast.makeText(
                        this,
                        resources.getString(R.string.please_login),
                        Toast.LENGTH_SHORT
                    )
                    pleaseLoginToast?.show()
                }
            }
            //そもそもログインしていなければ、ログインを促す
            else {
                pleaseLoginToast?.cancel()
                pleaseLoginToast = Toast.makeText(
                    this,
                    resources.getString(R.string.please_login),
                    Toast.LENGTH_SHORT
                )
                pleaseLoginToast?.show()
            }
        }
    }

    private fun afterLineLogin(result: LineLoginResult): Any = when (result.responseCode) {
        LineApiResponseCode.SUCCESS -> {
            Log.i(
                TAG,
                "AccessToken = ${result.lineCredential?.accessToken?.tokenString}, expire time = ${result.lineCredential?.accessToken?.expiresInMillis}"
            )
            Log.i(TAG, "Line Name = ${result.lineProfile?.displayName}")
            Log.i(TAG, "Line ID = ${result.lineProfile?.userId}")
            credential = result.lineCredential
            val accessTokenStr = credential?.accessToken?.tokenString
            val taskPost = AsyncNetworkTaskPost(this) { str ->
                when (str) {
                    "Registered your data" -> {
                        Toast.makeText(
                            this,
                            resources.getString(R.string.newly_registered_user_information),
                            Toast.LENGTH_SHORT
                        ).show()
                        isLoginUser = true
                        mTextPoint.text = point.toString()
                    }
                    "Failed to Register your data" -> {
                        Toast.makeText(
                            this,
                            resources.getString(R.string.failed_to_login),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        val userPoint = str.toIntOrNull()
                        if (userPoint != null) {
                            isLoginUser = true
                            point = userPoint
                            mTextPoint.text = point.toString()
                        } else {
                            Toast.makeText(
                                this,
                                resources.getString(R.string.failed_to_login_for_unknown_error),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            taskPost.execute(accessTokenStr, "")
            // サーバーから画像を取得して、丸く形成して、imageViewに入れる
            Glide.with(this)
                .load(result.lineProfile?.pictureUrl).circleCrop().into(mImageProfile)
            mTextUserName.text = result.lineProfile?.displayName
        }
        LineApiResponseCode.CANCEL -> {
            Log.e(TAG, "LINE Login Canceled by user.")
        }
        LineApiResponseCode.AUTHENTICATION_AGENT_ERROR -> {
            Log.e(TAG, "LINE Login AUTHENTICATION_AGENT_ERROR by user")
        }
        else -> {
            Log.e(TAG, "Login FAILED!")
            Log.i(TAG, "ERROR ${result.errorData}")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_LINE_LOGIN -> {
                val result = LineLoginApi.getLoginResultFromIntent(data)
                Log.i(TAG, result.toString())
                afterLineLogin(result)
            }
        }
    }
}
