package kr.btsoft.messagecommunication

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import kr.btsoft.messagecommunication.databinding.ActivityMainBinding
import java.lang.Exception
import java.lang.StringBuilder
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope(),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    var activityContext: Context? = null
    private val wearableAppCheckPayload = "AppOpenWearable"
    private val wearableAppCheckPayloadReturnACK = "AppOpenWearableACK"
    private var wearableDeviceConnected: Boolean = false

    private var currentAckFromWearForAppOpenCheck: String? = null
    private val APP_OPEN_WEARABLE_PAYLOAD_PATH = "/APP_OPEN_WEARABLE_PAYLOAD"

    private val MESSAGE_ITEM_RECEIVED_PATH: String = "/message-item-received"

    private val TAG_GET_NODES: String = "getnodes1"
    private val TAG_MESSAGE_RECEIVED: String = "receive1"

    private var messageEvent: MessageEvent? = null
    private var wearableNodeUri: String? = null

    private lateinit var binding: ActivityMainBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activityContext = this
        wearableDeviceConnected = false

        binding.checkWearablesButton.setOnClickListener {
            if (!wearableDeviceConnected) {
                val tempAct: Activity = activityContext as MainActivity
                // Coroutine
                initializeDevicePairing(tempAct)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initializeDevicePairing(tempAct: Activity) {
        // Coroutine
        launch(Dispatchers.Default) {
            var getNodesResBool: BooleanArray? = null

            try {
                getNodesResBool = getNodes(tempAct.applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // UI Thread
            withContext(Dispatchers.Main) {
                if (getNodesResBool!![0]) {
                    // if message Acknowledgement Received
                    if (getNodesResBool[1]) {
                        Toast.makeText(
                            activityContext,
                            "Wearable device paired and app is open.",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.deviceConnectionStatusTv.text =
                            "Wearable device paired and app is open."
                        binding.deviceConnectionStatusTv.visibility = View.VISIBLE
                        wearableDeviceConnected = true
                    } else {
                        Toast.makeText(
                            activityContext,
                            "A wearable device is paired but the wearable app on your watch isn't open.\nLaunch the wearable app and try again.",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.deviceConnectionStatusTv.text =
                            "Wearable device paired but app isn't open."
                        binding.deviceConnectionStatusTv.visibility = View.VISIBLE
                        wearableDeviceConnected = false
                    }
                } else {
                    Toast.makeText(
                        activityContext,
                        "No wearable device paired. Pair a wearable device to your phone using the Wear OS app and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.deviceConnectionStatusTv.text =
                        "Wearable device not paired and connected."
                    binding.deviceConnectionStatusTv.visibility = View.VISIBLE
                    wearableDeviceConnected = false
                }
            }
        }
    }

    private fun getNodes(context: Context): BooleanArray {
        val nodeResults = HashSet<String>()
        val resBool = BooleanArray(2)
        // nodePresent
        resBool[0] = false
        // wearableReturnAckReceived
        resBool[1] = false
        val nodeListTask =
            Wearable.getNodeClient(context).connectedNodes
        try {
            // Block on a task and get the result synchronously (because this is on a background thread).
            val nodes =
                Tasks.await(
                    nodeListTask
                )
            Log.e(TAG_GET_NODES, "Task fetched nodes")
            for (node in nodes) {
                Log.e(TAG_GET_NODES, "inside loop")
                nodeResults.add(node.id)
                try {
                    val nodeId = node.id
                    // 메시지의 데이터를 Uri의 Byte로 설정
                    val payload: ByteArray = wearableAppCheckPayload.toByteArray()
                    // Send the rpc
                    // 클라이언트가 생성 비용이 저렴하기 때문에 멤버 변수가 없는 클라이언트를 인스턴스화한다.
                    // (이러한 파일은 캐시되어 GoogleApi instance 간에 공유된다.)
                    val sendMessageTask =
                        Wearable.getMessageClient(context)
                            .sendMessage(nodeId, APP_OPEN_WEARABLE_PAYLOAD_PATH, payload)
                    try {
                        // 작업을 차단하고 결과를 동기식으로 가져온다(백그라운드 스레드에 있으므로).
                        val result = Tasks.await(sendMessageTask)
                        Log.d(TAG_GET_NODES, "send message result : $result")
                        resBool[0] = true
                        // 확인 메시지가 표시될 때까지 1000ms/1 sec 동안 Wait
                        // Wait 1
                        if (currentAckFromWearForAppOpenCheck != wearableAppCheckPayloadReturnACK) {
                            Thread.sleep(100)
                            Log.d(TAG_GET_NODES, "ACK thread sleep 1")
                        }
                        if (currentAckFromWearForAppOpenCheck == wearableAppCheckPayloadReturnACK) {
                            resBool[1] = true
                            return resBool
                        }
                        //Wait 2
                        if (currentAckFromWearForAppOpenCheck != wearableAppCheckPayloadReturnACK) {
                            Thread.sleep(150)
                            Log.d(TAG_GET_NODES, "ACK thread sleep 2")
                        }
                        if (currentAckFromWearForAppOpenCheck == wearableAppCheckPayloadReturnACK) {
                            resBool[1] = true
                            return resBool
                        }
                        //Wait 3
                        if (currentAckFromWearForAppOpenCheck != wearableAppCheckPayloadReturnACK) {
                            Thread.sleep(200)
                            Log.d(TAG_GET_NODES, "ACK thread sleep 3")
                        }
                        if (currentAckFromWearForAppOpenCheck == wearableAppCheckPayloadReturnACK) {
                            resBool[1] = true
                            return resBool
                        }
                        //Wait 4
                        if (currentAckFromWearForAppOpenCheck != wearableAppCheckPayloadReturnACK) {
                            Thread.sleep(250)
                            Log.d(TAG_GET_NODES, "ACK thread sleep 4")
                        }
                        if (currentAckFromWearForAppOpenCheck == wearableAppCheckPayloadReturnACK) {
                            resBool[1] = true
                            return resBool
                        }
                        //Wait 5
                        if (currentAckFromWearForAppOpenCheck != wearableAppCheckPayloadReturnACK) {
                            Thread.sleep(350)
                            Log.d(TAG_GET_NODES, "ACK thread sleep 5")
                        }
                        if (currentAckFromWearForAppOpenCheck == wearableAppCheckPayloadReturnACK) {
                            resBool[1] = true
                            return resBool
                        }
                        resBool[1] = false
                        Log.d(
                            TAG_GET_NODES,
                            "ACK thread timeout, no message received from the wearable"
                        )
                    } catch (exception: Exception) {
                        exception.printStackTrace()
                    }
                } catch (e1: Exception) {
                    Log.d(TAG_GET_NODES, "send message exception")
                    e1.printStackTrace()
                }
            }   // end of for loop
        } catch (exception: Exception) {
            Log.e(TAG_GET_NODES, "Task failed: $exception")
            exception.printStackTrace()
        }
        return resBool
    }

    override fun onDataChanged(p0: DataEventBuffer) {
    }

    @SuppressLint("SetTextI18n")
    override fun onMessageReceived(p0: MessageEvent) {
        try {
            val s = String(p0.data, StandardCharsets.UTF_8)
            val messageEventPath: String = p0.path
            Log.d(
                TAG_MESSAGE_RECEIVED,
                "onMessageReceived() Received a message from watch:"
                        + p0.requestId
                        + " "
                        + messageEventPath
                        + " "
                        + s
            )
            if (messageEventPath == APP_OPEN_WEARABLE_PAYLOAD_PATH) {
                currentAckFromWearForAppOpenCheck = s
                Log.d(
                    TAG_MESSAGE_RECEIVED,
                    "Received acknowledgement message that app is open in wear"
                )

                val sbTemp = StringBuilder()
                sbTemp.append(binding.messageLogTextView.text.toString())
                sbTemp.append("\nWearable device connected.")
                Log.d("receive1", " $sbTemp")
                binding.messageLogTextView.text = sbTemp

                binding.checkWearablesButton.visibility = View.GONE
                messageEvent = p0
                wearableNodeUri = p0.sourceNodeId
            } else if (messageEventPath.isNotEmpty() && messageEventPath == MESSAGE_ITEM_RECEIVED_PATH) {

                try {
                    binding.messageLogTextView.visibility = View.VISIBLE

                    val sbTemp = StringBuilder()
                    sbTemp.append("\n")
                    sbTemp.append(s)
                    Log.d("receive1", " $sbTemp")
                    binding.messageLogTextView.append(sbTemp)

                    binding.scrollviewText.requestFocus()
                    binding.scrollviewText.post {
                        binding.scrollviewText.scrollTo(0, binding.scrollviewText.bottom)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("receive1", "Handled")
        }
    }

    override fun onCapabilityChanged(p0: CapabilityInfo) {
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
}