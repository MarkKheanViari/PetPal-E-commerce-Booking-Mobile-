package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.card.MaterialCardView
import org.json.JSONException
import org.json.JSONObject

class ServiceFragment : Fragment(R.layout.fragment_service) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var serviceAdapter: ServiceAdapter
    private val serviceList = mutableListOf<ServiceModel>()
    private var selectedServiceType: String = "Grooming" // Default to Grooming
    private lateinit var queue: RequestQueue

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        queue = Volley.newRequestQueue(requireContext()) // Initialize the request queue once

        val cardGrooming: MaterialCardView? = view.findViewById(R.id.cardGrooming)
        val cardVet: MaterialCardView? = view.findViewById(R.id.cardVet)
        val serviceTitle: TextView? = view.findViewById(R.id.serviceTitle)
        recyclerView = view.findViewById(R.id.recyclerViewServices)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        serviceAdapter = ServiceAdapter(requireContext(), serviceList)
        recyclerView.adapter = serviceAdapter

        fetchServices("Grooming") // Load default grooming services

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

        val scheduleAppointmentButton: Button? = view.findViewById(R.id.btnScheduleAppointment)
        scheduleAppointmentButton?.setOnClickListener {
            clearUserInput(view)
        } ?: Log.e("ServiceFragment", "No schedule appointment button found in the layout!")
    }

    private fun fetchServices(serviceType: String) {
        val url = if (serviceType == "Grooming") {
            "http://192.168.1.12/backend/fetch_grooming_services.php"
        } else {
            "http://192.168.1.12/backend/fetch_veterinary_services.php"
        }

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    Log.d("ServiceFragment", "🔍 API Response: $response")

                    val servicesArray = response.getJSONArray("services")
                    serviceList.clear()

                    for (i in 0 until servicesArray.length()) {
                        val jsonObject = servicesArray.getJSONObject(i)

                        if (jsonObject.optString("removed", "0") == "0") { // Ensure missing values don't crash
                            val service = ServiceModel(
                                jsonObject.optString("service_name", "N/A"),
                                jsonObject.optString("price", "0"),
                                jsonObject.optString("description", "No description available"),
                                jsonObject.optString("type", "Unknown")
                            )
                            serviceList.add(service)
                        }
                    }

                    requireActivity().runOnUiThread {
                        serviceAdapter.notifyDataSetChanged()
                        recyclerView.visibility = View.VISIBLE
                    }

                    Log.d("ServiceFragment", "✅ Updated Service List: $serviceList")

                } catch (e: JSONException) {
                    Log.e("ServiceFragment", "❌ JSON Parsing Error: ${e.message}")
                }
            },
            { error ->
                val errorMessage = when (error) {
                    is TimeoutError -> "Request timeout. Check network connection."
                    is NoConnectionError -> "No internet connection."
                    is AuthFailureError -> "Authentication error."
                    is ServerError -> "Server error. Try again later."
                    is NetworkError -> "Network error occurred."
                    is ParseError -> "Error parsing response."
                    else -> "Unknown error: ${error.message}"
                }
                Log.e("ServiceFragment", "❌ Error fetching services: $errorMessage")
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            })

        queue.add(request)
    }

    private fun clearUserInput(view: View) {
        view.findViewById<EditText>(R.id.groomTypeField)?.setText("")
        view.findViewById<EditText>(R.id.checkupTypeField)?.setText("")
        view.findViewById<EditText>(R.id.etName)?.setText("")
        view.findViewById<EditText>(R.id.etAddress)?.setText("")
        view.findViewById<EditText>(R.id.etPhone)?.setText("")
        view.findViewById<EditText>(R.id.etPetName)?.setText("")
        view.findViewById<EditText>(R.id.etPetBreed)?.setText("")
        view.findViewById<EditText>(R.id.etNotes)?.setText("")

        view.findViewById<RadioGroup>(R.id.radioPetType)?.clearCheck()
        view.findViewById<Spinner>(R.id.spinnerPaymentMethod)?.setSelection(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        queue.cancelAll { true } // Cancel all pending requests when fragment is destroyed
    }
}
