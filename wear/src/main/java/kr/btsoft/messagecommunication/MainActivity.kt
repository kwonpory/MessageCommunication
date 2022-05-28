package kr.btsoft.messagecommunication

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.wear.ambient.AmbientModeSupport
import androidx.wear.ambient.AmbientModeSupport.AmbientCallback
import com.google.android.gms.wearable.*
import kr.btsoft.messagecommunication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), AmbientModeSupport.AmbientCallbackProvider,
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    private var activityContext: Context? = null

    private lateinit var binding: ActivityMainBinding

    private val TAG_MESSAGE_RECEIVED = "receive1"
    private val APP_OPEN_WEARABLE_PAYLOAD_PATH = "/APP_OPEN_WEARABLE_PAYLOAD"

    private var mobileDeviceConnected: Boolean = false

    // Payload string items
    private val wearableAppCheckPayloadReturnACK = "AppOpenWearableACK"

    private val MESSAGE_ITEM_RECEIVED_PATH: String = "/message-item-received"

    private var messageEvent: MessageEvent? = null
    private var mobileNodeUri: String? = null

    private lateinit var ambientController: AmbientModeSupport.AmbientController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activityContext = this

        // Enables Always-on
        ambientController = AmbientModeSupport.attach(this)

        // On click listener for send message button
        binding.sendMessageButton.setOnClickListener {
            if (mobileDeviceConnected) {
                if (binding.messageContentEditText.text!!.isNotEmpty()) {
                    val nodeId: String = messageEvent?.sourceNodeId!!

                    // Set the data of the message to be the bytes of the Uri.
                    val payload: ByteArray =
                        binding.messageContentEditText.text.toString().toByteArray()

                    // Send the rpc
                    val sendMessageTask =
                        Wearable.getMessageClient(activityContext!!)
                            .sendMessage(nodeId, MESSAGE_ITEM_RECEIVED_PATH, payload)

                    binding.deviceConnectionStatusTv.visibility = View.GONE

                } else {
                    Toast.makeText(
                        activityContext,
                        "Message content is empty. Pleas enter some message and procced.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDataChanged(p0: DataEventBuffer) {}

    override fun onCapabilityChanged(p0: CapabilityInfo) {}

    @SuppressLint("SetTextI18n")
    override fun onMessageReceived(p0: MessageEvent) {
        try {
            // Uri host 부분에서 data item 을 생성한 node 의 node id 를 가져온다.
            val nodeId: String = p0.sourceNodeId.toString()
            // 메시지의 데이터를 URI 의 Byte 로 설정
            val returnPayloadAck = wearableAppCheckPayloadReturnACK
            val payload: ByteArray = returnPayloadAck.toByteArray()

            // Send the rpc
            val sendMessageTask =
                Wearable.getMessageClient(activityContext!!)
                    .sendMessage(nodeId, APP_OPEN_WEARABLE_PAYLOAD_PATH, payload)

            Log.d(
                TAG_MESSAGE_RECEIVED,
                "Acknowledgement message successfully with payload : $returnPayloadAck"
            )

            messageEvent = p0
            mobileNodeUri = p0.sourceNodeId

            sendMessageTask.addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d(TAG_MESSAGE_RECEIVED, "Message sent successfully")

                    val sbTemp = StringBuilder()
                    sbTemp.append("\nMobile device connected.")
                    Log.d("receive1", " $sbTemp")

                    mobileDeviceConnected = true

                    binding.textInputLayout.visibility = View.VISIBLE
                    binding.sendMessageButton.visibility = View.VISIBLE
                    binding.deviceConnectionStatusTv.visibility = View.VISIBLE
                    binding.deviceConnectionStatusTv.text = "Mobile device is connected"
                } else {
                    Log.d(TAG_MESSAGE_RECEIVED, "Message failed.")
                }
            }
        } catch (e: Exception) {
            Log.d(
                TAG_MESSAGE_RECEIVED,
                "Handled in sending message back to the sending node"
            )
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            Wearable.getDataClient(activityContext!!).removeListener(this)
            Wearable.getMessageClient(activityContext!!).removeListener(this)
            Wearable.getCapabilityClient(activityContext!!).removeListener(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onResume() {
        super.onResume()
        try {
            Wearable.getDataClient(activityContext!!).addListener(this)
            Wearable.getMessageClient(activityContext!!).addListener(this)
            Wearable.getCapabilityClient(activityContext!!)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getAmbientCallback(): AmbientCallback = MyAmbientCallback()

    private inner class MyAmbientCallback : AmbientCallback() {
        override fun onEnterAmbient(ambientDetails: Bundle?) {
            super.onEnterAmbient(ambientDetails)
        }

        override fun onUpdateAmbient() {
            super.onUpdateAmbient()
        }

        override fun onExitAmbient() {
            super.onExitAmbient()
        }
    }
}