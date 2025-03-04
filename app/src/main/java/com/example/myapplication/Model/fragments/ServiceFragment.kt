package com.example.myapplication

import android.content.Context
import com.android.volley.RequestQueue
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
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

        val cardGrooming: MaterialCardView = view.findViewById(R.id.cardGrooming)
        val cardVet: MaterialCardView = view.findViewById(R.id.cardVet)
        val serviceTitle: TextView = view.findViewById(R.id.serviceTitle)
        recyclerView = view.findViewById(R.id.recyclerViewServices)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        serviceAdapter = ServiceAdapter(requireContext(), serviceList)
        recyclerView.adapter = serviceAdapter

        // Default load Grooming services
        fetchServices("Grooming")

        cardGrooming.setOnClickListener {
            selectedServiceType = "Grooming"
            serviceTitle.text = "Grooming Services"
            fetchServices("Grooming")
        }

        cardVet.setOnClickListener {
            selectedServiceType = "Veterinary"
            serviceTitle.text = "Veterinary Services"
            fetchServices("Veterinary")
        }
    }

    private fun fetchServices(serviceType: String) {
        val url = if (serviceType == "Grooming") {
            "http://192.168.1.65/backend/fetch_grooming_services.php"
        } else {
            "http://192.168.1.65/backend/fetch_veterinary_services.php"
        }

        val queue: RequestQueue = Volley.newRequestQueue(requireContext())

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    Log.d("ServiceFragment", "🔍 API Response: $response") // ✅ Check API response

                    // Extract "services" array from the response
                    val servicesArray = response.getJSONArray("services")

                    serviceList.clear()
                    for (i in 0 until servicesArray.length()) {
                        val jsonObject = servicesArray.getJSONObject(i)

                        // ✅ Only add services that are NOT removed
                        if (jsonObject.getString("removed") == "0") {
                            val service = ServiceModel(
                                jsonObject.getString("service_name"),
                                jsonObject.getString("price"),
                                jsonObject.getString("description"),
                                jsonObject.getString("type") // ✅ Ensure type is passed
                            )
                            serviceList.add(service)
                        }
                    }

                    Log.d("ServiceFragment", "✅ Updated Service List: $serviceList") // ✅ Verify service list

                    serviceAdapter.notifyDataSetChanged()
                    recyclerView.visibility = View.VISIBLE // Ensure RecyclerView is visible

                } catch (e: JSONException) {
                    Log.e("ServiceFragment", "❌ JSON Parsing Error: ${e.message}")
                }
            },
            { error ->
                Log.e("ServiceFragment", "❌ Error fetching services: ${error.message}")
            })

        queue.add(request) // Add request to the queue
    }

}
