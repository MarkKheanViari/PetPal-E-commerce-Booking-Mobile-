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

        // Common views used in both layouts
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

        // Try to find the schedule button by checking both possible IDs.
        val scheduleAppointmentButton: Button? =
            view.findViewById(R.id.btnScheduleAppointment)

        scheduleAppointmentButton?.setOnClickListener {
            clearUserInput(view)
        } ?: Log.e("ServiceFragment", "No schedule appointment button found in the layout!")
    }

    // Clears the text (and any formatting) from the appointment input fields.
    private fun clearUserInput(view: View) {
        // Check for both grooming and checkup fields since each layout has its own
        val groomTypeField: EditText? = view.findViewById(R.id.groomTypeField)
        val checkupTypeField: EditText? = view.findViewById(R.id.checkupTypeField)
        groomTypeField?.setText("")
        checkupTypeField?.setText("")

        // Clear the rest of the common fields
        view.findViewById<EditText>(R.id.etName)?.setText("")
        view.findViewById<EditText>(R.id.etAddress)?.setText("")
        view.findViewById<EditText>(R.id.etPhone)?.setText("")
        view.findViewById<EditText>(R.id.etPetName)?.setText("")
        view.findViewById<EditText>(R.id.etPetBreed)?.setText("")
        view.findViewById<EditText>(R.id.etNotes)?.setText("")

        // Optionally reset other inputs (e.g., RadioGroup, Spinner)
        view.findViewById<RadioGroup>(R.id.radioPetType)?.clearCheck()
        view.findViewById<Spinner>(R.id.spinnerPaymentMethod)?.setSelection(0)
    }

    private fun fetchServices(serviceType: String) {
        val url = if (serviceType == "Grooming") {
            "http://192.168.168.55/backend/fetch_grooming_services.php"
        } else {
            "http://192.168.168.55/backend/fetch_veterinary_services.php"
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
