package fi.oamk.chat

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    private lateinit var messages : ArrayList<Message>
    private lateinit var database : DatabaseReference
    private lateinit var edMessage : EditText
    private lateinit var rcMessageList : RecyclerView

    private val TAG : String = MainActivity::class.java.name
    private lateinit var auth : FirebaseAuth
    private var currentUser : FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        edMessage = findViewById(R.id.messageText)
        rcMessageList = findViewById(R.id.messageList)
        database = Firebase.database.reference
        messages = arrayListOf<Message>()

        auth = Firebase.auth

        edMessage.setOnKeyListener { y, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                addMessage()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }

        val messageListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.value != null) {
                    val messagesFromFirebase =
                        (snapshot.value as HashMap<String, ArrayList<Message>>).get("messages")
                    messages.clear()
                    if(messagesFromFirebase != null) {
                        for(i in 0..messagesFromFirebase.size-1) {
                            if(messagesFromFirebase.get(i) != null){
                                val message: Message = Message.from(messagesFromFirebase.get(i) as HashMap<String, String>)
                                messages.add(message)
                            }
                        }
                    }
                    rcMessageList.adapter?.notifyDataSetChanged()
                    rcMessageList.smoothScrollToPosition(rcMessageList.adapter!!.itemCount-1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("Chat", error.toString())
            }
        }

        database.addValueEventListener(messageListener)
        rcMessageList.layoutManager = LinearLayoutManager(this)
        rcMessageList.adapter = MyAdapter(messages)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.app_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.settings -> {
            this.showSettings()
            true
        } else -> {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        if(auth.currentUser == null){
            loginDialog()
        }
    }

    private fun showSettings(){
        val intent = Intent(this, Settings::class.java).apply{
            putExtra("currentUser",currentUser)
        }
        startActivity(intent)
    }

    fun loginDialog(){
        val builder = AlertDialog.Builder(this)

        with(builder){
            setTitle("Login")
            val linearLayout : LinearLayout = LinearLayout(this@MainActivity)
            linearLayout.orientation = LinearLayout.VERTICAL

            val inputEmail : EditText = EditText(this@MainActivity)
            inputEmail.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            inputEmail.hint = "Enter email"
            linearLayout.addView(inputEmail)

            val inputPw : EditText = EditText(this@MainActivity)
            inputPw.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_PASSWORD
            inputPw.hint = "Enter password"
            linearLayout.addView(inputPw)

            builder.setView(linearLayout)

            builder.setPositiveButton("OK"){dialog, which ->
                login(inputEmail.text.toString(), inputPw.text.toString())
            }.show()
        }
    }

    fun login(email : String, password : String){
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this){ task ->
            if(task.isSuccessful){
                Log.d(TAG, "signInWithEmail : success")
                currentUser = auth.currentUser
            }
            else{
                Log.w(TAG, "signInWithEmail : failure", task.exception)
                Toast.makeText(baseContext, "Authentification failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun addMessage(){
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH.mm")
        val newMessage:Message = Message(edMessage.text.toString(), currentUser?.email.toString(), formatter.format(LocalDateTime.now()))
        messages.add(newMessage)
        database.child("messages").setValue(messages)
        edMessage.setText("")
        closeKeyBoard()
        rcMessageList.smoothScrollToPosition(rcMessageList.adapter!!.itemCount-1)
    }

    private fun closeKeyBoard(){
        val view = this.currentFocus
        if(view != null){
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken,0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FirebaseAuth.getInstance().signOut()
    }
}