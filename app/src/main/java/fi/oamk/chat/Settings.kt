package fi.oamk.chat

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class Settings : AppCompatActivity(){
    private lateinit var tvEmail : TextView
    private lateinit var welcomeMsg : TextView
    private lateinit var logOut : Button

    private lateinit var auth : FirebaseAuth

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState : Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        tvEmail = findViewById(R.id.email)
        welcomeMsg = findViewById(R.id.welcome)
        logOut = findViewById(R.id.logout_button)

        auth = Firebase.auth

        supportActionBar?.apply{
            title="Settings"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        if(auth.currentUser == null){
            welcomeMsg.text = "Not logged in"
            tvEmail.text = ""
            logOut.text = "Go back"
        }
        else{
            val currentUser = intent.getParcelableExtra<FirebaseUser>("currentUser")
            tvEmail.text = currentUser?.email
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    fun signOut(view: View){
        if(auth.currentUser != null){
            FirebaseAuth.getInstance().signOut()
        }
        goBack()
    }

    fun goBack(){
        val intent : Intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}