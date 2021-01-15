package com.example.locationactivity

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private val CAPTURE_IMAGE:Int=100
    private lateinit var image_view: ImageView
    private var cameraUri: Uri? = null

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_second, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        image_view=view.findViewById<ImageView>(R.id.photo_imageView)

        view.findViewById<Button>(R.id.button_second).setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
        view.findViewById<Button>(R.id.photo_button).setOnClickListener {

            // 保存先のフォルダー
            // 保存先のフォルダー
            val cFolder = activity?.getExternalFilesDir(Environment.DIRECTORY_DCIM)
            val fileDate: String = SimpleDateFormat(
                    "yyyyMMdd_HHmmss", Locale.JAPAN).format(Date())
            // ファイル名
            // ファイル名
            val fileName = String.format("CameraIntent_%s.jpg", fileDate)

            val cameraFile = File(cFolder, fileName)

            cameraUri = FileProvider.getUriForFile(
                    requireActivity(), requireActivity().packageName.toString() + ".fileprovider",
                    cameraFile)

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
            startActivityForResult(intent, CAPTURE_IMAGE)
/*
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent->
                activity?.let { it1 ->
                    intent.resolveActivity(it1.packageManager)?.also {
                        startActivityForResult(intent, CAPTURE_IMAGE)
                    }
                }
            }

 */
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==CAPTURE_IMAGE&&resultCode== Activity.RESULT_OK){
            if(data?.extras==null){
                Log.d("debug", "cancel")
                return
            }
            //
            val bitmap: Bitmap = data.extras!!.get("data") as Bitmap
            val bmpWidth=bitmap.width
            val bmpHeight=bitmap.height
            Log.d("debug", String.format("Width:%d", bmpWidth))
            Log.d("debug", String.format("Height:%d", bmpHeight))
            //bmpWidth*=3
            //bmpHeight *= 3
            val resizeBitmap = Bitmap.createScaledBitmap(bitmap, bmpWidth, bmpHeight, true)
            image_view.setImageBitmap(resizeBitmap)

            //ファイルに写真を保存する
            //val activity=activity as MainActivity
            //activity.MediaStoreFunction(bitmap)
        }
    }
}