package com.example.locationactivity

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleObserver
import androidx.loader.content.CursorLoader
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.annotations.NonNull
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() ,LifecycleObserver{

    val REQUEST_CODE:Int=9999
    val fitnessUrl="com.google.android.apps.fitness"
    val test="&hl=ja&gl=US"

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
        //FitnessInstalled
        appInstalledOrNot(fitnessUrl)

        val finenessOption=FitnessOptions.builder()
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA)
                .addDataType(DataType.TYPE_LOCATION_SAMPLE)
                .build()

        if(!GoogleSignIn.hasPermissions(
                        GoogleSignIn.getLastSignedInAccount(this),
                        finenessOption
                )){
            GoogleSignIn.requestPermissions(
                    this,
                    REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    finenessOption
            )
        }

        val bitmap=BitmapFactory.decodeResource(resources, R.drawable.ic_baseline_alarm_add_24)
        //MediaStoreFunction(bitmap)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun ReadFitnessData(): @NonNull io.reactivex.rxjava3.core.Observable<Int>? {
        var result=0
        val cal=Calendar.getInstance()
        cal.time=Date()
        val endRequest = cal.timeInMillis
        cal.add(Calendar.WEEK_OF_YEAR, -1)
        val startRequest=cal.timeInMillis

        val request=DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .setTimeRange(startRequest, endRequest, TimeUnit.MILLISECONDS)
            .bucketByTime(1, TimeUnit.DAYS)
            .build()

        return Single.create<Int> { emitter ->
            Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readData(request)
                .addOnSuccessListener {
                    val buckets=it.buckets
                    buckets.forEach{ bucket->
                        val start=bucket.getStartTime(TimeUnit.MILLISECONDS)
                        val end=bucket.getEndTime(TimeUnit.MILLISECONDS)
                        val dataSet=bucket.getDataSet(DataType.AGGREGATE_STEP_COUNT_DELTA)
                        if(!dataSet.dataPoints.isEmpty()){
                            val value=dataSet.dataPoints.first().getValue(Field.FIELD_STEPS)
                            Log.d("AggregateBucket", "OK")
                            Log.d("Aggregate", "$start $end $value")
                            result=value.asInt()
                            emitter.onSuccess(value.asInt())
                        }
                        else{
                            Log.d("AggregateBucket", "Empty")
                            Log.d("Aggregate", "$start $end")
                        }
                    }
                }
        }.subscribeOn(Schedulers.io()).toObservable()
    }

    fun actibityReadData(): @NonNull io.reactivex.rxjava3.core.Observable<String>? {
        val cal=Calendar.getInstance()
        cal.time=Date()
        val endRequest = cal.timeInMillis
        cal.add(Calendar.WEEK_OF_YEAR, -1)
        val startRequest=cal.timeInMillis

        val readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_LOCATION_SAMPLE)
                .bucketByActivityType(1, TimeUnit.SECONDS)
                .setTimeRange(startRequest, endRequest, TimeUnit.MILLISECONDS)
                .build()

        return Single.create<String> { emitter ->
            Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                    .readData(readRequest)
                    .addOnSuccessListener {
                        val buckets=it.buckets
                        buckets.forEach{ bucket->
                            val dataSet=bucket.getDataSet(DataType.TYPE_LOCATION_SAMPLE)
                            if(dataSet!=null){
                                Log.d("FitLocation", "${dataSet.dataPoints.first().getValue(Field.FIELD_LATITUDE)},${dataSet.dataPoints.first().getValue(Field.FIELD_LONGITUDE)}")
                                emitter.onSuccess("${dataSet.dataPoints.first().getValue(Field.FIELD_LATITUDE)},${dataSet.dataPoints.first().getValue(Field.FIELD_LONGITUDE)}")
                            }
                            else{
                                Log.d("FitLocation", "Empty")
                                emitter.onSuccess("Emitter$dataSet")
                            }
                            Log.d("Activity", bucket.activity)
                        }
                    }.addOnFailureListener {
                        Log.d("FitLocation", "Failed:$it")
                    }
                    .addOnCanceledListener {
                        Log.d("FitLocation", "Canceled")
                    }
        }.subscribeOn(Schedulers.io()).toObservable()
    }

    fun MediaStoreFunction(bitmap: Bitmap){
        if(isExternalStorageWritable()){
            val fileNameDate = SimpleDateFormat("yyyyMMdd_HHmmss")
            val fileName = fileNameDate.format(Date())+".jpg"
            val values= ContentValues().apply {
                this.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
                this.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                this.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                this.put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val resolver=applicationContext.contentResolver
            val collection= MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val item=resolver.insert(collection, values)
            try{
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                val outstream: OutputStream? = item?.let {
                    contentResolver.openOutputStream(it)
                }
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outstream)
            }catch (e: IOException){
                Log.e("MediaStore", e.toString())
            }
            values.clear()
            //　排他的にアクセスの解除
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
        }
    }
    fun DebugRoadfunction(){
        val projection= arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.TITLE,
                MediaStore.Images.Media.DATE_TAKEN
        )

        val selection = ( "${MediaStore.Images.Media.DISPLAY_NAME} == ?")
        val selectionArgs = arrayOf(
                dateToTimestamp(day = 18, month = 8, year = 2020).toString()
        )

        val img_uri= MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val strage_uri= MediaStore.Files.getContentUri("external")

        var sb: StringBuilder? = null

        contentResolver.query(
                img_uri,
                projection,
                null,
                null,
                null
        ).use{
            try{
                if(it!=null && it.moveToFirst()){
                    val str = java.lang.String.format(
                            "MediaStore.Images = %s\n\n", it.count)
                    sb = java.lang.StringBuilder(str)
                    do {
                        val idColumn= it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

                        val dateTakenColumn =
                                it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                        val id= idColumn.let { it1 ->
                            it.getLong(it1)
                        }
                        val uri=Uri.withAppendedPath(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id.toString()
                        )
                        val dateTaken = dateTakenColumn.let { it1 -> Date(it.getLong(it1)) }
                        contentResolver.openInputStream(uri).use { _inputStream->
                            val bmp=BitmapFactory.decodeStream(_inputStream)
                        }
                        sb!!.append("ID: ")
                        sb!!.append(it.getString(it.getColumnIndex(
                                MediaStore.Images.Media._ID)))
                        sb!!.append("\n")
                        sb!!.append("Title: ")
                        sb!!.append(it.getString(it.getColumnIndex(
                                MediaStore.Images.Media.TITLE)))
                        sb!!.append("\n")
                        sb!!.append("DATE: ")
                        sb!!.append(dateTaken)
                        sb!!.append("\n\n")
                    } while (it.moveToNext())
                }else{

                }
            }catch (e: IOException){
                Log.d("ROADERROR", e.toString())
            }finally{
                it?.close()
            }
            val textView = findViewById<TextView>(R.id.Road_textView)
            textView.text=sb
        }
    }

    fun MediaRoadFunction():io.reactivex.rxjava3.core.Observable<Bitmap>?{

        val projection= arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.TITLE,
                MediaStore.Images.Media.DATE_TAKEN
        )

        val selection = ( "${MediaStore.Images.Media.DATE_TAKEN} >= ?")
        val selectionArgs = arrayOf(
                dateToTimestamp(day = 1, month = 1, year = 2021).toString()
        )

        val img_uri= MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val strage_uri= MediaStore.Files.getContentUri("external")

        var sb: StringBuilder? = null
        val cursorLoader=CursorLoader(
                this,
                img_uri,
                projection,
                selection,
                selectionArgs,
                null
        )

        val result= Single.create<Bitmap> { emitter ->

            //cursorLoader.loadInBackground().use{
            contentResolver.query(
                    strage_uri,
                    projection,
                    selection,
                    selectionArgs,
                    null
            ).use{
            try{
                    if(it!=null && it.moveToFirst()){
                        val str = java.lang.String.format(
                                "MediaStore.Images = %s\n\n", it.count)
                        sb = java.lang.StringBuilder(str)
                        do {
                            val idColumn= it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

                            val dateTakenColumn =
                                    it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                            val id= idColumn.let { it1 ->
                                it.getLong(it1)
                            }
                            val uri=Uri.withAppendedPath(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    id.toString()
                            )
                            val dateTaken = dateTakenColumn.let { it1 -> Date(it.getLong(it1)) }
                            contentResolver.openInputStream(uri).use { _inputStream->
                                val bmp=BitmapFactory.decodeStream(_inputStream)
                                emitter.onSuccess(bmp)
                            }
                            sb!!.append("ID: ")
                            sb!!.append(it.getString(it.getColumnIndex(
                                    MediaStore.Images.Media._ID)))
                            sb!!.append("\n")
                            sb!!.append("Title: ")
                            sb!!.append(it.getString(it.getColumnIndex(
                                    MediaStore.Images.Media.TITLE)))
                            sb!!.append("\n")
                            sb!!.append("DATE: ")
                            sb!!.append(dateTaken)
                            sb!!.append("\n\n")
                        } while (it.moveToNext())
                    }else{

                    }
                }catch (e: IOException){
                    Log.d("ROADERROR", e.toString())
                    emitter.onError(e)
                }finally{
                    it?.close()
                }
                val textView = findViewById<TextView>(R.id.Road_textView)
                textView.text=sb
            }
        }.subscribeOn(Schedulers.io()).toObservable()

        return result
    }

     fun dateToTimestamp(day: Int, month: Int, year: Int): Long =
            SimpleDateFormat("dd.MM.yyyy").let { formatter ->
                formatter.parse("$day.$month.$year")?.time ?: 0
            }

    fun isExternalStorageWritable(): Boolean {
        val state: String = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED.equals(state)
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            REQUEST_CODE -> {
            }
        }
    }

    private fun appInstalledOrNot(url: String):Boolean{
        var app_installed=false
        val packageManager=packageManager
        try{
            packageManager.getPackageInfo(url, PackageManager.GET_ACTIVITIES)
            app_installed=true
        }
        catch (e: PackageManager.NameNotFoundException){
            app_installed=false
            val uri:Uri= Uri.parse("https://play.google.com/store/apps/details?id=$url$test")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
        return app_installed
    }
}