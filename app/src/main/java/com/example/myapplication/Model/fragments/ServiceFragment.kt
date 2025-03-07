package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
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

        // Common views used in the fragment layout
        val cardGrooming: MaterialCardView? = view.findViewById(R.id.cardGrooming)
        val cardVet: MaterialCardView? = view.findViewById(R.id.cardVet)
        val serviceTitle: TextView? = view.findViewById(R.id.serviceTitle)
        recyclerView = view.findViewById(R.id.recyclerViewServices)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        serviceAdapter = ServiceAdapter(requireContext(), serviceList)
        recyclerView.adapter = serviceAdapter

        // Default load Grooming services
        fetchServices("Grooming")

        cardGrooming?.setOnClickListener {
            selectedServiceType = "Grooming"
            serviceTitle?.text = "Grooming Services"
            fetchServices("Grooming")
        }

        cardVet?.setOnClickListener {
            selectedServiceType = "Veterinary"
            serviceTitle?.text = "Veterinary Services"
            fetchServices("Veterinary")
        }

        // Get schedule appointment button from the activity's layout
        val scheduleAppointmentButton: Button? = requireActivity().findViewById(R.id.btnScheduleAppointment)
        scheduleAppointmentButton?.setOnClickListener {
            clearUserInput()
        } ?: Log.e("ServiceFragment", "No schedule appointment button found in the activity layout!")
    }

    // Clears the text (and any formatting) from the appointment input fields.
    // Using requireActivity().findViewById ensures the correct views are found if they're part of the activity layout.
    private fun clearUserInput() {
        val groomTypeField: EditText? = requireActivity().findViewById(R.id.groomTypeField)
        val checkupTypeField: EditText? = requireActivity().findViewById(R.id.checkupTypeField)
        groomTypeField?.setText("")
        checkupTypeField?.setText("")

        requireActivity().findViewById<EditText>(R.id.etName)?.setText("")
        requireActivity().findViewById<EditText>(R.id.etAddress)?.setText("")
        requireActivity().findViewById<EditText>(R.id.etPhone)?.setText("")
        requireActivity().findViewById<EditText>(R.id.etPetName)?.setText("")
        requireActivity().findViewById<EditText>(R.id.etPetBreed)?.setText("")
        requireActivity().findViewById<EditText>(R.id.etNotes)?.setText("")

        // Optionally reset other inputs (e.g., RadioGroup, Spinner)
        requireActivity().findViewById<RadioGroup>(R.id.radioPetType)?.clearCheck()
        requireActivity().findViewById<Spinner>(R.id.spinnerPaymentMethod)?.setSelection(0)
    }

    private fun fetchServices(serviceType: String) {
        val url = if (serviceType == "Grooming") {
            "http://192.168.43.215/backend/fetch_grooming_services.php"
        } else {
            "http://192.168.43.215/backend/fetch_veterinary_services.php"
        }

        val queue: RequestQueue = Volley.newRequestQueue(requireContext())

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    Log.d("ServiceFragment", "🔍 API Response: $response")
                    // Extract "services" array from the response
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
}
