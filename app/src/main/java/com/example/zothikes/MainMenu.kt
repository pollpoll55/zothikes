package com.example.zothikes

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.AuthFailureError
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import org.json.JSONObject

class MainMenu : AppCompatActivity() {
    companion object{private const val LOCATION_PERMISSION_REQUEST_CODE = 1}
    var user_latitude = 0.0
    var user_longitude = 0.0
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var recommended_trails = JSONArray()
    var latitudes = JSONArray()
    var longitudes = JSONArray()

    var volleyRequestQueue: RequestQueue? = null
    var dialog: ProgressDialog? = null
    val serverAPIURL: String = "http://10.0.2.2:5000/api/get_recommendation"
    val TAG = "ZotHikes"
    fun SendSignUpDataToServer(email: String?, latitude: String?, longitude: String?) {
        volleyRequestQueue = Volley.newRequestQueue(this)
        dialog = ProgressDialog.show(this, "", "Please wait...", true);
        val parameters: MutableMap<String, String?> = HashMap()
        // Add your parameters in HashMap
        parameters["email"] = email;
        parameters["latitude"] = latitude
        parameters["longitude"] = longitude
        val strReq: StringRequest = object : StringRequest(
                Method.POST,serverAPIURL,
                Response.Listener { response ->
                    Log.e(TAG, "response: " + response)
                    dialog?.dismiss()

                    // Handle Server response here
                    try {
                        val responseObj = JSONArray(response)
                        for (i in 0 until responseObj.length()) {
                            Log.e("Trail Name", ""+responseObj.getJSONArray(i)[0])
                            Log.e("Trail Latitude", ""+ responseObj.getJSONArray(i).getJSONObject(1).getString("latitude"))
                            Log.e("Trail Longitude", ""+ responseObj.getJSONArray(i).getJSONObject(1).getString("longitude"))
                            recommended_trails.put(responseObj.getJSONArray(i)[0])
                            latitudes.put(responseObj.getJSONArray(i).getJSONObject(1).getString("latitude"))
                            longitudes.put(responseObj.getJSONArray(i).getJSONObject(1).getString("longitude"))
                        }

//                        val isSuccess = responseObj.getBoolean("isSuccess")
//                        val code = responseObj.getInt("code")
//                        val message = responseObj.getString("message")
//                        Log.e("REAcher ", "REaching here?")
//                        if (responseObj.has("data")) {
//                            val data = responseObj.getJSONArray("data")
//                            // Handle your server response data here
//                        }
//                        Toast.makeText(this,message,Toast.LENGTH_LONG).show()

                    } catch (e: Exception) { // caught while parsing the response
                        Log.e(TAG, "problem occurred")
                        e.printStackTrace()
                    }
                },
                Response.ErrorListener { volleyError -> // error occurred
                    Log.e(TAG, "problem occurred, volley error: " + volleyError.message)
                }) {

            override fun getParams(): MutableMap<String, String?> {
                return parameters;
            }

            override fun getBodyContentType(): String {
                return "application/json"
            }

            @Throws(AuthFailureError::class)
            override fun getBody(): ByteArray? {
                return JSONObject(parameters as Map<*, *>).toString().toByteArray()
            }

            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {

                val headers: MutableMap<String, String> = HashMap()
                // Add your Header paramters here
                return headers
            }
        }
        // Adding request to request queue
        volleyRequestQueue?.add(strReq)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)
        val user_data = mutableListOf<String>()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLocation()
        findViewById<Button>(R.id.open_map_button).setOnClickListener{
            val intent = Intent(this, NearbyMap::class.java).apply{
                for (i in 0 until recommended_trails.length())
                {
                    putExtra("$i", recommended_trails[i].toString())
                    putExtra("latitudes$i", latitudes[i].toString())
                    putExtra("longitudes$i", longitudes[i].toString())
                }
            }
            startActivity(intent)
        }
        findViewById<Button>(R.id.recommend_button).setOnClickListener{
            val intent = Intent(this, RecommendationPage::class.java).apply{
//                Log.e("Length is:", recommended_trails.length().toString())
                for (i in 0 until recommended_trails.length())
                {
                    putExtra("$i", recommended_trails[i].toString())
                    putExtra("latitudes$i", latitudes[i].toString())
                    putExtra("longitudes$i", longitudes[i].toString())
                }
            }
            startActivity(intent)
        }

        findViewById<Button>(R.id.edit_info_button).setOnClickListener{
            val intent = Intent(this, MainActivity::class.java).apply{}
            startActivity(intent)
            //mainActivity is to enter user's info.
        }

        findViewById<Button>(R.id.logout_button).setOnClickListener{
            mGoogleSignInClient.signOut()
                    .addOnCompleteListener(this) {
                        // Update your UI here
                        val intent = Intent(this, UserLogin::class.java).apply{}
                        startActivity(intent)
                        Toast.makeText(this, "Successfully logged out.", Toast.LENGTH_SHORT).show()
                    }
        }

    }

    fun getLocation()
    {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)  //location permission request
        }

        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                user_latitude = location.latitude
                user_longitude = location.longitude
                //Toast.makeText(this, "Coordinates: $user_latitude , $user_longitude", Toast.LENGTH_SHORT).show()
                val acct = GoogleSignIn.getLastSignedInAccount(this)
                if (acct != null) {
                    SendSignUpDataToServer(acct.email, user_latitude.toString(), user_longitude.toString())
                }
            }
            else{
                Toast.makeText(this, "ERROR: Location could not be found.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1) // gucci
        {
            if (grantResults.contains(PackageManager.PERMISSION_GRANTED))
            {
                //Toast.makeText(this, "permissions are good.", Toast.LENGTH_SHORT).show()
                getLocation()
            }


        }
    }


}
