package com.greenhackers.multiiamgeuploadtest

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle

import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast

import java.io.File
import java.util.ArrayList
import java.util.Objects
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private var parentView: View? = null
    private var listView: ListView? = null
    private var mProgressBar: ProgressBar? = null
    private var btnChoose: Button? = null
    private var btnUpload: Button? = null

    private var arrayList: ArrayList<Uri>? = null

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        parentView = findViewById(R.id.parent_layout)
        listView = findViewById(R.id.listView)
        mProgressBar = findViewById(R.id.progressBar)

        btnChoose = findViewById(R.id.btnChoose)
        btnChoose!!.setOnClickListener {
            // Display the file chooser dialog
            if (askForPermission())
                showChooser()
        }

        btnUpload = findViewById(R.id.btnUpload)
        btnUpload!!.setOnClickListener { uploadImagesToServer() }

        arrayList = ArrayList()
    }

    private fun showChooser() {
        // Use the GET_CONTENT intent from the utility class
        val target = FileUtils.createGetContentIntent()
        // Create the chooser Intent
        val intent = Intent.createChooser(
            target, getString(R.string.chooser_title)
        )
        try {
            startActivityForResult(intent, REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            // The reason for the existence of aFileChooser
        }

    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE ->
                // If the file selection was successful
                if (resultCode == Activity.RESULT_OK) {
                    if (data!!.clipData != null) {
                        val count = data.clipData!!.itemCount
                        var currentItem = 0
                        while (currentItem < count) {
                            val imageUri = data.clipData!!.getItemAt(currentItem).uri
                            //do something with the image (save it to some directory or whatever you need to do with it here)
                            currentItem = currentItem + 1
                            Log.d("Uri Selected", imageUri.toString())
                            try {
                                // Get the file path from the URI
                                val path = FileUtils.getPath(this, imageUri)
                                Log.d("Multiple File Selected", path!!)

                                arrayList!!.add(imageUri)
                                val mAdapter = MyAdapter(this@MainActivity, arrayList!!)
                                listView!!.adapter = mAdapter

                            } catch (e: Exception) {
                                Log.e(TAG, "File select error", e)
                            }

                        }
                    } else if (data.data != null) {
                        //do something with the image (save it to some directory or whatever you need to do with it here)
                        val uri = data.data
                        Log.i(TAG, "Uri = " + uri!!.toString())
                        try {
                            // Get the file path from the URI
                            val path = FileUtils.getPath(this, uri)
                            Log.d("Single File Selected", path!!)

                            arrayList!!.add(uri)
                            val mAdapter = MyAdapter(this@MainActivity, arrayList!!)
                            listView!!.adapter = mAdapter

                        } catch (e: Exception) {
                            Log.e(TAG, "File select error", e)
                        }

                    }
                }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private fun uploadImagesToServer() {
        if (InternetConnection.checkConnection(this@MainActivity)) {
            val retrofit = Retrofit.Builder()
                .baseUrl("http://192.168.0.103/multiimageuploadtest/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            showProgress()

            // create list of file parts (photo, video, ...)
            val parts = ArrayList<MultipartBody.Part>()

            // create upload service client
            val service = retrofit.create(ApiService::class.java)

            if (arrayList != null) {
                // create part for file (photo, video, ...)
                for (i in arrayList!!.indices) {
                    parts.add(prepareFilePart("image$i", arrayList!![i]))
                }
            }

            // create a map of data to pass along
            val description = createPartFromString("www.androidlearning.com")
            val size = createPartFromString("" + parts.size)

            // finally, execute the request
            val call = service.uploadMultiple(description, size, parts)

            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    hideProgress()
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@MainActivity,
                            "Images successfully uploaded!", Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Snackbar.make(parentView!!, R.string.string_some_thing_wrong, Snackbar.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    hideProgress()
                    Snackbar.make(parentView!!, t.message.toString(), Snackbar.LENGTH_LONG).show()
                }
            })

        } else {
            hideProgress()
            Toast.makeText(
                this@MainActivity,
                R.string.string_internet_connection_not_available, Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showProgress() {
        mProgressBar!!.visibility = View.VISIBLE
        btnChoose!!.isEnabled = false
        btnUpload!!.isEnabled = false
    }

    private fun hideProgress() {
        mProgressBar!!.visibility = View.GONE
        btnChoose!!.isEnabled = true
        btnUpload!!.isEnabled = true
    }

    private fun createPartFromString(descriptionString: String): RequestBody {
        return RequestBody.create(
            okhttp3.MultipartBody.FORM, descriptionString
        )
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private fun prepareFilePart(partName: String, fileUri: Uri): MultipartBody.Part {
        // https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
        // use the FileUtils to get the actual file by uri
        val file = FileUtils.getFile(this, fileUri)

        // create RequestBody instance from file
        val requestFile = RequestBody.create(
            MediaType.parse(Objects.requireNonNull(contentResolver.getType(fileUri))),
            file!!
        )

        // MultipartBody.Part is used to send also the actual file name
        return MultipartBody.Part.createFormData(partName, file.name, requestFile)
    }

    /**
     * Runtime Permission
     */
    private fun askForPermission(): Boolean {
        val currentAPIVersion = Build.VERSION.SDK_INT
        if (currentAPIVersion >= Build.VERSION_CODES.M) {
            val hasCallPermission = ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            if (hasCallPermission != PackageManager.PERMISSION_GRANTED) {
                // Ask for permission
                // need to request permission
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this@MainActivity,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) {
                    // explain
                    showMessageOKCancel(
                        DialogInterface.OnClickListener { dialogInterface, i ->
                            ActivityCompat.requestPermissions(
                                this@MainActivity,
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                                REQUEST_CODE_ASK_PERMISSIONS
                            )
                        })
                    // if denied then working here
                } else {
                    // Request for permission
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        REQUEST_CODE_ASK_PERMISSIONS
                    )
                }

                return false
            } else {
                // permission granted and calling function working
                return true
            }
        } else {
            return true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_ASK_PERMISSIONS -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                showChooser()
            } else {
                // Permission Denied
                Toast.makeText(this@MainActivity, "Permission Denied!", Toast.LENGTH_SHORT).show()
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun showMessageOKCancel(okListener: DialogInterface.OnClickListener) {

        val builder = AlertDialog.Builder(this@MainActivity)
        val dialog = builder.setMessage("You need to grant access to Read External Storage")
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                ContextCompat.getColor(this@MainActivity, android.R.color.holo_blue_light)
            )
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_light)
            )
        }

        dialog.show()

    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private val REQUEST_CODE = 6384
        private val REQUEST_CODE_ASK_PERMISSIONS = 124
    }
}
