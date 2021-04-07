package `in`.mrd.demoapp

import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val tag = MainActivity::class.java.simpleName
    private var amList: RecyclerView? = null
    private var amLoader: ProgressBar? = null
    private var conMgr: ConnectivityManager? = null
    private val url = "https://fetch-hiring.s3.amazonaws.com/"
    private var dataList: List<Data>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getIds()
        callApi()
    }

    private fun getIds() {
        try {
            amList = findViewById(R.id.amList)
            amLoader = findViewById(R.id.amLoader)

            conMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            dataList = ArrayList<Data>()
        } catch (ex: Exception) {
            Log.e(tag, ex.toString())
        }
    }

    private val isConnectedToInternet: Boolean
        get() {
            if (conMgr != null) {
                val info = conMgr!!.allNetworkInfo
                for (anInfo in info) if (anInfo.state == NetworkInfo.State.CONNECTED) {
                    return true
                }
            }
            return false
        }

    private fun showAlert(message: String?) {
        if (isFinishing) return
        val builder = AlertDialog.Builder(this)
        builder.setMessage(message)
        builder.setPositiveButton("Ok", null)
        builder.setCancelable(true)
        builder.show()
    }

    private fun callApi() {
        try {
            if (isConnectedToInternet) {
                amLoader?.visibility = View.VISIBLE
                getClient().getJson()?.enqueue(object : Callback<List<Data>?> {
                    override fun onResponse(
                        call: Call<List<Data>?>,
                        response: Response<List<Data>?>
                    ) {
                        amLoader?.visibility = View.GONE
                        if (response.body() != null && response.body()!!.isNotEmpty()) {
                            for (v in response.body()!!) {
                                if (v.name != null && v.name != "") {
                                    dataList = dataList?.plus(v) // for removing null and empty values of name.
                                }
                            }

                            Collections.sort(dataList) { lhs, rhs -> lhs?.id?.compareTo(rhs?.id!!)!! }
                            Collections.sort(dataList) { lhs, rhs -> lhs?.listId?.compareTo(rhs?.listId!!)!! }

                            val arrayAdapter = AdapterCustom(dataList)
                            val manager: RecyclerView.LayoutManager =
                                LinearLayoutManager(this@MainActivity)
                            amList?.itemAnimator = DefaultItemAnimator()
                            amList?.layoutManager = manager
                            amList?.adapter = arrayAdapter
                        } else {
                            showAlert("No data found!!")
                        }
                    }

                    override fun onFailure(call: Call<List<Data>?>, t: Throwable) {
                        amLoader?.visibility = View.GONE
                        showAlert("No data received!!")
                    }
                })
            } else {
                showAlert("No internet connectivity!!")
            }
        } catch (ex: Exception) {
            Log.e(tag, ex.toString())
        }
    }

    private class AdapterCustom(dataList: List<Data>?) :
        RecyclerView.Adapter<AdapterCustom.MyViewHolder>() {

        var dataList: List<Data>? = null

        init {
            this.dataList = dataList
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): MyViewHolder {
            val itemView: View = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.inflate_row, viewGroup, false)
            return MyViewHolder(itemView)
        }

        override fun onBindViewHolder(viewHolder: MyViewHolder, i: Int) {
            viewHolder.irTitle.text =
                HtmlCompat.fromHtml(
                    "<b>" + dataList?.get(viewHolder.adapterPosition)!!.name + "</b><br><small>Id : " +
                            dataList?.get(viewHolder.adapterPosition)!!.id + " | ListId : " +
                            dataList?.get(viewHolder.adapterPosition)!!.listId + "</small>",
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            if (viewHolder.adapterPosition % 2 == 0) {
                viewHolder.irTitle.setBackgroundResource(R.color.colorEven)
            } else {
                viewHolder.irTitle.setBackgroundResource(R.color.colorOdd)
            }
        }

        override fun getItemCount(): Int {
            return dataList!!.size
        }

        class MyViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            var irTitle: TextView = itemView.findViewById(R.id.irTitle)
        }
    }

    class Data {
        val id: Int? = null
        val listId: Int? = null
        val name: String? = null
    }

    interface WebMethod {
        @GET("hiring.json")
        fun getJson(): Call<List<Data>?>?
    }

    private fun getClient(): WebMethod {

        val gson = GsonBuilder()
            .setLenient()
            .create()

        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .build()

        val adapter: Retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()
        return adapter.create(WebMethod::class.java)
    }
}