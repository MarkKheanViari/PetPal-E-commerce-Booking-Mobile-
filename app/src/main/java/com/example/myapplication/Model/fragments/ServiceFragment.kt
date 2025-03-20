package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.myapplication.R
import com.example.myapplication.ServiceAdapter
import com.example.myapplication.ServiceModel
import com.google.android.material.card.MaterialCardView
import org.json.JSONException

class ServiceFragment : Fragment(R.layout.fragment_service) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var serviceAdapter: ServiceAdapter
    private val serviceList = mutableListOf<ServiceModel>()
    private var selectedServiceType: String = "Grooming" // Default to Grooming

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        val cardGrooming: MaterialCardView = view.findViewById(R.id.cardGrooming)
        val cardVet: MaterialCardView = view.findViewById(R.id.cardVet)
        val groomingUnderline: View = view.findViewById(R.id.groomingUnderline)
        val vetUnderline: View = view.findViewById(R.id.vetUnderline)
        recyclerView = view.findViewById(R.id.recyclerViewServices)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        serviceAdapter = ServiceAdapter(requireContext(), serviceList)
        recyclerView.adapter = serviceAdapter

        // Default load Grooming services
        fetchServices("Grooming")

        // Category tab click listeners
        cardGrooming.setOnClickListener {
            selectedServiceType = "Grooming"
            // Update underlines
            groomingUnderline.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.orange))
            vetUnderline.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
            fetchServices("Grooming")
        }

        cardVet.setOnClickListener {
            selectedServiceType = "Veterinary"
            // Update underlines
            vetUnderline.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.orange))
            groomingUnderline.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
            fetchServices("Veterinary")
        }
    }

    private fun fetchServices(serviceType: String) {
        val url = if (serviceType == "Grooming") {
            "http://192.168.1.15/backend/fetch_grooming_services.php"
        } else {
            "http://192.168.1.15/backend/fetch_veterinary_services.php"
        }

        val queue: RequestQueue = Volley.newRequestQueue(requireContext())

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    Log.d("ServiceFragment", "🔍 API Response: $response")
                    val servicesArray = response.getJSONArray("services")
                    serviceList.clear()
                    for (i in 0 until servicesArray.length()) {
                        val jsonObject = servicesArray.getJSONObject(i)
                        if (jsonObject.getString("removed") == "0") {
                            val service = ServiceModel(
                                jsonObject.getString("service_name"),
                                jsonObject.getString("price"),
                                jsonObject.getString("description"),
                                jsonObject.getString("type")
                            )
                            serviceList.add(service)
                        }
                    }
                    Log.d("ServiceFragment", "✅ Updated Service List: $serviceList")
                    serviceAdapter.notifyDataSetChanged()
                    recyclerView.visibility = View.VISIBLE
                } catch (e: JSONException) {
                    Log.e("ServiceFragment", "❌ JSON Parsing Error: ${e.message}")
                }
            },
            { error ->
                Log.e("ServiceFragment", "❌ Error fetching services: ${error.message}")
            })

        queue.add(request)
    }

    private fun clearUserInput(view: View) {
        // Note: This method references fields that may not be present in this layout.
        // Adjust or remove if not needed.
        val groomTypeField: EditText? = view.findViewById(R.id.groomTypeField)
        val checkupTypeField: EditText? = view.findViewById(R.id.checkupTypeField)
        groomTypeField?.setText("")
        checkupTypeField?.setText("")

        view.findViewById<EditText>(R.id.etName)?.setText("")
        view.findViewById<EditText>(R.id.etAddress)?.setText("")
        view.findViewById<EditText>(R.id.etPhone)?.setText("")
        view.findViewById<EditText>(R.id.etPetName)?.setText("")
        view.findViewById<EditText>(R.id.etPetBreed)?.setText("")
        view.findViewById<EditText>(R.id.etNotes)?.setText("")

        view.findViewById<RadioGroup>(R.id.radioPetType)?.clearCheck()
        view.findViewById<Spinner>(R.id.spinnerPaymentMethod)?.setSelection(0)
    }
}