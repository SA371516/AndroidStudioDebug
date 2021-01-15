package com.example.locationactivity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.button_first).setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        view.findViewById<Button>(R.id.GetPhoto).setOnClickListener {
            val activity= activity as MainActivity
            activity.DebugRoadfunction()
            /*
            val observable=activity.MediaRoadFunction()
            observable?.doOnError {

            }

            activity.MediaRoadFunction()?.subscribeBy(
                    onNext = {
                        view.findViewById<ImageView>(R.id.Road_textView).setImageBitmap(it)
                    },
                    onError = {
                        Log.d("MediaLoadError",it.toString())
                    },
                    onComplete = {

                    }
            )

             */
        }

        val finenessOption= FitnessOptions.builder()
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA)
                .addDataType(DataType.TYPE_LOCATION_SAMPLE)
                .build()

        if(GoogleSignIn.hasPermissions(
                        GoogleSignIn.getLastSignedInAccount(context),
                        finenessOption
                )
        )
        {
            val activity=activity as MainActivity
            activity.ReadFitnessData()?.subscribe{
                view.findViewById<TextView>(R.id.textview_first).text=it.toString()
            }
            activity.actibityReadData()?.subscribe{
                view.findViewById<TextView>(R.id.Locationview).text=it.toString()
            }
        }
    }
}